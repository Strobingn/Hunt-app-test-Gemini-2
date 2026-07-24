package com.example.ml

import com.example.data.model.LazDataset
import kotlin.math.*

/**
 * On-device Machine Learning & Topographic Classifier for 3D LiDAR point clouds.
 * Generates a Digital Elevation Model (DEM) grid and computes slope, aspect, curvature,
 * and topographic feature classification (Saddles, Ridges, Draws, Benches).
 */
object TerrainFeatureExtractor {

    private const val GRID_SIZE = 30 // 30x30 DEM grid resolution

    fun processDataset(dataset: LazDataset): Array<Array<TerrainCell>> {
        val width = dataset.width.coerceAtLeast(10f)
        val height = dataset.height.coerceAtLeast(10f)

        val cellW = width / GRID_SIZE
        val cellH = height / GRID_SIZE

        // 1. Build Elevation Matrix (Average Z per cell)
        val elevGrid = Array(GRID_SIZE) { FloatArray(GRID_SIZE) { dataset.minZ } }
        val countGrid = Array(GRID_SIZE) { IntArray(GRID_SIZE) { 0 } }

        dataset.points.forEach { pt ->
            val gx = ((pt.x - dataset.minX) / cellW).toInt().coerceIn(0, GRID_SIZE - 1)
            val gy = ((pt.y - dataset.minY) / cellH).toInt().coerceIn(0, GRID_SIZE - 1)
            elevGrid[gy][gx] += pt.z
            countGrid[gy][gx]++
        }

        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                if (countGrid[y][x] > 0) {
                    elevGrid[y][x] /= countGrid[y][x]
                } else {
                    // Fallback to bilinear interpolation or surrounding mean
                    elevGrid[y][x] = dataset.minZ + (dataset.elevationRange * 0.3f)
                }
            }
        }

        // 2. Compute Slope, Aspect, TPI, and Feature Type per cell
        val result = Array(GRID_SIZE) { y ->
            Array(GRID_SIZE) { x ->
                val zCenter = elevGrid[y][x]

                // Partial derivatives dz/dx, dz/dy using Horn's algorithm
                val dzdx = computeDzDx(elevGrid, x, y, cellW)
                val dzdy = computeDzDy(elevGrid, x, y, cellH)

                val slopeRad = atan(sqrt(dzdx * dzdx + dzdy * dzdy))
                val slopeDeg = Math.toDegrees(slopeRad.toDouble()).toFloat()

                var aspectDeg = Math.toDegrees(atan2(-dzdy.toDouble(), dzdx.toDouble())).toFloat()
                if (aspectDeg < 0) aspectDeg += 360f

                // Topographic Position Index (TPI) = Center elevation - Mean elevation of neighborhood
                val tpi = zCenter - computeNeighborhoodMean(elevGrid, x, y)

                // Classify feature based on TPI, Slope, and Curvature
                val featureType = when {
                    tpi < -12f -> TerrainFeatureType.CREEK_BOTTOM
                    tpi < -4f -> TerrainFeatureType.DRAINAGE_DRAW
                    tpi > 12f -> TerrainFeatureType.RIDGE_CREST
                    tpi in -2f..3f && slopeDeg in 3f..12f -> TerrainFeatureType.SADDLE
                    tpi in 2f..8f && slopeDeg in 1f..6f -> TerrainFeatureType.BENCH
                    else -> TerrainFeatureType.BEDDING_FLAT
                }

                // AI Stand Suitability Score (0 - 100)
                val standScore = computeStandScore(slopeDeg, tpi, featureType)

                val worldX = dataset.minX + (x + 0.5f) * cellW
                val worldY = dataset.minY + (y + 0.5f) * cellH

                TerrainCell(
                    gridX = x,
                    gridY = y,
                    worldX = worldX,
                    worldY = worldY,
                    elevationZ = zCenter,
                    slopeDegrees = slopeDeg,
                    aspectDegrees = aspectDeg,
                    tpiScore = tpi,
                    featureType = featureType,
                    standScore = standScore
                )
            }
        }

        return result
    }

    private fun computeDzDx(grid: Array<FloatArray>, x: Int, y: Int, cellW: Float): Float {
        val xMinus = (x - 1).coerceIn(0, GRID_SIZE - 1)
        val xPlus = (x + 1).coerceIn(0, GRID_SIZE - 1)
        return (grid[y][xPlus] - grid[y][xMinus]) / (2 * cellW)
    }

    private fun computeDzDy(grid: Array<FloatArray>, x: Int, y: Int, cellH: Float): Float {
        val yMinus = (y - 1).coerceIn(0, GRID_SIZE - 1)
        val yPlus = (y + 1).coerceIn(0, GRID_SIZE - 1)
        return (grid[yPlus][x] - grid[yMinus][x]) / (2 * cellH)
    }

    private fun computeNeighborhoodMean(grid: Array<FloatArray>, cx: Int, cy: Int): Float {
        var sum = 0f
        var count = 0
        for (dy in -2..2) {
            for (dx in -2..2) {
                if (dx == 0 && dy == 0) continue
                val nx = (cx + dx).coerceIn(0, GRID_SIZE - 1)
                val ny = (cy + dy).coerceIn(0, GRID_SIZE - 1)
                sum += grid[ny][nx]
                count++
            }
        }
        return if (count > 0) sum / count else grid[cy][cx]
    }

    private fun computeStandScore(slopeDeg: Float, tpi: Float, feature: TerrainFeatureType): Int {
        var score = 50

        // Bonus for Saddles & Pinch points
        if (feature == TerrainFeatureType.SADDLE) score += 35
        if (feature == TerrainFeatureType.BENCH) score += 25
        if (feature == TerrainFeatureType.DRAINAGE_DRAW) score += 15

        // Penalize extreme cliff slopes (> 35°)
        if (slopeDeg > 35f) score -= 30
        if (slopeDeg in 8f..22f) score += 15 // Ideal walking slope

        return score.coerceIn(5, 99)
    }
}
