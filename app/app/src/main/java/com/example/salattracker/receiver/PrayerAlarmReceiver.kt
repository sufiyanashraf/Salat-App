package com.example.salattracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.salattracker.lock.LockManager

/**
 * BroadcastReceiver triggered by AlarmManager when a prayer time arrives.
 *
 * Activates the lock overlay via [LockManager] and briefly wakes the screen
 * so the user immediately sees the lock.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        private const val WAKE_LOCK_TIMEOUT_MS = 3000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Unknown"
        Log.d(TAG, "Alarm received for prayer: $prayerName")

        // Trigger the global lock overlay
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
