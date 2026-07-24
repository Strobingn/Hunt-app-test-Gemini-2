package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_overlays")
data class SavedOverlay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lazDatasetId: String,
    val centerLat: Double,
    val centerLng: Double,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 0.75f,
    val colorRampName: String = "TOPO_RAINBOW",
    val minElevationFilter: Float = 0.0f,
    val maxElevationFilter: Float = 1.0f,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
