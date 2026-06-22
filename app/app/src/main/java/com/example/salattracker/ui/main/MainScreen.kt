package com.example.salattracker.ui.main

import android.location.Geocoder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import com.example.salattracker.Settings as SettingsNav
import com.example.salattracker.SetupWizard
import com.example.salattracker.data.local.PrayerTimeEntity
import com.example.salattracker.data.local.SalatDatabase
import com.example.salattracker.data.preferences.UserPreferences
import com.example.salattracker.data.remote.AladhanApiService
import com.example.salattracker.data.repository.PrayerTimeRepository
import com.example.salattracker.lock.LockManager
import com.example.salattracker.scheduler.DefaultPrayerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
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

// Full display order including Sunrise (not a prayer)
private val DISPLAY_ORDER = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
// Only actual prayers (for alarm scheduling)
private val PRAYER_ORDER = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private const val GRACE_PERIOD_MINUTES = 15L

// ── Hero card state ──
private enum class HeroState { GRACE, ACTIVE, WAITING, ALL_DONE }
private data class HeroInfo(
    val state: HeroState,
    val prayerName: String = "",
    val timeStr: String = "",
    val countdownMinutes: Long = 0,
    val countdownLabel: String = ""
)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // ── State ──
    var prayerTimes by remember { mutableStateOf<PrayerTimeEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var heroInfo by remember { mutableStateOf(HeroInfo(HeroState.WAITING)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var locationName by remember { mutableStateOf<String?>(null) }

    // ── Check required settings, then load/fetch prayer times ──
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

        val (lat, lng) = location

        // Location name fallback: if missing, reverse geocode now
        val savedName = UserPreferences.getLocationName(context).firstOrNull()
        if (savedName != null) {
            locationName = savedName
        } else {
            try {
                val name = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, java.util.Locale.getDefault())
                        .getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val country = addr.countryName
                        listOfNotNull(city, country).joinToString(", ")
                    } else null
                }
                if (name != null) {
                    locationName = name
                    UserPreferences.saveLocation(context, lat, lng, name)
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Reverse geocode failed", e)
            }
        }

        val dao = SalatDatabase.getInstance(context).prayerTimeDao()
        val today = LocalDate.now().toString()

        // Step 1: Try local DB first
        var entity = withContext(Dispatchers.IO) {
            dao.getPrayerTimeByDateOnce(today, lat, lng)
        }

        // Step 2: If not in DB, fetch directly from API
        if (entity == null) {
            Log.d("MainScreen", "No cached prayer times, fetching from API...")
            try {
                val method = UserPreferences.getCalculationMethod(context).firstOrNull() ?: 1
                val school = UserPreferences.getAsrSchool(context).firstOrNull() ?: 0
                val api = AladhanApiService()
                val repo = PrayerTimeRepository(dao, api)
                withContext(Dispatchers.IO) {
                    repo.fetchAndCacheTodayPrayerTimes(lat, lng, method, school)
                    entity = dao.getPrayerTimeByDateOnce(today, lat, lng)
                }
                api.close()
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to fetch prayer times", e)
                errorMessage = "Could not fetch prayer times. Check your internet connection."
            }
        }

        // Step 3: Schedule alarms for remaining prayers
        if (entity != null) {
            try {
                val scheduler = DefaultPrayerAlarmScheduler(context)
                val now = java.time.LocalDateTime.now()
                val todayDate = LocalDate.now()
                val zone = java.time.ZoneId.systemDefault()

                for (name in PRAYER_ORDER) {
                    val timeStr = getTimeFromEntity(entity!!, name) ?: continue
                    val prayerTime = LocalTime.parse(timeStr, TIME_FORMAT)
                    val prayerDateTime = java.time.LocalDateTime.of(todayDate, prayerTime)

                    if (prayerDateTime.isAfter(now)) {
                        val triggerMillis = prayerDateTime.atZone(zone).toInstant().toEpochMilli()
                        scheduler.scheduleExactAlarm(name, triggerMillis)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to schedule alarms", e)
            }
        }

        prayerTimes = entity
        isLoading = false
    }

    // ── Live countdown ticker — calculates the 3-state hero info ──
    LaunchedEffect(prayerTimes) {
        val entity = prayerTimes ?: return@LaunchedEffect
        while (true) {
            heroInfo = calculateHeroInfo(entity)
            delay(15_000) // refresh every 15 seconds for accurate countdowns
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

        // ── Header with settings icon ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Salat Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = LocalDate.now().format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (locationName != null) {
                    Text(
                        text = "\uD83D\uDCCD $locationName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { onItemClick(SettingsNav) }) {
                Text(
                    text = "\u2699\uFE0F",
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Hero Card ──
        when (heroInfo.state) {
            HeroState.GRACE -> {
                // Grace period: prayer just started, lock coming in X minutes
                val colors = prayerColors[heroInfo.prayerName]
                    ?: PrayerColor(Color(0xFF1B5E20), Color(0xFF2E7D32))
                HeroCard(
                    gradientStart = colors.start,
                    gradientEnd = colors.end,
                ) {
                    Text(
                        text = "PRAYER TIME",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = heroInfo.prayerName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = heroInfo.timeStr,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                Color.Red.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚠\uFE0F  Screen locks in ${heroInfo.countdownLabel}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HeroState.ACTIVE -> {
                // Active prayer period: lock has triggered, counting down to next boundary
                val colors = prayerColors[heroInfo.prayerName]
                    ?: PrayerColor(Color(0xFF1B5E20), Color(0xFF2E7D32))
                HeroCard(
                    gradientStart = colors.start,
                    gradientEnd = colors.end,
                ) {
                    Text(
                        text = "ACTIVE PRAYER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = heroInfo.prayerName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = heroInfo.timeStr,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Ends in ${heroInfo.countdownLabel}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            HeroState.WAITING -> {
                // Waiting for next prayer
                val colors = prayerColors[heroInfo.prayerName]
                    ?: PrayerColor(Color(0xFF1B5E20), Color(0xFF2E7D32))
                HeroCard(
                    gradientStart = colors.start,
                    gradientEnd = colors.end,
                ) {
                    Text(
                        text = "NEXT PRAYER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = heroInfo.prayerName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = heroInfo.timeStr,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Starts in ${heroInfo.countdownLabel}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            HeroState.ALL_DONE -> {
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Prayer Times List (with Sunrise) ──
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading prayer times…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (prayerTimes == null) {
            Text(
                text = errorMessage ?: "No prayer times available.\nCheck your internet and restart the app.",
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
                    DISPLAY_ORDER.forEachIndexed { index, name ->
                        val timeStr = getTimeFromEntity(prayerTimes!!, name)
                        val prayerTime = timeStr?.let { LocalTime.parse(it, TIME_FORMAT) }
                        val isPast = prayerTime?.isBefore(now) == true
                        val isSunrise = name == "Sunrise"
                        val isActive = name == heroInfo.prayerName &&
                            (heroInfo.state == HeroState.GRACE || heroInfo.state == HeroState.ACTIVE)
                        val isNext = name == heroInfo.prayerName && heroInfo.state == HeroState.WAITING
                        val dotColor = prayerColors[name]?.start

                        PrayerTimeRow(
                            name = name,
                            time = timeStr ?: "--:--",
                            isPast = isPast,
                            isActive = isActive,
                            isNext = isNext,
                            isSunrise = isSunrise,
                            dotColor = dotColor ?: MaterialTheme.colorScheme.primary
                        )

                        if (index < DISPLAY_ORDER.size - 1) {
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

// ── Reusable Hero Card ──

@Composable
private fun HeroCard(
    gradientStart: Color,
    gradientEnd: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(gradientStart, gradientEnd)),
                    RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column { content() }
        }
    }
}

// ── Prayer Time Row (updated for sunrise) ──

@Composable
private fun PrayerTimeRow(
    name: String,
    time: String,
    isPast: Boolean,
    isActive: Boolean,
    isNext: Boolean,
    isSunrise: Boolean,
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
            if (isSunrise) {
                // Sun icon instead of dot
                Text(
                    text = "☀\uFE0F",
                    fontSize = 14.sp,
                    modifier = Modifier.width(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(if (isActive || isNext) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPast) dotColor.copy(alpha = 0.3f)
                            else dotColor
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive || isNext) FontWeight.Bold
                    else if (isSunrise) FontWeight.Light
                    else FontWeight.Normal,
                color = if (isPast)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else if (isSunrise)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive || isNext) FontWeight.Bold
                else if (isSunrise) FontWeight.Light
                else FontWeight.Normal,
            color = if (isPast)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else if (isActive)
                dotColor
            else if (isNext)
                dotColor
            else if (isSunrise)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Core hero calculation logic ──

/**
 * Determines the hero card state based on the current time and prayer schedule.
 *
 * Time windows:
 * - [Prayer Start] to [Prayer Start + 15 min] → GRACE (lock warning)
 * - [Prayer Start + 15 min] to [Next Boundary] → ACTIVE (prayer active, counting down to end)
 * - [Next Boundary] to [Next Prayer Start] → WAITING (counting down to next prayer)
 *
 * Boundaries:
 * - Fajr ends at Sunrise
 * - Dhuhr ends at Asr
 * - Asr ends at Maghrib
 * - Maghrib ends at Isha
 * - Isha ends at midnight (23:59)
 */
private fun calculateHeroInfo(entity: PrayerTimeEntity): HeroInfo {
    val now = LocalTime.now()

    // Build time pairs: (prayerName, startTime, endTime)
    data class PrayerWindow(
        val name: String,
        val start: LocalTime,
        val end: LocalTime
    )

    val fajr = LocalTime.parse(entity.fajr, TIME_FORMAT)
    val sunrise = LocalTime.parse(entity.sunrise, TIME_FORMAT)
    val dhuhr = LocalTime.parse(entity.dhuhr, TIME_FORMAT)
    val asr = LocalTime.parse(entity.asr, TIME_FORMAT)
    val maghrib = LocalTime.parse(entity.maghrib, TIME_FORMAT)
    val isha = LocalTime.parse(entity.isha, TIME_FORMAT)

    val windows = listOf(
        PrayerWindow("Fajr", fajr, sunrise),
        PrayerWindow("Dhuhr", dhuhr, asr),
        PrayerWindow("Asr", asr, maghrib),
        PrayerWindow("Maghrib", maghrib, isha),
        PrayerWindow("Isha", isha, LocalTime.of(23, 59)),
    )

    // Check each prayer window
    for (window in windows) {
        val graceEnd = window.start.plusMinutes(GRACE_PERIOD_MINUTES)

        if (!now.isBefore(window.start) && now.isBefore(graceEnd)) {
            // GRACE period
            val mins = ChronoUnit.MINUTES.between(now, graceEnd)
            return HeroInfo(
                state = HeroState.GRACE,
                prayerName = window.name,
                timeStr = window.start.format(TIME_FORMAT),
                countdownMinutes = mins,
                countdownLabel = formatCountdown(mins)
            )
        }

        if (!now.isBefore(graceEnd) && now.isBefore(window.end)) {
            // ACTIVE period
            val mins = ChronoUnit.MINUTES.between(now, window.end)
            return HeroInfo(
                state = HeroState.ACTIVE,
                prayerName = window.name,
                timeStr = window.start.format(TIME_FORMAT),
                countdownMinutes = mins,
                countdownLabel = formatCountdown(mins)
            )
        }
    }

    // Not inside any window — find next upcoming prayer
    val allPrayers = listOf(
        "Fajr" to fajr,
        "Dhuhr" to dhuhr,
        "Asr" to asr,
        "Maghrib" to maghrib,
        "Isha" to isha,
    )

    for ((name, time) in allPrayers) {
        if (time.isAfter(now)) {
            val mins = ChronoUnit.MINUTES.between(now, time)
            return HeroInfo(
                state = HeroState.WAITING,
                prayerName = name,
                timeStr = time.format(TIME_FORMAT),
                countdownMinutes = mins,
                countdownLabel = formatCountdown(mins)
            )
        }
    }

    // All prayers are past
    return HeroInfo(state = HeroState.ALL_DONE)
}

private fun formatCountdown(minutes: Long): String {
    return if (minutes > 60) "${minutes / 60}h ${minutes % 60}m"
    else "${minutes}m"
}

// ── Helpers ──

private fun getTimeFromEntity(entity: PrayerTimeEntity, name: String): String? {
    return when (name) {
        "Fajr" -> entity.fajr
        "Sunrise" -> entity.sunrise
        "Dhuhr" -> entity.dhuhr
        "Asr" -> entity.asr
        "Maghrib" -> entity.maghrib
        "Isha" -> entity.isha
        else -> null
    }
}
