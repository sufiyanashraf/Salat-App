# Architecture Research

**Domain:** Android App / System Utility / Machine Learning
**Researched:** 2026-06-14
**Confidence:** HIGH

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                          UI Layer                           │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ MainActivity │  │  Overlay UI  │  │ Settings UI  │       │
│  │ (Dashboard)  │  │ (Lock Screen)│  │ (Whitelist)  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │               │
├─────────┴─────────────────┴─────────────────┴───────────────┤
│                       Service Layer                         │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────────┐  ┌─────────────────────────┐    │
│  │ Accessibility Service  │  │ Prayer Scheduling Worker│    │
│  │ (Foreground App Mon.)  │  │ (AlarmManager/WorkMgr)  │    │
│  └──────────┬─────────────┘  └──────────┬──────────────┘    │
│             │                           │                   │
├─────────────┴───────────────────────────┴───────────────────┤
│                    Domain/Manager Layer                     │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ LockManager  │  │  MLVerifier  │  │ LocationMgr  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
├─────────┴─────────────────┴─────────────────┴───────────────┤
│                         Data Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Prayer Repo  │  │ Whitelist Repo│  │ State Prefs  │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| **MainActivity** | Onboarding, permissions requests, status dashboard. | Android Activity / Jetpack Compose |
| **Overlay UI** | The lock screen blocking access, hosts camera preview. | Android Activity with `NEW_TASK` / `SingleInstance` |
| **Accessibility Service** | Monitors `WINDOW_STATE_CHANGED` events to detect active app. | `AccessibilityService` subclass |
| **Prayer Scheduling Worker** | Fetches/calculates daily prayer times, schedules lock triggers. | `WorkManager` & `AlarmManager` |
| **LockManager** | Central truth for "should the device be locked right now?". | Kotlin Object / Singleton |
| **MLVerifier** | Processes camera frames to classify if they contain a prayer mat. | CameraX + TFLite/ML Kit |
| **Data Repositories** | Caches API times, saves user whitelist, stores "last prayed" state. | `Room` database or `DataStore` (Prefs) |

## Recommended Project Structure

```
app/src/main/java/com/salattracker/
├── ui/                 # UI components and view models
│   ├── overlay/        # Lock screen and camera UI
│   ├── settings/       # Whitelist and app settings
│   └── main/           # Dashboard and permissions
├── service/            # Background services
│   ├── accessibility/  # AppLockAccessibilityService
│   └── scheduling/     # PrayerTimeWorker, AlarmReceivers
├── domain/             # Business logic
│   ├── lock/           # LockManager, LockState rules
│   ├── ml/             # MLVerifier, ImageAnalyzer
│   └── time/           # PrayerTime calculator/API client
├── data/               # Repositories and local storage
│   ├── local/          # Room DB, DataStore
│   └── remote/         # Retrofit interfaces (Aladhan API)
└── di/                 # Dependency Injection modules
```

### Structure Rationale

- **ui/:** Separates user-facing components by feature (overlay, settings, main).
- **service/:** Isolates Android-specific background execution contexts which are heavily lifecycle-dependent.
- **domain/:** Contains the core logic that doesn't depend on Android UI, allowing easier testing of the lock rules and ML processing.
- **data/:** Abstracts away whether data is coming from the network (API) or local storage (DataStore/Room).

## Architectural Patterns

### Pattern 1: Accessibility Service App Blocker

**What:** Using Android's `AccessibilityService` to listen for `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`. When an app package moves to the foreground, check if it's whitelisted.
**When to use:** When building App Lockers, Screen Time limiters, or Focus apps on Android without requiring heavy Device Admin profiles.
**Trade-offs:** 
- *Pros:* Does not require complex MDM enrollment; works reliably on standard devices.
- *Cons:* Requires the user to dig into Settings to grant a sensitive permission. Prone to being killed by aggressive OEM battery managers (e.g., Xiaomi, Samsung).

**Example:**
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        val packageName = event.packageName?.toString() ?: return
        if (LockManager.isLocked() && !WhitelistRepo.isWhitelisted(packageName)) {
            // Launch overlay activity
            val intent = Intent(this, OverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }
}
```

### Pattern 2: On-Device ML Pipeline (CameraX + ImageAnalysis)

**What:** Streaming camera frames from `CameraX` directly into a local Machine Learning model (TFLite) without saving the image to disk.
**When to use:** For privacy-preserving real-time image verification (like checking for a prayer mat).
**Trade-offs:** 
- *Pros:* High privacy (no server uploads), works offline, zero server cost.
- *Cons:* Models must be small enough to bundle in the APK or download post-install, potential accuracy limits compared to heavy cloud models.

## Data Flow

### 1. Prayer Time Sync & Scheduling Flow

```
[AlarmManager] (Daily Trigger)
    ↓
[PrayerTimeWorker] → [LocationMgr] (Get Lat/Lon)
    ↓
