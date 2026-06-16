package com.example.salattracker.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.salattracker.receiver.PrayerAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleExactAlarm(prayerName: String, triggerAtMillis: Long) {
        val pendingIntent = buildPendingIntent(prayerName)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d(TAG, "Scheduled exact alarm for $prayerName at $triggerAtMillis")
    }

    override fun cancelAlarm(prayerName: String) {
        val pendingIntent = buildPendingIntent(prayerName)
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for $prayerName")
    }

    override fun cancelAll() {
        PRAYER_NAMES.forEach { cancelAlarm(it) }
        Log.d(TAG, "Cancelled all prayer alarms")
    }

    private fun buildPendingIntent(prayerName: String): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
        }
        return PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
