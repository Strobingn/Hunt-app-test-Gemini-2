package com.example.data.repository

import com.example.data.dao.HuntingWaypointDao
import com.example.data.dao.SavedOverlayDao
import com.example.data.laz.LazParser
import com.example.data.model.HuntingWaypoint
import com.example.data.model.LazDataset
import com.example.data.model.SavedOverlay
import kotlinx.coroutines.flow.Flow

class HuntMapRepository(
    private val overlayDao: SavedOverlayDao,
    private val waypointDao: HuntingWaypointDao
) {
    val allSavedOverlays: Flow<List<SavedOverlay>> = overlayDao.getAllOverlays()
    val allWaypoints: Flow<List<HuntingWaypoint>> = waypointDao.getAllWaypoints()

    fun getSampleDatasets(): List<LazDataset> {
        return LazParser.getSampleDatasets()
    }

    suspend fun saveOverlay(overlay: SavedOverlay): Long {
        return if (overlay.id == 0L) {
            overlayDao.insertOverlay(overlay)
        } else {
            overlayDao.updateOverlay(overlay)
            overlay.id
        }
    }

    suspend fun deleteOverlay(overlay: SavedOverlay) {
        overlayDao.deleteOverlay(overlay)
    }

    suspend fun addWaypoint(waypoint: HuntingWaypoint): Long {
        return waypointDao.insertWaypoint(waypoint)
    }

    suspend fun deleteWaypoint(waypoint: HuntingWaypoint) {
        waypointDao.deleteWaypoint(waypoint)
    }
}
