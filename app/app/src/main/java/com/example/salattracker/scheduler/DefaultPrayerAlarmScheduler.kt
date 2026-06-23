package com.example.salattracker.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.salattracker.data.local.PrayerTimeEntity
import com.example.salattracker.lock.LockManager
import com.example.salattracker.receiver.PrayerAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [PrayerAlarmScheduler] using [AlarmManager].
 *
 * Schedules exact alarms via [AlarmManager.setExactAndAllowWhileIdle] which
 * fires even during Doze mode — critical for on-time prayer locks.
 */
@Singleton
class DefaultPrayerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : PrayerAlarmScheduler {

    companion object {
        private const val TAG = "PrayerAlarmScheduler"
        private const val GRACE_PERIOD_MINUTES = 15L
        private val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleExactAlarm(prayerName: String, triggerAtMillis: Long, isLockTrigger: Boolean) {
        val pendingIntent = buildPendingIntent(prayerName, isLockTrigger)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d(TAG, "Scheduled exact alarm for $prayerName at $triggerAtMillis (isLockTrigger=$isLockTrigger)")
    }

    override fun cancelAlarm(prayerName: String) {
        // Cancel both initial and lock-trigger alarms
        val initialIntent = buildPendingIntent(prayerName, false)
        alarmManager.cancel(initialIntent)
        val lockIntent = buildPendingIntent(prayerName, true)
        alarmManager.cancel(lockIntent)
        Log.d(TAG, "Cancelled alarms for $prayerName (initial + lock)")
    }

    override fun cancelAll() {
        PRAYER_NAMES.forEach { cancelAlarm(it) }
        Log.d(TAG, "Cancelled all prayer alarms")
    }

    /**
     * Intelligently schedule alarms for all prayers based on current time.
     *
     * For each prayer:
     * 1. BEFORE prayer time → schedule initial alarm (fires at prayer time).
     * 2. WITHIN grace period (0–15 min after prayer) → schedule lock alarm at grace end.
     * 3. AFTER grace period (still in prayer window) → immediately lock.
     */
    override fun scheduleAllAlarmsForToday(entity: PrayerTimeEntity): Int {
        cancelAll()

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        var scheduledCount = 0

        // Build prayer windows: (name, start, end)
        val fajr = LocalTime.parse(entity.fajr, TIME_FORMAT)
        val sunrise = LocalTime.parse(entity.sunrise, TIME_FORMAT)
        val dhuhr = LocalTime.parse(entity.dhuhr, TIME_FORMAT)
        val asr = LocalTime.parse(entity.asr, TIME_FORMAT)
        val maghrib = LocalTime.parse(entity.maghrib, TIME_FORMAT)
        val isha = LocalTime.parse(entity.isha, TIME_FORMAT)

        data class PrayerWindow(val name: String, val start: LocalTime, val end: LocalTime)
        val windows = listOf(
            PrayerWindow("Fajr", fajr, sunrise),
            PrayerWindow("Dhuhr", dhuhr, asr),
            PrayerWindow("Asr", asr, maghrib),
            PrayerWindow("Maghrib", maghrib, isha),
            PrayerWindow("Isha", isha, LocalTime.of(23, 59)),
        )

        val nowTime = now.toLocalTime()

        for (window in windows) {
            val graceEnd = window.start.plusMinutes(GRACE_PERIOD_MINUTES)

            when {
                // Case 1: Current time is BEFORE this prayer → schedule initial alarm
                nowTime.isBefore(window.start) -> {
                    val triggerDateTime = LocalDateTime.of(today, window.start)
                    val triggerMillis = triggerDateTime.atZone(zone).toInstant().toEpochMilli()
                    scheduleExactAlarm(window.name, triggerMillis, isLockTrigger = false)
                    scheduledCount++
                    Log.d(TAG, "Scheduled INITIAL alarm for ${window.name} at ${window.start}")
                }

                // Case 2: Current time is WITHIN grace period → schedule lock alarm at grace end
                !nowTime.isBefore(window.start) && nowTime.isBefore(graceEnd) -> {
                    val lockDateTime = LocalDateTime.of(today, graceEnd)
                    val triggerMillis = lockDateTime.atZone(zone).toInstant().toEpochMilli()
                    scheduleExactAlarm(window.name, triggerMillis, isLockTrigger = true)
                    scheduledCount++
                    Log.d(TAG, "In GRACE period for ${window.name} — lock scheduled at $graceEnd")
                }

                // Case 3: Current time is AFTER grace period but still in prayer window → lock NOW
                !nowTime.isBefore(graceEnd) && nowTime.isBefore(window.end) -> {
                    Log.d(TAG, "PAST grace period for ${window.name} — locking immediately")
                    LockManager.setLocked(true, window.name)
                    // No alarm needed — lock is already active
                }

                // Case 4: Prayer window has passed entirely → skip
                else -> {
                    Log.d(TAG, "Skipped ${window.name} — already passed")
                }
            }
        }

        Log.d(TAG, "scheduleAllAlarmsForToday complete: $scheduledCount alarms scheduled")
        return scheduledCount
    }

    private fun buildPendingIntent(prayerName: String, isLockTrigger: Boolean = false): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
            putExtra("IS_LOCK_TRIGGER", isLockTrigger)
        }
        return PendingIntent.getBroadcast(
            context,
            prayerName.hashCode() + isLockTrigger.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
