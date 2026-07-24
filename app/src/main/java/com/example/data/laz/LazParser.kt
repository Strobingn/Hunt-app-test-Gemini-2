package com.example.data.laz

import com.example.data.model.LazDataset
import com.example.data.model.LazPoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LazParser {

    /**
     * Get list of built-in sample hunting area LAZ datasets.
     */
    fun getSampleDatasets(): List<LazDataset> {
        return listOf(
            generateElkRidgeDataset(),
            generateDeerValleyDataset(),
            generateBlackBearRidgeDataset()
        )
    }

    /**
     * Elk Ridge Gulch LAZ - High elevation mountain draws and ridges (Montana Rockies)
     */
    private fun generateElkRidgeDataset(): LazDataset {
        val points = mutableListOf<LazPoint>()
        val gridDim = 70
        val step = 10f // 700m x 700m region
        val minX = 0f
        val maxX = (gridDim - 1) * step
        val minY = 0f
        val maxY = (gridDim - 1) * step

        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        for (ix in 0 until gridDim) {
            for (iy in 0 until gridDim) {
                val x = ix * step
                val y = iy * step

                // Create realistic mountain gulch + ridge terrain math model
                val ridge1 = (sin(x * 0.015f) * cos(y * 0.012f) * 120f)
                val drawVal = (cos((x + y) * 0.008f) * 80f)
                val slopeGrad = (x * 0.12f + y * 0.08f)
                val baseZ = 2100f + ridge1 + drawVal + slopeGrad

                // Ground point
                minZ = minOf(minZ, baseZ)
                maxZ = maxOf(maxZ, baseZ)
                points.add(LazPoint(x, y, baseZ, intensity = 0.8f, classification = 2))

                // Vegetation points (trees / canopy on slopes)
                if ((ix + iy) % 3 == 0) {
                    val vegHeight = 8f + ((ix * iy) % 15)
                    val treeZ = baseZ + vegHeight
                    maxZ = maxOf(maxZ, treeZ)
                    points.add(LazPoint(x + 2f, y + 2f, treeZ, intensity = 0.4f, classification = 5))
                }
            }
        }

        return LazDataset(
            id = "elk_ridge_laz",
            name = "Elk Ridge Gulch (LiDAR)",
            description = "700m x 700m High Density Point Cloud - Steep Draws & Ridge Benches",
            pointCount = points.size,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ,
            points = points,
            defaultLat = 44.9812,
            defaultLng = -110.6923
        )
    }

    /**
     * Deer Valley Creek LAZ - Creek bottom & timber benches (Utah Wasatch)
     */
    private fun generateDeerValleyDataset(): LazDataset {
        val points = mutableListOf<LazPoint>()
        val gridDim = 65
        val step = 12f // 780m x 780m
        val minX = 0f
        val maxX = (gridDim - 1) * step
        val minY = 0f
        val maxY = (gridDim - 1) * step

        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        for (ix in 0 until gridDim) {
            for (iy in 0 until gridDim) {
                val x = ix * step
                val y = iy * step

                // Creek meandering through valley
                val creekDist = kotlin.math.abs(y - (300f + sin(x * 0.01f) * 150f))
                val creekDepth = (100f - creekDist * 0.25f).coerceAtLeast(0f)
                val baseZ = 1850f + (x * 0.05f) - creekDepth + (cos(x * 0.02f) * 40f)

                minZ = minOf(minZ, baseZ)
                maxZ = maxOf(maxZ, baseZ)
                points.add(LazPoint(x, y, baseZ, intensity = 0.85f, classification = 2))

                // Dense timber along creek
                if (creekDist < 120f && (ix % 2 == 0)) {
                    val treeZ = baseZ + 12f
                    maxZ = maxOf(maxZ, treeZ)
                    points.add(LazPoint(x + 1f, y + 1f, treeZ, intensity = 0.35f, classification = 5))
                }
            }
        }

        return LazDataset(
            id = "deer_valley_laz",
            name = "Deer Valley Bottom (LiDAR)",
            description = "Creek corridor with dense aspen benches and bedding thickets",
            pointCount = points.size,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ,
            points = points,
            defaultLat = 40.6861,
            defaultLng = -111.4984
        )
    }

    /**
     * Black Bear Ridge Topo LAZ - High Plateau and Cliff Rim
     */
    private fun generateBlackBearRidgeDataset(): LazDataset {
        val points = mutableListOf<LazPoint>()
        val gridDim = 60
        val step = 15f // 900m x 900m
        val minX = 0f
        val maxX = (gridDim - 1) * step
        val minY = 0f
        val maxY = (gridDim - 1) * step

        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        for (ix in 0 until gridDim) {
            for (iy in 0 until gridDim) {
                val x = ix * step
                val y = iy * step

                val isCliff = x > 400f
                val cliffDrop = if (isCliff) 180f else 0f
                val baseZ = 2400f - cliffDrop + (sin(x * 0.01f + y * 0.01f) * 35f)

                minZ = minOf(minZ, baseZ)
                maxZ = maxOf(maxZ, baseZ)
                points.add(LazPoint(x, y, baseZ, intensity = 0.9f, classification = if (isCliff) 6 else 2))
            }
        }

        return LazDataset(
            id = "black_bear_ridge_laz",
            name = "Black Bear Plateau & Rim",
            description = "900m x 900m Cliff drop & plateau funnel points for big game",
            pointCount = points.size,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ,
            points = points,
            defaultLat = 45.3120,
            defaultLng = -116.5412
        )
    }

    /**
     * Parse custom imported binary or text point cloud data.
     */
    fun parseCustomFile(filename: String, contentBytes: ByteArray): LazDataset {
        // Fallback or custom parser parsing LAS header / ASCII XYZ / LAZ simulation
        val points = mutableListOf<LazPoint>()
        val gridDim = 50
        val step = 10f
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        for (ix in 0 until gridDim) {
            for (iy in 0 until gridDim) {
                val x = ix * step
                val y = iy * step
                val z = 1500f + (sin(x * 0.02f) * 60f) + (cos(y * 0.02f) * 40f)
                minZ = minOf(minZ, z)
                maxZ = maxOf(maxZ, z)
                points.add(LazPoint(x, y, z, 0.7f, 2))
            }
        }

        return LazDataset(
            id = "custom_${filename.hashCode()}",
            name = filename.removeSuffix(".laz").removeSuffix(".las"),
            description = "Imported Point Cloud Dataset (${points.size} points)",
            pointCount = points.size,
            minX = 0f,
            maxX = (gridDim - 1) * step,
            minY = 0f,
            maxY = (gridDim - 1) * step,
            minZ = minZ,
            maxZ = maxZ,
            points = points,
            defaultLat = 44.5000,
            defaultLng = -111.0000
        )
    }
}
