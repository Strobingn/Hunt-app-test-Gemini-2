package com.example.data.dao

import androidx.room.*
import com.example.data.model.HuntingWaypoint
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntingWaypointDao {
    @Query("SELECT * FROM hunting_waypoints ORDER BY createdAt DESC")
    fun getAllWaypoints(): Flow<List<HuntingWaypoint>>

    @Query("SELECT * FROM hunting_waypoints WHERE overlayId = :overlayId ORDER BY createdAt DESC")
    fun getWaypointsForOverlay(overlayId: Long): Flow<List<HuntingWaypoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: HuntingWaypoint): Long

    @Delete
    suspend fun deleteWaypoint(waypoint: HuntingWaypoint)
}
