package com.example.salattracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PrayerTimeEntity::class], version = 1, exportSchema = false)
abstract class SalatDatabase : RoomDatabase() {

    abstract fun prayerTimeDao(): PrayerTimeDao

    companion object {
        @Volatile
        private var INSTANCE: SalatDatabase? = null

        fun getInstance(context: Context): SalatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SalatDatabase::class.java,
                    "salat_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
