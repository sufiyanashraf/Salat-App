package com.example.salattracker.service

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Bridges the [Lifecycle] and [SavedStateRegistry] that Jetpack Compose
 * requires into a context that has neither (e.g. an [android.accessibilityservice.AccessibilityService]).
 *
 * Usage:
 * ```
 * val owner = ServiceLifecycleOwner()
 * owner.performRestore(null)
 * owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
 * composeView.setViewTreeLifecycleOwner(owner)
 * composeView.setViewTreeSavedStateRegistryOwner(owner)
 * ```
 */
class ServiceLifecycleOwner : SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    /** Restore any previously-saved state (pass `null` for a fresh start). */
    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }

    /** Drive the lifecycle forward (e.g. ON_CREATE → ON_START → ON_RESUME). */
    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
