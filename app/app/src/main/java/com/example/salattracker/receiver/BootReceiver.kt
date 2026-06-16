package com.example.salattracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.salattracker.worker.PrayerTimeWorker

/**
 * BroadcastReceiver that listens for [Intent.ACTION_BOOT_COMPLETED].
 *
 * AlarmManager state is wiped on device restart, so this receiver
 * enqueues a [PrayerTimeWorker] to immediately restore prayer alarms.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed — restoring prayer alarms")

            val workRequest = OneTimeWorkRequestBuilder<PrayerTimeWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
