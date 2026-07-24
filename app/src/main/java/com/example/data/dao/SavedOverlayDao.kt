package com.example.data.dao

import androidx.room.*
import com.example.data.model.SavedOverlay
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedOverlayDao {
    @Query("SELECT * FROM saved_overlays ORDER BY createdAt DESC")
    fun getAllOverlays(): Flow<List<SavedOverlay>>

    @Query("SELECT * FROM saved_overlays WHERE id = :id LIMIT 1")
    suspend fun getOverlayById(id: Long): SavedOverlay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverlay(overlay: SavedOverlay): Long

    @Update
    suspend fun updateOverlay(overlay: SavedOverlay)

    @Delete
    suspend fun deleteOverlay(overlay: SavedOverlay)
}
