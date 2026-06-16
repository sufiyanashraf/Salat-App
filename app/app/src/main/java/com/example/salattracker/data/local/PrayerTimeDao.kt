package com.example.salattracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao {

    @Query(
        "SELECT * FROM prayer_times WHERE date = :date AND latitude = :lat AND longitude = :lng LIMIT 1"
    )
    fun getPrayerTimeByDate(date: String, lat: Double, lng: Double): Flow<PrayerTimeEntity?>

    @Query(
        "SELECT * FROM prayer_times WHERE date LIKE :yearMonth || '%' AND latitude = :lat AND longitude = :lng"
    )
    fun getPrayerTimesForMonth(yearMonth: String, lat: Double, lng: Double): Flow<List<PrayerTimeEntity>>

    @Query(
        "SELECT * FROM prayer_times WHERE date = :date AND latitude = :lat AND longitude = :lng LIMIT 1"
    )
    suspend fun getPrayerTimeByDateOnce(date: String, lat: Double, lng: Double): PrayerTimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(vararg prayerTimes: PrayerTimeEntity)

    @Query("DELETE FROM prayer_times WHERE date < :date")
    suspend fun deleteOlderThan(date: String)
}
