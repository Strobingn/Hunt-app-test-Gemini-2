package com.example.ml

import kotlin.math.cos
import kotlin.math.sin

/**
 * Micro-Thermal Wind Vector Field & Scent Dispersion Model.
 * Simulates how solar aspect, slope angle, and time-of-day drive cold air drainage (morning thermal downdrafts)
 * and solar heating updrafts (afternoon thermal thermals) across the 3D LiDAR terrain.
 */
object ThermalScentSimulator {

    enum class TimeOfDay(val label: String, val isCooling: Boolean) {
        DAWN_MORNING("Dawn / Morning (Cold Air Drainage)", true),
        MIDDAY_SUN("Midday (Thermal Updrafts)", false),
        EVENING_DUSK("Dusk / Evening (Thermal Inversion)", true)
    }

    /**
     * Calculates thermal wind vectors across all cells in the grid.
     */
    fun calculateWindVectors(
        grid: Array<Array<TerrainCell>>,
        timeOfDay: TimeOfDay,
        ambientWindSpeedMps: Float = 2.5f,
        ambientWindDirectionDegrees: Float = 270f // West wind default
    ): List<ThermalWindVector> {
        val vectors = mutableListOf<ThermalWindVector>()

        for (y in grid.indices step 3) {
            for (x in grid[0].indices step 3) {
                val cell = grid[y][x]

                // Thermal direction depends on slope aspect
                val thermalDir = if (timeOfDay.isCooling) {
                    // Downslope flow (cold air sinks along aspect)
                    cell.aspectDegrees
                } else {
                    // Upslope flow (warm air rises opposite aspect)
                    (cell.aspectDegrees + 180f) % 360f
                }

                // Combine ambient wind vector with slope thermal vector
                val ambientRad = Math.toRadians(ambientWindDirectionDegrees.toDouble())
                val thermalRad = Math.toRadians(thermalDir.toDouble())

                val thermalMagnitude = (cell.slopeDegrees / 30f).coerceIn(0.2f, 2.0f)

                val vecX = ambientWindSpeedMps * cos(ambientRad) + thermalMagnitude * cos(thermalRad)
                val vecY = ambientWindSpeedMps * sin(ambientRad) + thermalMagnitude * sin(thermalRad)

                val combinedAngleRad = Math.atan2(vecY, vecX)
                var combinedAngleDeg = Math.toDegrees(combinedAngleRad).toFloat()
                if (combinedAngleDeg < 0) combinedAngleDeg += 360f

                val combinedSpeed = Math.sqrt(vecX * vecX + vecY * vecY).toFloat()

                vectors.add(
                    ThermalWindVector(
                        localX = cell.worldX,
                        localY = cell.worldY,
                        windAngleDegrees = combinedAngleDeg,
                        speedMps = combinedSpeed,
                        isDowndraft = timeOfDay.isCooling
                    )
                )
            }
        }

        return vectors
    }

    /**
     * Generates a 2D scent plume dispersion cone for a tree stand position given wind vector.
     */
    fun generateScentPlume(
        standX: Float,
        standY: Float,
        windDirectionDegrees: Float,
        windSpeedMps: Float
    ): ScentPlumeCone {
        val reach = (windSpeedMps * 60f).coerceIn(80f, 250f)
        val coneAngle = (60f / windSpeedMps.coerceAtLeast(1.0f)).coerceIn(30f, 75f)

        return ScentPlumeCone(
            originX = standX,
            originY = standY,
            windDirectionDegrees = windDirectionDegrees,
            coneAngleDegrees = coneAngle,
            reachMeters = reach
        )
    }
}
