package com.example.salattracker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.salattracker.lock.LockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Monitors foreground app changes and enforces the prayer-time lock.
 *
 * When [LockManager.isLocked] is `true`, any app not in the dynamic
 * whitelist (default dialer, default SMS, telecom server) is blocked.
 * For now "blocking" means logging — the overlay is added in a later plan.
 */
class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLock"
    }

    private var serviceScope: CoroutineScope? = null
    private var isCurrentlyLocked = false

    // ── lifecycle ───────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")

        serviceScope = CoroutineScope(Dispatchers.Main + Job()).also { scope ->
            scope.launch {
                LockManager.isLocked.collect { locked ->
                    isCurrentlyLocked = locked
                    Log.d(TAG, "Lock state changed: $locked")
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
        if (packageName !in whitelist) {
            Log.d(TAG, "Blocking app: $packageName")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope?.cancel()
        serviceScope = null
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
