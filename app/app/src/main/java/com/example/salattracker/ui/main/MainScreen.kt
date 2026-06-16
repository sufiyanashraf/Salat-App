package com.example.salattracker.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.salattracker.data.DefaultDataRepository
import com.example.salattracker.scheduler.DefaultPrayerAlarmScheduler
import com.example.salattracker.theme.SalatTrackerTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // ── Camera permission request ──────────────────────────────────
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { /* no-op: permission result handled silently */ }

  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED
    ) {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Column(modifier = modifier) {
    when (state) {
      MainScreenUiState.Loading -> {
        // Blank
      }
      is MainScreenUiState.Success -> {
        MainScreen(data = (state as MainScreenUiState.Success).data, modifier = Modifier)
      }
      is MainScreenUiState.Error -> {
        Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}")
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Debug: Test alarm button to verify end-to-end scheduling flow
    Button(
      onClick = {
        val scheduler = DefaultPrayerAlarmScheduler(context)
        val triggerTime = System.currentTimeMillis() + 60_000L // 1 minute from now
        scheduler.scheduleExactAlarm("TestPrayer", triggerTime)
        Toast.makeText(context, "Test alarm scheduled in 1 minute", Toast.LENGTH_SHORT).show()
      },
      modifier = Modifier.fillMaxWidth(),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiary
      )
    ) {
      Text("Test Alarm (+1 min)")
    }
  }
}

@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
  Column(modifier) { data.forEach { Greeting(it) } }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  SalatTrackerTheme { MainScreen(listOf("Android")) }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  SalatTrackerTheme { MainScreen(listOf("Android")) }
}

