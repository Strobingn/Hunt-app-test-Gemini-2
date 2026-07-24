package com.example.data.model

import androidx.compose.ui.graphics.Color

/**
 * Single 3D point in a LAZ point cloud.
 */
data class LazPoint(
    val x: Float,          // Local X coordinate (meters)
    val y: Float,          // Local Y coordinate (meters)
    val z: Float,          // Elevation Z (meters)
    val intensity: Float,  // Return intensity (0.0 - 1.0)
    val classification: Int // 2 = Ground, 3 = Low Veg, 4 = Med Veg, 5 = High Veg, 6 = Structure
)

/**
 * Color ramp options for LiDAR elevation / surface rendering.
 */
enum class ColorRamp(val displayName: String) {
    TOPO_RAINBOW("Topo Rainbow"),
    ELEVATION_HEATMAP("Heatmap (Red-Yellow)"),
    SLOPE_SHADING("Slope Shading"),
    FOREST_CANOPY("Forest Canopy"),
    GRAYSCALE_RELIEF("Shaded Relief")
}

/**
 * Data structure holding a parsed LAZ file dataset.
 */
data class LazDataset(
    val id: String,
    val name: String,
    val description: String,
    val pointCount: Int,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
    val points: List<LazPoint>,
    val defaultLat: Double,
    val defaultLng: Double
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val elevationRange: Float get() = maxZ - minZ
}
