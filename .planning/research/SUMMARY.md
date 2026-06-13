# Project Research Summary

**Project:** Salat Tracker
**Domain:** Android App / System Utility / Digital Wellness / Machine Learning
**Researched:** 2026-06-14
**Confidence:** HIGH

## Executive Summary

Salat Tracker is an Android digital wellness app that enforces the habit of praying by locking the user's phone during prayer times until they take a photo of their prayer mat. The recommended approach utilizes an Android AccessibilityService for robust app blocking, combined with MediaPipe Tasks Vision for on-device ML image classification to preserve privacy and function completely offline.

The main risks involve aggressive Android battery management killing the background service and the app accidentally blocking critical communications during emergencies. These are mitigated by an explicit onboarding flow for battery settings, a robust dynamic whitelist, and emergency fallback unlock mechanisms.

## Key Findings

### Recommended Stack

The stack prioritizes modern Android development and privacy-first on-device operations.

**Core technologies:**
- **Kotlin (2.4.0):** Primary Language — Standard for Android; Coroutines/Flow manage async tasks smoothly.
- **Jetpack Compose BOM (2026.04.01):** UI Toolkit — Declarative UI is ideal for rapid development of overlays and settings.
- **Android AccessibilityService:** App Monitoring & Blocking — Provides instant window state events and full-screen overlay capabilities without device admin rights.
- **MediaPipe Tasks Vision (0.10.35):** On-device ML Inference — Modern successor to raw TFLite; handles preprocessing and inference locally.
- **CameraX (1.6.1):** Camera Capture — Simplifies Camera2 API complexities for reliable image capture.

### Expected Features

**Must have (table stakes):**
- Accurate Prayer Times API integration
- Accessibility Service Lock (Overlay)
- Hardcoded Whitelist (Phone, Messages)
- Manual Unlock Fallback

**Should have (competitive):**
- Physical Action Verification (Camera Capture Flow)
- On-Device ML Verification

**Defer (v2+):**
- Customizable Whitelist
- Grace Period Warning
- Streak Tracker
- Advanced Custom Schedules

### Architecture Approach

The architecture relies on a clear separation of UI, Service, Domain, and Data layers to isolate lifecycle-dependent Android components from business logic.

**Major components:**
1. **UI Layer (MainActivity, Overlay UI, Settings):** Handles onboarding, renders the lock screen overlay, and manages user preferences.
2. **Service Layer (Accessibility Service, Scheduling Worker):** Monitors active apps in the background and schedules precise lock triggers via AlarmManager.
3. **Domain/Manager Layer (LockManager, MLVerifier):** Holds the central truth for the lock state and processes camera frames through the ML pipeline.
4. **Data Layer (Prayer Repo, Whitelist Repo, State Prefs):** Caches API times via Room/DataStore to ensure offline functionality.

### Critical Pitfalls

1. **OS Killing the Accessibility Service** — Aggressive OEM battery managers. **Avoid by:** Guiding users to disable battery optimizations and using persistent foreground services.
2. **Locking Out Emergency Functions** — Blocking dialers or SMS. **Avoid by:** Dynamically whitelisting ACTION_DIAL/CALL/SMS handlers and the System UI, plus adding a panic unlock.
3. **Poor ML Performance in Low Light** — Fajr/Isha lighting. **Avoid by:** Adding an in-app flashlight toggle and manual fallback mechanism.
4. **Getting Bypassed via Settings** — Users circumventing the lock. **Avoid by:** Aggressively monitoring and blocking the Android Settings and Package Installer.
5. **Sudden Blackouts** — Locking without warning. **Avoid by:** Implementing a grace period warning.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Foundation & Data
**Rationale:** Must establish local storage and API integration before building locking mechanisms.
**Delivers:** DataStore preferences, WhitelistRepo, Aladhan API integration (Ktor), Room DB.
**Addresses:** Accurate Prayer Times fetching.
**Avoids:** Accidental lockouts due to missing local state or network dependency.

### Phase 2: Core Locking Engine
**Rationale:** The core blocking mechanism needs to be reliable before scheduling or ML verification.
**Delivers:** AppLockAccessibilityService, LockManager rules, basic OverlayActivity (with manual bypass).
**Uses:** AccessibilityService, Jetpack Compose.
**Implements:** Accessibility Service App Blocker pattern.

### Phase 3: Scheduling Automation
**Rationale:** Locks need to happen automatically at specific times without user intervention.
**Delivers:** PrayerTimeWorker, AlarmManager integration, connection to Prayer Repo.
**Addresses:** Accurate automatic triggering of the lock state.
**Avoids:** Doze mode delaying locks by using Exact Alarms.

### Phase 4: ML & Verification
**Rationale:** Replaces manual bypass with the competitive differentiator: physical camera verification.
**Delivers:** CameraX integration, MLVerifier, MediaPipe Tasks integration in OverlayActivity.
**Addresses:** Physical Action Verification, On-Device ML Verification.
**Avoids:** Poor ML performance by including flashlight toggle and handling verification failures gracefully.

### Phase 5: Polish & Hardening
**Rationale:** Required to survive aggressive OEM environments and user tampering before launch.
**Delivers:** Boot completion handlers, battery optimization onboarding, dynamic whitelisting of emergency apps.
**Addresses:** OS Killing Service, Getting bypassed via settings, Locking out emergency functions.

### Phase Ordering Rationale

- Data and state repositories are built first to support offline-first capabilities.
- The core locking mechanism is built next, as it is the foundation of the app.
- Automation and ML verification are layered on top of the working lock.
- Finally, system hardening ensures reliability against OEM battery managers and user interference.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 4:** ML model availability — Needs validation if the default MediaPipe object detection model is sufficient for detecting "rug/fabric" or if a custom `.tflite` model needs to be trained via Model Maker.

Phases with standard patterns (skip research-phase):
- **Phase 1, Phase 2, Phase 3:** Well-documented Android patterns (Room, AlarmManager, AccessibilityService).

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified with official Android/Google documentation. |
| Features | HIGH | Based on competitive analysis and established MVP digital wellness features. |
| Architecture | HIGH | Standard Android clean architecture applied to overlay apps. |
| Pitfalls | HIGH | Well-documented issues for AccessibilityServices and app blockers. |

**Overall confidence:** HIGH

### Gaps to Address

- **ML Model Specificity:** Need to validate if the default MediaPipe model is sufficient for "prayer mat" detection. We will start with a basic fabric/rug model or simple object detection and may need to train a custom model if accuracy is too low.

## Sources

### Primary (HIGH confidence)
- Official Android CameraX Documentation — Verified `1.6.1` stability.
- Google MediaPipe Tasks Vision Docs — Verified `0.10.35` for Android image classification.
- Android Accessibility Constraints (Android 14/15) — Verified constraints and best practices for overlays.
- Aladhan Prayer Times API — Verified data availability.

### Secondary (MEDIUM confidence)
- `dontkillmyapp.com` and community discussions — OEM battery management restrictions.
- Reviews of digital wellness apps (AppBlock, SalatGuard) — Feature expectations and common user complaints.

---
*Research completed: 2026-06-14*
*Ready for roadmap: yes*
