package com.example.salattracker.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salattracker.data.local.SalatDatabase
import com.example.salattracker.data.preferences.UserPreferences
import com.example.salattracker.data.remote.AladhanApiService
import com.example.salattracker.data.repository.PrayerTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Calculation method options ──
// Maps Aladhan API method ID → display name
private val CALCULATION_METHODS = listOf(
    1 to "University of Islamic Sciences, Karachi",
    2 to "Islamic Society of North America (ISNA)",
    3 to "Muslim World League (MWL)",
    4 to "Umm Al-Qura University, Makkah",
    5 to "Egyptian General Authority of Survey",
    7 to "University of Tehran",
    8 to "Gulf Region",
    9 to "Kuwait",
    10 to "Qatar",
    11 to "Singapore",
    12 to "France (UOIF)",
    13 to "Turkey (Diyanet)",
    0 to "Ithna Ashari (Jafari)",
)

// ── Asr school options ──
private val ASR_SCHOOLS = listOf(
    0 to "Standard (Shafi'i, Maliki, Hanbali)",
    1 to "Hanafi",
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMethod by remember { mutableIntStateOf(1) }
    var selectedSchool by remember { mutableIntStateOf(0) }

    // Load current values from DataStore
    LaunchedEffect(Unit) {
        val method = UserPreferences.getCalculationMethod(context).firstOrNull() ?: 1
        val school = UserPreferences.getAsrSchool(context).firstOrNull() ?: 0
        selectedMethod = method
        selectedSchool = school
    }

    // Helper to save, re-fetch prayer times, and notify
    fun saveAndRefresh() {
        scope.launch {
            UserPreferences.setCalculationMethod(context, selectedMethod)
            UserPreferences.setAsrSchool(context, selectedSchool)

            // Re-fetch prayer times with the new settings
            try {
                val location = UserPreferences.getLocation(context).firstOrNull()
                if (location != null) {
                    val (lat, lng) = location
                    val dao = SalatDatabase.getInstance(context).prayerTimeDao()
                    val api = AladhanApiService()
                    val repo = PrayerTimeRepository(dao, api)
                    withContext(Dispatchers.IO) {
                        repo.fetchAndCacheTodayPrayerTimes(lat, lng, selectedMethod, selectedSchool)
                    }
                    api.close()
                }
            } catch (e: Exception) {
                // Non-fatal — times will update next sync
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Settings saved — prayer times updated", Toast.LENGTH_SHORT).show()
            }
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Calculation Method ──
        Text(
            text = "Calculation Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Determines Fajr and Isha angles used to calculate prayer times.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                CALCULATION_METHODS.forEachIndexed { index, (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMethod = id }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMethod == id,
                            onClick = { selectedMethod = id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (index < CALCULATION_METHODS.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Asr Juristic School ──
        Text(
            text = "Asr Juristic School",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "The Hanafi method calculates Asr when the shadow is twice the object's length, making it later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ASR_SCHOOLS.forEachIndexed { index, (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSchool = id }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedSchool == id,
                            onClick = { selectedSchool = id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (index < ASR_SCHOOLS.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Save button ──
        androidx.compose.material3.Button(
            onClick = { saveAndRefresh() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Save & Update Times",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
