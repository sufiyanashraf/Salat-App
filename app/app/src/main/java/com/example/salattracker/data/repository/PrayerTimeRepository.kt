package com.example.salattracker.data.repository

import com.example.salattracker.data.local.PrayerTimeDao
import com.example.salattracker.data.local.PrayerTimeEntity
import com.example.salattracker.data.remote.AladhanApiService
import com.example.salattracker.data.remote.AladhanData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrayerTimeRepository(
    private val dao: PrayerTimeDao,
    private val api: AladhanApiService
) {

    private val apiDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val dbDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getTodayPrayerTimes(lat: Double, lng: Double): Flow<PrayerTimeEntity?> {
        val today = LocalDate.now().format(dbDateFormatter)
        return dao.getPrayerTimeByDate(today, lat, lng)
    }

    suspend fun getTodayPrayerTimesOnce(lat: Double, lng: Double): PrayerTimeEntity? {
        val today = LocalDate.now().format(dbDateFormatter)
        return dao.getPrayerTimeByDateOnce(today, lat, lng)
    }

    fun getPrayerTimesForMonth(yearMonth: String, lat: Double, lng: Double): Flow<List<PrayerTimeEntity>> {
        return dao.getPrayerTimesForMonth(yearMonth, lat, lng)
    }

    suspend fun fetchAndCacheTodayPrayerTimes(
        lat: Double,
        lng: Double,
        method: Int = 1,
        school: Int = 0
    ) {
        val today = LocalDate.now().format(apiDateFormatter)
        val response = api.getTimingsByDate(today, lat, lng, method, school)
        val entity = mapToEntity(response.data, lat, lng)
        dao.insertPrayerTimes(entity)
    }

    suspend fun fetchAndCacheMonthlyPrayerTimes(
        year: Int,
        month: Int,
        lat: Double,
        lng: Double,
        method: Int = 1,
        school: Int = 0
    ) {
        val response = api.getTimingsForMonth(year, month, lat, lng, method, school)
        val entities = response.data.map { data -> mapToEntity(data, lat, lng) }
        dao.insertPrayerTimes(*entities.toTypedArray())
    }

    suspend fun cleanupOldEntries() {
        val cutoff = LocalDate.now().minusDays(7).format(dbDateFormatter)
        dao.deleteOlderThan(cutoff)
    }

    private fun mapToEntity(data: AladhanData, lat: Double, lng: Double): PrayerTimeEntity {
        val apiDate = data.date.gregorian.date // "DD-MM-YYYY"
        val parsedDate = LocalDate.parse(apiDate, apiDateFormatter)
        val dbDate = parsedDate.format(dbDateFormatter) // "yyyy-MM-dd"

        return PrayerTimeEntity(
            date = dbDate,
            fajr = extractTime(data.timings.Fajr),
            dhuhr = extractTime(data.timings.Dhuhr),
            asr = extractTime(data.timings.Asr),
            maghrib = extractTime(data.timings.Maghrib),
            isha = extractTime(data.timings.Isha),
            latitude = lat,
            longitude = lng
        )
    }

    /**
     * Aladhan API returns times like "05:12 (PKT)" — strip the timezone suffix.
     */
    private fun extractTime(rawTime: String): String {
        return rawTime.split(" ").first()
    }
}
