package com.example.salattracker.ui.lock

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.salattracker.data.local.SalatDatabase
import com.example.salattracker.data.preferences.UserPreferences
import com.example.salattracker.lock.LockManager
import com.example.salattracker.ml.ImageClassifierHelper
import com.example.salattracker.scheduler.DefaultPrayerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ── Prayer-specific colours ────────────────────────────────────────

private data class PrayerTheme(
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val prayerThemes = mapOf(
    "Fajr"    to PrayerTheme(Color(0xFF1A237E), Color(0xFF4A148C)),   // deep indigo → purple (pre-dawn)
    "Dhuhr"   to PrayerTheme(Color(0xFFF57F17), Color(0xFFFF8F00)),   // warm amber (midday)
    "Asr"     to PrayerTheme(Color(0xFFE65100), Color(0xFFBF360C)),   // burnt orange (afternoon)
    "Maghrib" to PrayerTheme(Color(0xFFAD1457), Color(0xFF880E4F)),   // rose → magenta (sunset)
    "Isha"    to PrayerTheme(Color(0xFF0D47A1), Color(0xFF1A237E)),   // dark blue (night)
)

private val defaultTheme = PrayerTheme(Color(0xFF1B5E20), Color(0xFF2E7D32)) // green fallback

// ── Constants ──────────────────────────────────────────────────────

private const val EMERGENCY_HOLD_MILLIS = 10_000L
private const val PROGRESS_TICK_MILLIS = 100L

// ── Composable ─────────────────────────────────────────────────────

/**
 * Full-screen lock overlay displayed over non-essential apps during prayer time.
 *
 * @param currentPrayer        Active prayer name (e.g. "Fajr", "Isha"). Drives the background gradient.
 * @param onEmergencyUnlockHold Invoked after the user holds the bypass button for 10 continuous seconds.
 * @param onVerifySuccess       Invoked when the camera verifies a prayer mat, unlocking the device.
 */
@Composable
fun LockScreenOverlay(
    currentPrayer: String?,
    onEmergencyUnlockHold: () -> Unit,
    onVerifySuccess: () -> Unit = {},
) {
    val theme = prayerThemes[currentPrayer] ?: defaultTheme

    var showCamera by remember { mutableStateOf(false) }

    if (showCamera) {
        CameraVerificationScreen(
            theme = theme,
            onVerifySuccess = onVerifySuccess,
            onCancel = { showCamera = false },
        )
    } else {
        LockContent(
            currentPrayer = currentPrayer,
            theme = theme,
            onEmergencyUnlockHold = onEmergencyUnlockHold,
            onVerifyClick = { showCamera = true },
        )
    }
}

// ── Lock content (default view) ────────────────────────────────────

@Composable
private fun LockContent(
    currentPrayer: String?,
    theme: PrayerTheme,
    onEmergencyUnlockHold: () -> Unit,
    onVerifyClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var holdProgress by remember { mutableStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress,
        label = "holdProgress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(theme.gradientStart, theme.gradientEnd))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            // ── Prayer name ────────────────────────────────────
            Text(
                text = currentPrayer?.uppercase() ?: "PRAYER",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Main message ───────────────────────────────────
            Text(
                text = "It's time for ${currentPrayer ?: "prayer"} prayer.",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your phone is locked until you pray.\nOpen the app and verify with a photo of your prayer mat.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Verify with Camera button ──────────────────────
            Button(
                onClick = onVerifyClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.25f),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text(
                    text = "Verify with Camera",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Snooze 15m button (hidden only if next prayer <= 30 mins away) ──
            var showSnooze by remember { mutableStateOf(true) }

            LaunchedEffect(currentPrayer) {
                showSnooze = try {
                    calculateShowSnooze(context, currentPrayer)
                } catch (e: Exception) {
                    // If we can't determine next prayer time, show snooze to be safe
                    true
                }
            }

            if (showSnooze) {
                OutlinedButton(
                    onClick = {
                        // Unlock and schedule a new lock alarm in 15 minutes
                        LockManager.setLocked(false)
                        val scheduler = DefaultPrayerAlarmScheduler(context)
                        scheduler.scheduleExactAlarm(
                            currentPrayer ?: "Unknown",
                            System.currentTimeMillis() + 15 * 60 * 1000L,
                            isLockTrigger = true
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Snooze 15m",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Emergency bypass button (10-second hold) ───────
            Text(
                text = "Emergency bypass",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Start incrementing progress
                                holdJob?.cancel()
                                holdProgress = 0f

                                holdJob = scope.launch {
                                    val steps = (EMERGENCY_HOLD_MILLIS / PROGRESS_TICK_MILLIS).toInt()
                                    for (i in 1..steps) {
                                        delay(PROGRESS_TICK_MILLIS)
                                        holdProgress = i.toFloat() / steps
                                    }
                                    // Completed the full 10-second hold
                                    onEmergencyUnlockHold()
                                }

                                // Wait for release or cancellation
                                val released = tryAwaitRelease()
                                if (released || holdProgress < 1f) {
                                    // Finger lifted early — cancel and reset
                                    holdJob?.cancel()
                                    holdProgress = 0f
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (holdProgress > 0f) "${(holdProgress * 10).toInt()}s" else "Hold",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Hold progress bar ──────────────────────────────
            if (animatedProgress > 0f) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(4.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Hold for 10 seconds to unlock",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp,
            )
        }
    }
}

// ── Camera verification screen ─────────────────────────────────────

@Composable
private fun CameraVerificationScreen(
    theme: PrayerTheme,
    onVerifySuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(theme.gradientStart, theme.gradientEnd))
            ),
    ) {
        if (!hasCameraPermission) {
            // ── Permission denied fallback ──────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Please grant camera permission in the app settings to verify your prayer mat.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.25f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Go Back")
                }
            }
        } else {
            // ── Camera preview + capture ────────────────────────
            val imageCapture = remember { ImageCapture.Builder().build() }
            val classifierHelper = remember {
                ImageClassifierHelper(context).also { it.setupImageClassifier() }
            }

            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            } catch (e: Exception) {
                                // Camera binding failed — user can try again
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Unbind camera when leaving this screen
            DisposableEffect(Unit) {
                onDispose {
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                        cameraProvider.unbindAll()
                    } catch (_: Exception) { }
                }
            }

            // ── Overlay buttons on top of camera ────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Cancel button
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel", modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // Capture button
                    Button(
                        onClick = {
                            if (isProcessing) return@Button
                            isProcessing = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val success = captureAndVerify(
                                        imageCapture = imageCapture,
                                        executor = ContextCompat.getMainExecutor(context),
                                        classifierHelper = classifierHelper,
                                    )
                                    if (success) {
                                        onVerifySuccess()
                                    } else {
                                        errorMessage = "Mat not detected. Please try again."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Capture failed. Try again."
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                    ) {
                        Text(
                            text = if (isProcessing) "Verifying…" else "Capture",
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Point at your prayer mat and tap Capture",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

// ── Capture + classify helper ──────────────────────────────────────

private suspend fun captureAndVerify(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    classifierHelper: ImageClassifierHelper,
): Boolean = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val bitmap = imageProxy.toBitmap()
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val success = classifierHelper.classify(bitmap, rotation)
                    continuation.resume(success)
                } finally {
                    imageProxy.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        })
    }
}

// ── Snooze safety check ────────────────────────────────────────────

private val PRAYER_ORDER = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Returns true if the "Snooze 15m" button should be shown.
 * The button is hidden if the next prayer is <= 30 minutes away.
 *
 * Special case: If the current prayer is Isha (last of the day),
 * we query tomorrow's Fajr time to determine the gap.
 */
private suspend fun calculateShowSnooze(
    context: android.content.Context,
    currentPrayer: String?
): Boolean = withContext(Dispatchers.IO) {
    if (currentPrayer == null) return@withContext false

    val location = UserPreferences.getLocation(context).firstOrNull()
        ?: return@withContext false
    val (lat, lng) = location

    val dao = SalatDatabase.getInstance(context).prayerTimeDao()
    val today = LocalDate.now().toString()
    val todayTimes = dao.getPrayerTimeByDateOnce(today, lat, lng)
        ?: return@withContext false

    val currentIndex = PRAYER_ORDER.indexOf(currentPrayer)
    if (currentIndex == -1) return@withContext false

    val now = LocalTime.now()

    val nextPrayerTime: LocalTime? = if (currentIndex < PRAYER_ORDER.size - 1) {
        // Not Isha — next prayer is today
        val nextPrayerName = PRAYER_ORDER[currentIndex + 1]
        val timeStr = when (nextPrayerName) {
            "Fajr" -> todayTimes.fajr
            "Dhuhr" -> todayTimes.dhuhr
            "Asr" -> todayTimes.asr
            "Maghrib" -> todayTimes.maghrib
            "Isha" -> todayTimes.isha
            else -> null
        }
        timeStr?.let { LocalTime.parse(it, TIME_FORMAT) }
    } else {
        // Isha — next prayer is tomorrow's Fajr
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val tomorrowTimes = dao.getPrayerTimeByDateOnce(tomorrow, lat, lng)
        tomorrowTimes?.fajr?.let { LocalTime.parse(it, TIME_FORMAT) }
    }

    if (nextPrayerTime == null) return@withContext false

    val minutesUntilNext = if (currentIndex < PRAYER_ORDER.size - 1) {
        ChronoUnit.MINUTES.between(now, nextPrayerTime)
    } else {
        // For Isha → Fajr (crosses midnight), add 24h if result is negative
        val diff = ChronoUnit.MINUTES.between(now, nextPrayerTime)
        if (diff < 0) diff + 24 * 60 else diff
    }

    minutesUntilNext > 30
}
