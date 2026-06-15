package com.example.salattracker.ui.lock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * @param currentPrayer  Active prayer name (e.g. "Fajr", "Isha"). Drives the background gradient.
 * @param onEmergencyUnlockHold  Invoked after the user holds the bypass button for 10 continuous seconds.
 */
@Composable
fun LockScreenOverlay(
    currentPrayer: String?,
    onEmergencyUnlockHold: () -> Unit,
) {
    val theme = prayerThemes[currentPrayer] ?: defaultTheme

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
