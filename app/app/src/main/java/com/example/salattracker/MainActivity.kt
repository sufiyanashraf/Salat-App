package com.example.salattracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.salattracker.theme.SalatTrackerTheme
import com.example.salattracker.worker.PrayerTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Schedule periodic prayer time sync (every 12 hours)
    val syncRequest = PeriodicWorkRequestBuilder<PrayerTimeWorker>(
        12, TimeUnit.HOURS
    ).build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        PrayerTimeWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )

    enableEdgeToEdge()
    setContent {
      SalatTrackerTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
