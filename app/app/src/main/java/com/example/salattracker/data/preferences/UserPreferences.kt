package com.example.salattracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object UserPreferences {

    private val LATITUDE = doublePreferencesKey("latitude")
    private val LONGITUDE = doublePreferencesKey("longitude")
    private val IS_LOCKED = booleanPreferencesKey("is_locked")
    private val LAST_PRAYER_NAME = stringPreferencesKey("last_prayer_name")

    fun getLocation(context: Context): Flow<Pair<Double, Double>?> {
        return context.userPreferencesDataStore.data.map { preferences ->
            val lat = preferences[LATITUDE]
            val lng = preferences[LONGITUDE]
            if (lat != null && lng != null) Pair(lat, lng) else null
        }
    }

    suspend fun saveLocation(context: Context, lat: Double, lng: Double) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[LATITUDE] = lat
            preferences[LONGITUDE] = lng
        }
    }

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
}
