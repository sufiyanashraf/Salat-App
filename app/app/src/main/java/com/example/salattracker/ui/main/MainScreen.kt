package com.example.salattracker.ui.main

import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.salattracker.SetupWizard
import com.example.salattracker.data.local.PrayerTimeEntity
import com.example.salattracker.data.local.SalatDatabase
import com.example.salattracker.data.preferences.UserPreferences
import com.example.salattracker.lock.LockManager
import com.example.salattracker.scheduler.DefaultPrayerAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── Prayer-specific colours (matches lock screen) ──

private data class PrayerColor(val start: Color, val end: Color)

private val prayerColors = mapOf(
    "Fajr"    to PrayerColor(Color(0xFF1A237E), Color(0xFF4A148C)),
    "Dhuhr"   to PrayerColor(Color(0xFFF57F17), Color(0xFFFF8F00)),
    "Asr"     to PrayerColor(Color(0xFFE65100), Color(0xFFBF360C)),
    "Maghrib" to PrayerColor(Color(0xFFAD1457), Color(0xFF880E4F)),
    "Isha"    to PrayerColor(Color(0xFF0D47A1), Color(0xFF1A237E)),
)

private val PRAYER_ORDER = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // ── State ──
    var prayerTimes by remember { mutableStateOf<PrayerTimeEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var nextPrayer by remember { mutableStateOf<String?>(null) }
    var minutesUntilNext by remember { mutableLongStateOf(0L) }

    // ── Check required settings and redirect to setup wizard if missing ──
    LaunchedEffect(Unit) {
        val pm = context.getSystemService(PowerManager::class.java)
        val isBatteryOptimized = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val isAccessibilityEnabled = enabledServices?.contains(context.packageName) == true

        val location = UserPreferences.getLocation(context).firstOrNull()

        if (!isBatteryOptimized || !isAccessibilityEnabled || location == null) {
            onItemClick(SetupWizard)
            return@LaunchedEffect
        }

        // Load prayer times from the local DB
        val (lat, lng) = location
        val dao = SalatDatabase.getInstance(context).prayerTimeDao()
        val today = java.time.LocalDate.now().toString()
        prayerTimes = dao.getPrayerTimeByDateOnce(today, lat, lng)
        isLoading = false
    }

    // ── Live countdown ticker ──
    LaunchedEffect(prayerTimes) {
        val entity = prayerTimes ?: return@LaunchedEffect
        while (true) {
            val now = LocalTime.now()
            var found = false
            for (name in PRAYER_ORDER) {
                val timeStr = getPrayerTime(entity, name) ?: continue
                val prayerTime = LocalTime.parse(timeStr, TIME_FORMAT)
                if (prayerTime.isAfter(now)) {
                    nextPrayer = name
                    minutesUntilNext = ChronoUnit.MINUTES.between(now, prayerTime)
                    found = true
                    break
                }
            }
            if (!found) {
                nextPrayer = null
                minutesUntilNext = 0
            }
            delay(30_000) // refresh every 30 seconds
        }
    }

    // ── UI ──
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Header ──
        Text(
            text = "Salat Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = java.time.LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Next Prayer Hero Card ──
        if (nextPrayer != null) {
            val colors = prayerColors[nextPrayer] ?: PrayerColor(Color(0xFF1B5E20), Color(0xFF2E7D32))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(colors.start, colors.end)),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "NEXT PRAYER",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = nextPrayer ?: "",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val entity = prayerTimes
                        val timeStr = if (entity != null && nextPrayer != null)
                            getPrayerTime(entity, nextPrayer!!) else null

                        Row(verticalAlignment = Alignment.Bottom) {
                            if (timeStr != null) {
                                Text(
                                    text = timeStr,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = if (minutesUntilNext > 60)
                                    "${minutesUntilNext / 60}h ${minutesUntilNext % 60}m left"
                                else
                                    "${minutesUntilNext}m left",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else if (!isLoading && prayerTimes != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "All prayers completed ✓",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MashaAllah! See you tomorrow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Prayer Times List ──
        if (isLoading) {
            Text(
                text = "Loading prayer times…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (prayerTimes == null) {
            Text(
                text = "No prayer times available.\nOpen the app to sync.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val now = LocalTime.now()
                    PRAYER_ORDER.forEachIndexed { index, name ->
                        val timeStr = getPrayerTime(prayerTimes!!, name)
                        val prayerTime = timeStr?.let { LocalTime.parse(it, TIME_FORMAT) }
                        val isPast = prayerTime?.isBefore(now) == true
                        val isNext = name == nextPrayer
                        val colors = prayerColors[name]

                        PrayerTimeRow(
                            name = name,
                            time = timeStr ?: "--:--",
                            isPast = isPast,
                            isNext = isNext,
                            dotColor = colors?.start ?: MaterialTheme.colorScheme.primary
                        )

                        if (index < PRAYER_ORDER.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Status card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Protection active — your phone will lock at prayer times",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Test Lock Screen button ──
        Button(
            onClick = {
                // Use isLockTrigger=true to bypass the 15-min silent delay
                LockManager.setLocked(true, "Test")
                Toast.makeText(context, "Lock screen triggered!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Test Lock Screen")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Helper composables ──

@Composable
private fun PrayerTimeRow(
    name: String,
    time: String,
    isPast: Boolean,
    isNext: Boolean,
    dotColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (isNext) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPast) dotColor.copy(alpha = 0.3f)
                        else dotColor
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = if (isPast)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = if (isPast)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else if (isNext)
                dotColor
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Helpers ──

private fun getPrayerTime(entity: PrayerTimeEntity, name: String): String? {
    return when (name) {
        "Fajr" -> entity.fajr
        "Dhuhr" -> entity.dhuhr
        "Asr" -> entity.asr
        "Maghrib" -> entity.maghrib
        "Isha" -> entity.isha
        else -> null
    }
}
