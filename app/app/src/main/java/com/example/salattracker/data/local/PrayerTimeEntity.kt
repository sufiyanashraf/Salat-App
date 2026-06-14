package com.example.salattracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_times",
    indices = [Index(value = ["date", "latitude", "longitude"], unique = true)]
)
data class PrayerTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,       // "yyyy-MM-dd"
    val fajr: String,       // "HH:mm"
    val dhuhr: String,      // "HH:mm"
    val asr: String,        // "HH:mm"
    val maghrib: String,    // "HH:mm"
    val isha: String,       // "HH:mm"
    val latitude: Double,
    val longitude: Double
)
