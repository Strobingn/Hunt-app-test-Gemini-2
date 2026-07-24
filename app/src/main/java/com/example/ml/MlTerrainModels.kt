package com.example.ml

import androidx.compose.ui.graphics.Color

/**
 * Topographic terrain micro-feature classified from LiDAR points.
 */
enum class TerrainFeatureType(val label: String, val colorHex: Long) {
    SADDLE("Pinch Point Saddle", 0xFFFFD700), // Gold
    RIDGE_CREST("Ridge Crest", 0xFFFF5722),    // Deep Orange
    DRAINAGE_DRAW("Drainage / Draw", 0xFF0288D1), // Cyan Blue
    BENCH("Ridge Bench", 0xFF8BC34A),           // Light Green
    CREEK_BOTTOM("Stream / Creek", 0xFF00BCD4),  // Water Blue
    BEDDING_FLAT("Bedding Flat", 0xFF9C27B0)     // Purple
}

/**
 * Single cell in the DEM (Digital Elevation Model) grid processed by ML algorithms.
 */
data class TerrainCell(
    val gridX: Int,
    val gridY: Int,
    val worldX: Float,
    val worldY: Float,
    val elevationZ: Float,
    val slopeDegrees: Float,
    val aspectDegrees: Float, // Direction slope faces (0° North - 360°)
    val tpiScore: Float,      // Topographic Position Index (positive = ridge, negative = draw)
    val featureType: TerrainFeatureType,
    val standScore: Int       // AI Stand Suitability Score (0 - 100)
)

/**
 * AI-generated game movement corridor path segment.
 */
data class WildlifeCorridor(
    val id: String,
    val name: String,
    val pathPoints: List<Pair<Float, Float>>, // Local X, Y coordinates
    val costScore: Float,
    val primarySpecies: String
)

/**
 * Thermal wind vector representation at a specific point.
 */
data class ThermalWindVector(
    val localX: Float,
    val localY: Float,
    val windAngleDegrees: Float, // Wind direction
    val speedMps: Float,         // Estimated speed
    val isDowndraft: Boolean     // Morning cooling thermal vs afternoon updraft
)

/**
 * Scent dispersion cone overlay calculated by AI simulation.
 */
data class ScentPlumeCone(
    val originX: Float,
    val originY: Float,
    val windDirectionDegrees: Float,
    val coneAngleDegrees: Float = 45f,
    val reachMeters: Float = 150f
)

/**
 * Trail Camera Vision Analysis Result from Gemini Vision AI.
 */
data class TrailCamAnalysis(
    val species: String,
    val count: Int,
    val details: String,
    val confidence: Float,
    val timeOfDay: String,
    val estimatedAntlerPoints: String?
)
