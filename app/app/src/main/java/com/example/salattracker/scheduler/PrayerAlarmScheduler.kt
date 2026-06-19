package com.example.salattracker.scheduler

import android.app.PendingIntent

/**
 * Abstraction over AlarmManager scheduling for prayer time alarms.
 * Allows swapping implementations for testing.
 */
interface PrayerAlarmScheduler {

    /**
     * Schedule an exact alarm that fires at [triggerAtMillis] for the given [prayerName].
     * Uses AlarmManager.setExactAndAllowWhileIdle under the hood.
     */
    fun scheduleExactAlarm(prayerName: String, triggerAtMillis: Long, isLockTrigger: Boolean = false)

    /**
     * Cancel a previously scheduled alarm for the given [prayerName].
     */
    fun cancelAlarm(prayerName: String)

    /**
     * Cancel all scheduled prayer alarms.
     */
    fun cancelAll()
}