[Retrofit Client] → [Aladhan API] (Fetch Times)
    ↓
[Prayer Repo] (Save Times) → [AlarmManager] (Schedule Lock Alarms)
```

### 2. App Launch & Locking Flow

```
[User Opens App]
    ↓
[Accessibility Service] (Detects Package Name)
    ↓
[Whitelist Repo] (Check if Exempt) → (If Yes) → [End]
    ↓ (If No)
[LockManager] (Check if Prayer Time && Not Prayed)
    ↓ (If Locked)
[Overlay UI] (Launch Lock Screen)
```

### 3. Unlock Verification Flow

```
[User Clicks 'Verify'] (In Overlay UI)
    ↓
[CameraX] (Captures Frame) → [MLVerifier] (Analyzes TFLite Model)
    ↓
[Mat Detected (High Confidence)]
    ↓
[LockManager] (Update `lastPrayedTime` to Current)
    ↓
[Overlay UI] (Finishes Activity) → [User Regains Access]
```

## Build Order & Dependencies

To structure the roadmap phases efficiently, the build order must follow dependency constraints (you can't lock apps until you have a whitelist, you can't verify ML until you have a lock screen).

1. **Phase 1: Foundation & Data (No UI/Services yet)**
   - Setup DataStore (Preferences)
   - Setup `WhitelistRepository`
   - Setup Location fetching and Aladhan API integration (`Prayer Repo`)
2. **Phase 2: Core Locking Engine**
   - Implement `AppLockAccessibilityService`
   - Build `LockManager` rules (e.g., fake the prayer times for testing)
   - Build a basic `OverlayActivity` (with a simple "Bypass" button for testing)
3. **Phase 3: Scheduling Automation**
   - Connect `Prayer Repo` to `AlarmManager`
   - Automatically trigger the `LockManager` state when real prayer times hit.
4. **Phase 4: ML & Verification**
   - Integrate CameraX into `OverlayActivity`.
   - Implement `MLVerifier` with a basic TFLite model.
   - Replace the fake "Bypass" button with real image verification.
5. **Phase 5: Polish & Edge Cases**
   - Handle device boot (re-register alarms via `BOOT_COMPLETED` receiver).
   - Boot loop preventions / Emergency overrides.
   - UI/UX polish and aggressive OEM battery optimizations guidance.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k users | On-device ML model embedded in APK. Free tier Aladhan API. |
| 1k-100k users | Move ML model to dynamic feature download to save base APK size. Implement caching for API to avoid rate limits if users spam refresh. |
| 100k+ users | May need custom prayer API backend if Aladhan blocks high traffic or requires paid tiers. |

### Scaling Priorities

1. **First bottleneck:** Battery management killing the Accessibility Service.
   - *Fix:* Provide clear in-app instructions for users to disable battery optimization for the app (using `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`).
2. **Second bottleneck:** False negatives in ML verification (app doesn't unlock).
   - *Fix:* Allow a timed fallback unlock, or collect opt-in failing images to retrain the model.

## Anti-Patterns

### Anti-Pattern 1: Cloud-Based Image Processing

**What people do:** Send the captured photo to a backend or a paid API (like OpenAI Vision) for verification.
**Why it's wrong:** Introduces latency, costs money per API call, breaks offline functionality, and ruins privacy guarantees.
**Do this instead:** Use `CameraX` ImageAnalyzer with a quantized TFLite model locally.

### Anti-Pattern 2: Drawing `SYSTEM_ALERT_WINDOW` for Lock Screen

**What people do:** Use `WindowManager` to draw a full-screen view floating over everything.
**Why it's wrong:** While it works for simple overlays, handling CameraX lifecycles inside a raw `View` attached via WindowManager is notoriously difficult and buggy. 
**Do this instead:** Have the Accessibility Service launch a standard Android `Activity` with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_SINGLE_TOP` to easily control lifecycle limits and handle ML camera views.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| **Aladhan API** | Retrofit / REST GET | Requires Lat/Lon. Calculate monthly times to reduce requests. |
| **Android AlarmManager** | `setExactAndAllowWhileIdle` | Necessary for precise lock triggers exactly at prayer time. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Accessibility Svc ↔ LockManager | Direct method call (Singleton/DI) | Service runs continuously; must query lock state fast without blocking main thread. |
| Overlay ↔ MLVerifier | Callbacks / StateFlow | Camera frames stream rapidly; process in background thread, emit result via StateFlow to UI. |

## Sources

- [Android AccessibilityService Documentation](https://developer.android.com/guide/topics/ui/accessibility/service)
- [CameraX ImageAnalysis Guide](https://developer.android.com/training/camerax/analyze)
- [TensorFlow Lite for Android](https://www.tensorflow.org/lite/android)
- [Aladhan Prayer Times API](https://aladhan.com/prayer-times-api)

---
*Architecture research for: Android Prayer Lock App*
*Researched: 2026-06-14*
