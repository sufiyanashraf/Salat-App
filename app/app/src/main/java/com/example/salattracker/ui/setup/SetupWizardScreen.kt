package com.example.salattracker.ui.setup

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SetupWizardScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── State for each permission/setting ──
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isBatteryOptimized by remember { mutableStateOf(false) }
    var isCameraGranted by remember { mutableStateOf(false) }

    // ── Refresh state when returning from settings ──
    fun refreshState() {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        isAccessibilityEnabled = enabledServices?.contains(context.packageName) == true

        val pm = context.getSystemService(PowerManager::class.java)
        isBatteryOptimized = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        isCameraGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Initial check + re-check when resuming from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Camera permission launcher ──
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isCameraGranted = granted
    }

    val allGranted = isAccessibilityEnabled && isBatteryOptimized && isCameraGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Setup Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Salat Tracker needs a few permissions to enforce your prayer habits. Complete all steps below to get started.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Step 1: Accessibility Service ──
        SetupStepCard(
            stepNumber = 1,
            title = "Enable Accessibility Service",
            description = "This lets Salat Tracker detect when you open apps and show the prayer lock screen.",
            hint = "Tip: If the toggle is greyed out, go to App Info → ⋮ menu → Allow restricted settings first.",
            isComplete = isAccessibilityEnabled,
            buttonText = if (isAccessibilityEnabled) "Enabled ✓" else "Open Settings",
            onButtonClick = {
                if (!isAccessibilityEnabled) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        )

        // ── Step 2: Battery Optimization ──
        SetupStepCard(
            stepNumber = 2,
            title = "Disable Battery Optimization",
            description = "Prevents Android from killing Salat Tracker in the background during prayer times.",
            hint = null,
            isComplete = isBatteryOptimized,
            buttonText = if (isBatteryOptimized) "Disabled ✓" else "Disable Optimization",
            onButtonClick = {
                if (!isBatteryOptimized) {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            }
        )

        // ── Step 3: Camera Permission ──
        SetupStepCard(
            stepNumber = 3,
            title = "Grant Camera Access",
            description = "Used to verify your prayer by taking a photo of a prayer mat.",
            hint = null,
            isComplete = isCameraGranted,
            buttonText = if (isCameraGranted) "Granted ✓" else "Grant Permission",
            onButtonClick = {
                if (!isCameraGranted) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Continue Button ──
        Button(
            onClick = { if (allGranted) onFinish() },
            enabled = allGranted,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (allGranted) "Continue" else "Complete all steps above",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    hint: String?,
    isComplete: Boolean,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Step $stepNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = onButtonClick,
                enabled = !isComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}
