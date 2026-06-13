# Pitfalls Research

**Domain:** Android Habit Enforcement / Accessibility Service Overlay Apps
**Researched:** 2026-06-14
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: OS Killing the Accessibility Service (The "Silent Death")

**What goes wrong:**
The app mysteriously stops locking during prayer times. The user checks their phone, realizes it's prayer time, but the lock never engaged. The habit is broken, and the user loses trust in the app.

**Why it happens:**
Android OEMs (especially Xiaomi, Oppo, Vivo, Samsung) employ aggressive battery management that kills background services. Accessibility Services are often collateral damage when the user swipes the app away from the "Recent Apps" menu.

**How to avoid:**
Implement an onboarding flow that guides the user to disable battery optimization for the app (using strategies from `dontkillmyapp.com`). Keep a persistent Foreground Service with a notification detailing the next prayer time to reduce the chance of being killed. Detect when the service is disconnected and prompt the user to re-enable it.

**Warning signs:**
Users reporting the app "only works if I open it first." The `onServiceConnected` callback firing repeatedly after long periods of inactivity.

**Phase to address:**
Core Service & Lifecycle Management

---

### Pitfall 2: Locking Out Emergency and Core Functions

**What goes wrong:**
The user's phone is locked during prayer time, and they need to make an emergency call, answer an incoming call, or respond to an urgent text. The overlay blocks the dialer, putting the user in a dangerous situation.

**Why it happens:**
The accessibility service is programmed to block everything except the app's own `package name`, failing to account for the fragmented ecosystem of Android dialers and SMS apps.

**How to avoid:**
Do not hardcode a single package (like `com.google.android.dialer`). Instead, query the system for apps that handle `ACTION_DIAL`, `ACTION_CALL`, and SMS intents, and dynamically add them to the whitelist. Explicitly whitelist Android System UI so the notification shade and incoming call screens are visible. Include an emergency "Panic Unlock" that ruins their streak but grants immediate access.

**Warning signs:**
Testing an incoming call on the emulator/device and seeing a black screen instead of the caller ID.

**Phase to address:**
Overlay & Whitelist Implementation

---

### Pitfall 3: Poor ML Performance in Real-World Conditions (Fajr/Isha Darkness)

**What goes wrong:**
The user prays Fajr (pre-dawn) or Isha (night) in a dimly lit room. The on-device ML model fails to recognize the prayer mat, leaving the user permanently locked out of their phone until the next prayer time ends.

**Why it happens:**
The model was trained on well-lit, perfectly framed stock photos of prayer mats. Real-world usage involves low light, odd angles, blurry camera feeds, and diverse mat designs.

**How to avoid:**
Add an in-app toggle for the flashlight during the camera verification step. Implement a fallback mechanism (e.g., a "Skip this time" button that logs a failed habit day but unlocks the phone). Augment the training data with low-light and noisy images.

**Warning signs:**
High verification failure rates specifically during early morning or late night hours.

**Phase to address:**
ML Model Integration

---

### Pitfall 4: Getting Bypassed via Settings or Recent Apps

**What goes wrong:**
Gen Z users quickly find a loophole to bypass the lock without taking a photo. They might rapidly press the home button, open Settings from the quick settings panel, force stop the app, or uninstall it.

**Why it happens:**
The overlay is too slow to react to window state changes, or the app fails to specifically block the Android Settings app and package installer.

**How to avoid:**
The accessibility service must aggressively monitor `TYPE_WINDOW_STATE_CHANGED` events. If the newly opened window is the Settings app (specifically the App Info or Accessibility screens) or the Package Installer, immediately draw the overlay. Provide a mechanism within your app to gracefully disable the lock *if* they want to quit, rather than letting them circumvent it.

**Warning signs:**
Users spending zero time on the verification screen but somehow still using other apps during prayer times.

**Phase to address:**
Overlay Hardening

---

### Pitfall 5: Sudden Blackouts (No Pre-Lock Warning)

**What goes wrong:**
The user is writing an important email or mid-game, and suddenly the screen is taken over by the prayer lock. They lose their work and get frustrated with the app.

**Why it happens:**
The scheduler triggers the lock precisely at the prayer time without any prior warning or grace period.

**How to avoid:**
Implement a "5-minute warning" notification or a subtle toast indicating the lock is imminent. Allow a 3-5 minute "snooze" functionality for the start time to let users gracefully wrap up their current task.

**Warning signs:**
User complaints about lost data or interrupting critical tasks.

