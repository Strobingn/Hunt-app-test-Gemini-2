package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WaypointType(val label: String, val iconName: String) {
    TREESTAND("Tree Stand", "Nature"),
    TRAIL_CAM("Trail Camera", "CameraAlt"),
    BEDDING("Bedding Area", "Bed"),
    ELK_SIGN("Elk / Deer Sign", "Pets"),
    WATER_HOLE("Water Hole", "WaterDrop"),
    PARKING("Parking / Access", "DirectionsCar"),
    CUSTOM("Custom Marker", "Place")
}

@Entity(tableName = "hunting_waypoints")
data class HuntingWaypoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val overlayId: Long = 0,
    val title: String,
    val note: String = "",
    val type: WaypointType = WaypointType.CUSTOM,
    val lat: Double,
    val lng: Double,
    val elevationMeters: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
