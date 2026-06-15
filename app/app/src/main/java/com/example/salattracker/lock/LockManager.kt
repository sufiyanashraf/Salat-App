package com.example.salattracker.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that holds the global app-lock state.
 *
 * Other components (AccessibilityService, UI) collect [isLocked] and
 * [currentPrayer] to decide whether to show / hide the blocking overlay.
 */
object LockManager {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _currentPrayer = MutableStateFlow<String?>("Fajr")
    val currentPrayer: StateFlow<String?> = _currentPrayer.asStateFlow()

    /**
     * Update the lock state and, optionally, the prayer that triggered it.
     *
     * @param locked  `true` to engage the lock, `false` to release it.
     * @param prayer  Name of the active prayer (e.g. "Fajr", "Dhuhr").
     */
    fun setLocked(locked: Boolean, prayer: String? = null) {
        _isLocked.value = locked
        if (prayer != null) {
            _currentPrayer.value = prayer
        }
    }
}
