package com.example.salattracker.scheduler

import com.example.salattracker.data.local.PrayerTimeEntity

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

    /**
     * Intelligently schedule all prayer alarms for today based on the current time.
     *
     * For each prayer:
     * - If current time is BEFORE prayer time → schedule initial alarm at prayer time.
     * - If current time is WITHIN the 15-min grace period → schedule lock alarm for grace end.
     * - If current time is AFTER the grace period (still in prayer window) → lock immediately.
     *
     * Returns the number of alarms scheduled.
     */
    fun scheduleAllAlarmsForToday(entity: PrayerTimeEntity): Int
}