**Phase to address:**
Scheduling & UX

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Polling time continuously | Easy to trigger locks | Destroys battery life; OS kills app | Never acceptable |
| Hardcoding specific whitelisted apps | Faster MVP development | Fails on Samsung/Xiaomi devices with custom dialers | Prototyping only |
| Evaluating all Accessibility events | Simple logic | Massive CPU/Battery drain; laggy device | Never acceptable |
| Using `AlarmManager.RTC` instead of Exact Alarms | Avoids Android 12+ permission hurdles | Locks happen 10-15 minutes late due to Doze mode | Never acceptable |

## Integration Gotchas

Common mistakes when connecting to external services.

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Aladhan API | Fetching times daily without caching | Fetch a whole month at once; cache locally (Room/DataStore) to handle offline days |
| Location Services | Requesting location constantly for prayer times | Fetch once on setup, allow manual city entry, only update on major network change |
| Default Camera App | Firing an Intent to default camera to take a photo | Default camera app gets blocked by your own overlay! Use CameraX directly in your app. |

## Performance Traps

Patterns that work at small scale but fail as usage grows.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Unfiltered Accessibility Config | Phone stutters when scrolling; battery drains fast | Set `packageNames` to null (monitor all), but filter `eventTypes` to ONLY `TYPE_WINDOW_STATE_CHANGED` | Within hours of installation |
| Heavy ML Model (>20MB) | App takes long to open; crashes on older devices | Use TFLite quantized models; keep model under 5MB | On low-end Android devices |
| Memory Leaks in Overlay | Overlay takes longer to appear over time | Ensure the overlay View is properly detached and garbage collected when hidden | After 2-3 days of uptime |

## Security Mistakes

Domain-specific security issues beyond general web security.

| Mistake | Risk | Prevention |
|---------|------|------------|
| Unsecured "Bypass" variables | Users use memory editors (GameGuardian) to skip lock | Store verification state in encrypted SharedPreferences/DataStore |
| Not handling Safe Mode | Users reboot to Safe Mode to uninstall app | Cannot easily prevent, but educate user that self-discipline is still required |
| Exposing ML confidence threshold | Users find exactly how blurry a photo can be to pass | Hardcode or obfuscate the acceptance threshold |

## UX Pitfalls

Common user experience mistakes in this domain.

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Overwhelming Onboarding | Users drop off when hit with 5 permission requests at once | Explain *why* each permission is needed before asking. Stagger requests. |
| Vague ML Rejection | User takes 5 photos, all rejected, doesn't know why | Show realtime bounding box or specific error ("Too dark", "No pattern detected") |
| Permanent Lock on Error | App crashes during verification, leaving device locked | Fail-open design: if the app crashes, the overlay service should die, freeing the device. |

## "Looks Done But Isn't" Checklist

Things that appear complete but are missing critical pieces.

- [ ] **Accessibility Service:** Works on Pixel emulator — verify on Samsung/Xiaomi physical device (battery management).
- [ ] **Overlay Drawing:** Appears over normal apps — verify it draws over the notification shade and recent apps menu.
- [ ] **Camera Integration:** Can take a photo — verify it works without needing to launch external blocked camera apps (use CameraX).
- [ ] **Alarms/Scheduling:** Works when screen is on — verify Exact Alarms fire when the device has been in Doze mode for 4 hours.

## Recovery Strategies

When pitfalls occur despite prevention, how to recover.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| ML Model fails for 20% of users | HIGH | Push emergency update with lower threshold; add manual "Emergency Unlock" button |
| OS kills background service | MEDIUM | Add in-app prominent banner "Service Stopped - Fix Here" routing to battery settings |
| Accidental block of core app | LOW | Update whitelist remotely via Firebase Remote Config or push an immediate app update |

## Pitfall-to-Phase Mapping

How roadmap phases should address these pitfalls.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| OS Killing Service | Core Infrastructure | Leave device locked/asleep for 12 hours, verify alarm still fires |
| Bypass via Settings | Overlay Implementation | Attempt to open Settings/Uninstall during lock; verify immediate block |
| ML Low Light Failure | ML Verification | Test camera verification in a completely dark room using flash |
| Emergency Lockout | Whitelist & Permissions | Attempt to receive a call and make an SOS call during an active block |
| Delayed Exact Alarms | Scheduling Integration | Put device in Doze mode using ADB, verify alarm triggers exactly on minute |

## Sources

- Android Developer Documentation (AccessibilityService, Doze mode)
- Community discussions on Reddit (r/AndroidDev) regarding background service restrictions (Don't Kill My App)
- Post-mortems from similar app-blocking/focus apps (Forest, Stay Focused)
- Known limitations of CameraX and external Intents in overlay apps.

---
*Pitfalls research for: Android Prayer Lock App*
*Researched: 2026-06-14*
