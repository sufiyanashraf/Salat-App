package com.example.salattracker.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.salattracker.data.preferences.UserPreferences
import com.example.salattracker.data.repository.PrayerTimeRepository
import com.example.salattracker.scheduler.PrayerAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Worker that fetches today's prayer times and schedules exact alarms
 * for each remaining prayer. Runs daily via PeriodicWorkRequest and
 * on-demand after device boot via BootReceiver.
 *
 * Reads the user's saved location from DataStore. If no location is
 * available, the worker fails gracefully and retries later.
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
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting prayer time sync")

            // Read location from DataStore — fail gracefully if not set
            val location = UserPreferences.getLocation(applicationContext).firstOrNull()
            if (location == null) {
                Log.e(TAG, "No location saved — user must complete setup first")
                return Result.failure()
            }
            val (lat, lng) = location
            Log.d(TAG, "Using location: lat=$lat, lng=$lng")

            // Read user's calculation preferences
            val method = UserPreferences.getCalculationMethod(applicationContext).firstOrNull() ?: 1
            val school = UserPreferences.getAsrSchool(applicationContext).firstOrNull() ?: 0
            Log.d(TAG, "Using method=$method, school=$school")

            // Try to get cached times first, fetch from network if missing
            var entity = repository.getTodayPrayerTimesOnce(lat, lng)
            if (entity == null) {
                Log.d(TAG, "No cached times found, fetching from network")
                repository.fetchAndCacheTodayPrayerTimes(lat, lng, method, school)
                entity = repository.getTodayPrayerTimesOnce(lat, lng)
            }

            if (entity == null) {
                Log.e(TAG, "Failed to retrieve prayer times after fetch")
                return Result.retry()
            }

            // Intelligently schedule alarms (handles grace period & immediate lock)
            val scheduledCount = scheduler.scheduleAllAlarmsForToday(entity)
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
