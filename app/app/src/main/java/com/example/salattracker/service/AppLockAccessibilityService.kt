package com.example.salattracker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Telephony
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.salattracker.lock.LockManager
import com.example.salattracker.ui.lock.LockScreenOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Monitors foreground app changes and enforces the prayer-time lock.
 *
 * When [LockManager.isLocked] is `true`, a full-screen Compose overlay
 * is drawn over every non-whitelisted app via [WindowManager] using
 * [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY].
 */
class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLock"
    }

    private var serviceScope: CoroutineScope? = null
    private var isCurrentlyLocked = false

    // ── overlay state ──────────────────────────────────────────
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var overlayLifecycleOwner: ServiceLifecycleOwner? = null
    private var isOverlayAttached = false

    private val overlayParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Omitting FLAG_NOT_FOCUSABLE → overlay intercepts Back button.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
    }

    // ── lifecycle ───────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        initOverlayView()

        serviceScope = CoroutineScope(Dispatchers.Main + Job()).also { scope ->
            scope.launch {
                LockManager.isLocked.collect { locked ->
                    isCurrentlyLocked = locked
                    Log.d(TAG, "Lock state changed: $locked")
                    if (locked) {
                        attachOverlay()
                    } else {
                        detachOverlay()
                    }
                }
            }
        }
    }

    // ── event handling ──────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (!isCurrentlyLocked) return

        val whitelist = buildWhitelist()
        if (packageName in whitelist) {
            // Whitelisted app → temporarily remove overlay so user can use it
            detachOverlay()
        } else {
            // Non-whitelisted app → ensure overlay is blocking
            attachOverlay()
            Log.d(TAG, "Blocking app: $packageName")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        detachOverlay()
        overlayLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        overlayLifecycleOwner = null
        composeView = null
        serviceScope?.cancel()
        serviceScope = null
    }

    // ── overlay management ─────────────────────────────────────

    /**
     * Initialises the [ComposeView] + lifecycle bridge once.
     * The view is NOT attached to the window until [attachOverlay] is called.
     */
    private fun initOverlayView() {
        val lifecycleOwner = ServiceLifecycleOwner().also { overlayLifecycleOwner = it }
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                LockScreenOverlay(
                    currentPrayer = LockManager.currentPrayer.value,
                    onEmergencyUnlockHold = {
                        LockManager.setLocked(false)
                    },
                )
            }
        }

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /** Attach the overlay to the screen (idempotent). */
    private fun attachOverlay() {
        if (isOverlayAttached) return
        val view = composeView ?: return
        try {
            windowManager?.addView(view, overlayParams)
            isOverlayAttached = true
            Log.d(TAG, "Overlay attached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay", e)
        }
    }

    /** Detach the overlay from the screen (idempotent). */
    private fun detachOverlay() {
        if (!isOverlayAttached) return
        val view = composeView ?: return
        try {
            windowManager?.removeView(view)
            isOverlayAttached = false
            Log.d(TAG, "Overlay detached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detach overlay", e)
        }
    }

    // ── whitelist ───────────────────────────────────────────────

    /**
     * Build a dynamic whitelist that adapts to OEM-specific default apps
     * instead of hard-coding package names.
     */
    private fun buildWhitelist(): Set<String> {
        val whitelist = mutableSetOf<String>()

        // Always allow the system telecom service
        whitelist.add("com.android.server.telecom")

        // Allow this app itself
        whitelist.add(packageName)

        // Resolve the default dialer app
        try {
            val dialerInfo = packageManager.resolveActivity(
                Intent(Intent.ACTION_DIAL),
                0
            )
            dialerInfo?.activityInfo?.packageName?.let { whitelist.add(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve default dialer", e)
        }

        // Resolve the default SMS app
        try {
            Telephony.Sms.getDefaultSmsPackage(this)?.let { whitelist.add(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve default SMS app", e)
        }

        return whitelist
    }
}
