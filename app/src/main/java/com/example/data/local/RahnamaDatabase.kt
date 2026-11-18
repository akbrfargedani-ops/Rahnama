package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedPlaceEntity::class,
        SearchHistoryEntity::class,
        StreetReportEntity::class,
        OfflineRegionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RahnamaDatabase : RoomDatabase() {
    abstract fun rahnamaDao(): RahnamaDao

    companion object {
        @Volatile
        private var INSTANCE: RahnamaDatabase? = null

        fun getInstance(context: Context): RahnamaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RahnamaDatabase::class.java,
                    "rahnama_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
