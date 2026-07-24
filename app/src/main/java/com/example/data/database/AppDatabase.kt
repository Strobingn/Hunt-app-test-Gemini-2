package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.HuntingWaypointDao
import com.example.data.dao.SavedOverlayDao
import com.example.data.model.HuntingWaypoint
import com.example.data.model.SavedOverlay

@Database(
    entities = [SavedOverlay::class, HuntingWaypoint::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedOverlayDao(): SavedOverlayDao
    abstract fun huntingWaypointDao(): HuntingWaypointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hunt_align_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
