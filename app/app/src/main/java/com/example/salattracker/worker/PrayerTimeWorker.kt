package com.example.salattracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.salattracker.data.repository.PrayerTimeRepository
import com.example.salattracker.scheduler.PrayerAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Worker that fetches today's prayer times and schedules exact alarms
 * for each remaining prayer. Runs daily via PeriodicWorkRequest and
 * on-demand after device boot via BootReceiver.
 *
 * Hard-coded coordinates (Islamabad, Pakistan) are used until location
 * services are implemented in a future phase.
 */
@HiltWorker
class PrayerTimeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PrayerTimeRepository,
    private val scheduler: PrayerAlarmScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PrayerTimeWorker"
        const val WORK_NAME = "prayer_time_sync"

        // Hard-coded location (Islamabad) until location phase
        private const val DEFAULT_LAT = 33.6844
        private const val DEFAULT_LNG = 73.0479

        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        private val PRAYER_FIELDS = listOf(
            "Fajr" to { entity: com.example.salattracker.data.local.PrayerTimeEntity -> entity.fajr },
            "Dhuhr" to { entity: com.example.salattracker.data.local.PrayerTimeEntity -> entity.dhuhr },
            "Asr" to { entity: com.example.salattracker.data.local.PrayerTimeEntity -> entity.asr },
            "Maghrib" to { entity: com.example.salattracker.data.local.PrayerTimeEntity -> entity.maghrib },
            "Isha" to { entity: com.example.salattracker.data.local.PrayerTimeEntity -> entity.isha }
        )
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting prayer time sync")

            // Try to get cached times first, fetch from network if missing
            var entity = repository.getTodayPrayerTimesOnce(DEFAULT_LAT, DEFAULT_LNG)
            if (entity == null) {
                Log.d(TAG, "No cached times found, fetching from network")
                repository.fetchAndCacheTodayPrayerTimes(DEFAULT_LAT, DEFAULT_LNG)
                entity = repository.getTodayPrayerTimesOnce(DEFAULT_LAT, DEFAULT_LNG)
            }

            if (entity == null) {
                Log.e(TAG, "Failed to retrieve prayer times after fetch")
                return Result.retry()
            }

            // Cancel all existing alarms before rescheduling
            scheduler.cancelAll()

            val now = LocalDateTime.now()
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            var scheduledCount = 0

            for ((prayerName, timeGetter) in PRAYER_FIELDS) {
                val timeStr = timeGetter(entity)
                val prayerTime = LocalTime.parse(timeStr, TIME_FORMATTER)
                val prayerDateTime = LocalDateTime.of(today, prayerTime)

                // Only schedule alarms for prayers that haven't passed yet
                if (prayerDateTime.isAfter(now)) {
                    val triggerMillis = prayerDateTime.atZone(zone).toInstant().toEpochMilli()
                    scheduler.scheduleExactAlarm(prayerName, triggerMillis)
                    scheduledCount++
                    Log.d(TAG, "Scheduled $prayerName at $timeStr ($triggerMillis)")
                } else {
                    Log.d(TAG, "Skipped $prayerName ($timeStr) — already passed")
                }
            }

            Log.d(TAG, "Prayer time sync complete: $scheduledCount alarms scheduled")

            // Clean up old entries
            repository.cleanupOldEntries()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Prayer time sync failed", e)
            Result.retry()
        }
    }
}
