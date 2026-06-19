package com.example.salattracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object UserPreferences {

    private val LATITUDE = doublePreferencesKey("latitude")
    private val LONGITUDE = doublePreferencesKey("longitude")
    private val LOCATION_NAME = stringPreferencesKey("location_name")
    private val IS_LOCKED = booleanPreferencesKey("is_locked")
    private val LAST_PRAYER_NAME = stringPreferencesKey("last_prayer_name")
    private val CALCULATION_METHOD = intPreferencesKey("calculation_method")
    private val ASR_SCHOOL = intPreferencesKey("asr_school")

    // ── Location ──

    fun getLocation(context: Context): Flow<Pair<Double, Double>?> {
        return context.userPreferencesDataStore.data.map { preferences ->
            val lat = preferences[LATITUDE]
            val lng = preferences[LONGITUDE]
            if (lat != null && lng != null) Pair(lat, lng) else null
        }
    }

    suspend fun saveLocation(context: Context, lat: Double, lng: Double, name: String? = null) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[LATITUDE] = lat
            preferences[LONGITUDE] = lng
            if (name != null) {
                preferences[LOCATION_NAME] = name
            }
        }
    }

    fun getLocationName(context: Context): Flow<String?> {
        return context.userPreferencesDataStore.data.map { preferences ->
            preferences[LOCATION_NAME]
        }
    }

    // ── Lock state ──

    fun getIsLocked(context: Context): Flow<Boolean> {
        return context.userPreferencesDataStore.data.map { preferences ->
            preferences[IS_LOCKED] ?: false
        }
    }

    suspend fun setIsLocked(context: Context, locked: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[IS_LOCKED] = locked
        }
    }

    fun getLastPrayerName(context: Context): Flow<String?> {
        return context.userPreferencesDataStore.data.map { preferences ->
            preferences[LAST_PRAYER_NAME]
        }
    }

    suspend fun setLastPrayerName(context: Context, name: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[LAST_PRAYER_NAME] = name
        }
    }

    // ── Calculation settings ──

    /**
     * Aladhan API calculation methods:
     * 0 = Jafari, 1 = University of Islamic Sciences Karachi,
     * 2 = ISNA, 3 = MWL, 4 = Umm Al-Qura, 5 = Egyptian,
     * 7 = University of Tehran, 8 = Gulf Region,
     * 9 = Kuwait, 10 = Qatar, 11 = Singapore,
     * 12 = France, 13 = Turkey (Diyanet), 14 = Spiritual Administration of Muslims of Russia
     */
    fun getCalculationMethod(context: Context): Flow<Int> {
        return context.userPreferencesDataStore.data.map { preferences ->
            preferences[CALCULATION_METHOD] ?: 1 // Default: Karachi (common in South Asia)
        }
    }

    suspend fun setCalculationMethod(context: Context, method: Int) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[CALCULATION_METHOD] = method
        }
    }

    /**
     * Asr juristic method:
     * 0 = Standard (Shafi, Maliki, Hanbali)
     * 1 = Hanafi
     */
    fun getAsrSchool(context: Context): Flow<Int> {
        return context.userPreferencesDataStore.data.map { preferences ->
            preferences[ASR_SCHOOL] ?: 0 // Default: Standard
        }
    }

    suspend fun setAsrSchool(context: Context, school: Int) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[ASR_SCHOOL] = school
        }
    }
}
