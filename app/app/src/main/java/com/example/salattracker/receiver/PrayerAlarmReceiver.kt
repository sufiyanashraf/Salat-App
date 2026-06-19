package com.example.salattracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.salattracker.lock.LockManager
import com.example.salattracker.scheduler.DefaultPrayerAlarmScheduler

/**
 * BroadcastReceiver triggered by AlarmManager when a prayer time arrives.
 *
 * Two modes:
 * - isLockTrigger=false (initial alarm): Silently schedules a lock alarm 15 minutes later.
 * - isLockTrigger=true (delayed lock): Activates the lock overlay and wakes the screen.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        private const val WAKE_LOCK_TIMEOUT_MS = 3000L
        private const val SNOOZE_DELAY_MS = 15 * 60 * 1000L // 15 minutes
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Unknown"
        val isLockTrigger = intent.getBooleanExtra("IS_LOCK_TRIGGER", false)
        Log.d(TAG, "Alarm received for prayer: $prayerName (isLockTrigger=$isLockTrigger)")

        if (!isLockTrigger) {
            // Initial prayer time alarm — schedule the actual lock 15 minutes from now
            val scheduler = DefaultPrayerAlarmScheduler(context)
            scheduler.scheduleExactAlarm(
                prayerName,
                System.currentTimeMillis() + SNOOZE_DELAY_MS,
                isLockTrigger = true
            )
            Log.d(TAG, "Silent snooze: lock scheduled in 15 minutes for $prayerName")
        } else {
            // Lock trigger — activate the overlay now
            LockManager.setLocked(true, prayerName)

            // Acquire a WakeLock to turn on the screen so the user sees the lock immediately
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SalatTracker:PrayerAlarmWakeLock"
            )
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }
}

