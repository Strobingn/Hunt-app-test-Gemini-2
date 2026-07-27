package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.dao.HuntingWaypointDao
import com.example.data.dao.SavedOverlayDao
import com.example.data.model.HuntingWaypoint
import com.example.data.model.SavedOverlay
import com.example.data.model.WaypointType

class Converters {
    @TypeConverter
    fun fromWaypointType(type: WaypointType?): String? = type?.name

    @TypeConverter
    fun toWaypointType(value: String?): WaypointType? = value?.let {
        try { WaypointType.valueOf(it) } catch (e: Exception) { WaypointType.CUSTOM }
    }
}

@Database(
    entities = [SavedOverlay::class, HuntingWaypoint::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
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
