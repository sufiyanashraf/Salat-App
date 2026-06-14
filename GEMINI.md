<!-- GSD:project-start source:PROJECT.md -->
## Project

**Salat Tracker**

An Android app designed to help Gen Z users build consistency in their Namaz (Islamic prayer) habits. It acts as an enforcer by locking non-essential apps during prayer times using an Accessibility Service overlay. The lock is only removed when the user takes a picture of a prayer mat, which is verified using a basic, lightweight on-device image classification model (looking for rug/fabric patterns).

**Core Value:** Enforce prayer consistency by making the user's phone unusable for non-essential tasks until they take an honest photo of a prayer mat.

### Constraints

- **Platform**: Android Only — iOS does not allow overlaying other apps or strictly blocking them in this manner without MDM.
- **Permissions**: Accessibility Service — Requires user to explicitly grant this sensitive permission in Android Settings.
- **Privacy**: On-device ML — Image classification must happen locally without uploading photos to a server.
- **Cost**: Free/Low Cost — Use free APIs (e.g., Aladhan API) and local ML models to keep running costs at zero.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Technologies
| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **Kotlin** | `2.4.0` | Primary Language | Standard for modern Android development. Coroutines and Flow are essential for cleanly managing async tasks like ML inference and API calls. |
| **Jetpack Compose BOM** | `2026.04.01` | UI Toolkit | Declarative UI is much faster to build and maintain than XML. We need complex overlays and custom lock screens that Compose handles gracefully. |
| **Android AccessibilityService** | `Native` | App Monitoring & Blocking | Provides instant `TYPE_WINDOW_STATE_CHANGED` events to detect what app the user just opened. Allows drawing a `TYPE_ACCESSIBILITY_OVERLAY` to block non-essential apps. |
| **MediaPipe Tasks Vision** | `0.10.35` | On-device ML Inference | The modern, Google-recommended successor to raw TFLite. Handles image resizing, rotation, and inference for our "prayer mat" image classifier locally, ensuring privacy. |
| **CameraX** | `1.6.1` | Camera Capture | Hides the immense complexity of the Camera2 API. Provides reliable Preview and ImageCapture use cases needed for verifying the prayer mat. |
### Supporting Libraries
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| **Ktor Client** | `3.5.0` | Networking | Fetching daily prayer times from the Aladhan API. Lightweight and perfectly integrated with Kotlin Coroutines/Serialization. |
| **Dagger Hilt** | `2.59.2` | Dependency Injection | Wiring up repositories, API clients, and the ML pipeline. Essential for keeping the `AccessibilityService` decoupled from domain logic. |
| **Room** | `2.8.4` | Local Storage (Relational) | Caching the monthly prayer time schedule so the app functions completely offline. Also stores the essential app whitelist. |
| **DataStore Preferences** | `1.1.1` | Local Storage (Key-Value) | Storing simple states like `is_currently_locked`, `last_prayer_time`, or `user_location_coords`. |
| **Coil** | `3.x` | Image Loading | Loading and caching any UI assets, placeholders, or user-facing images in Jetpack Compose. |
### Development Tools
| Tool | Purpose | Notes |
|------|---------|-------|
| **Android Studio** | IDE | Use the latest stable version (e.g., Ladybug or newer) that supports Kotlin 2.4.0 and Compose BOM 2026.04.01. |
| **Google ML Kit / Model Maker** | ML Training / Export | For training a custom `.tflite` image classification model on prayer mat datasets if standard models don't have the necessary labels. |
## Installation
## Alternatives Considered
| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| **AccessibilityService** | **UsageStatsManager** | If Accessibility permissions are absolutely rejected by the user. *Note: UsageStats polls every few seconds, allowing the user to briefly use blocked apps before the overlay kicks in. Accessibility is instant.* |
| **MediaPipe Tasks** | **Firebase ML Kit** | If we shift to a cloud-based verification model or want out-of-the-box object detection without bundling custom models. *Note: Breaks offline-only privacy constraints.* |
| **Ktor Client** | **Retrofit** | If the team is significantly more experienced with Retrofit or if we are integrating with legacy Java code. Ktor is better for pure Kotlin multiplatform/coroutines codebases. |
## What NOT to Use
| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **Device Admin / MDM APIs** | Requires a factory reset or complex enterprise provisioning to enforce properly. Overkill for a consumer habit-building app and terrible UX if the user gets locked out entirely. | **AccessibilityService** with a `TYPE_ACCESSIBILITY_OVERLAY` |
| **SYSTEM_ALERT_WINDOW** | Asking for "Draw over other apps" is redundant if you already have Accessibility access. It increases permission fatigue and friction during onboarding. | `AccessibilityService`'s native overlay capabilities |
| **Raw TensorFlow Lite (TFLite)** | Deprecated / hard to use. Requires manual image preprocessing (byte buffers, resizing, rotation handling), which introduces bugs. | **MediaPipe Tasks Vision API** |
| **Cloud Vision APIs** | Requires internet access to unlock the phone (dangerous if the user is out of data/wifi) and costs money per API call. | On-device ML (MediaPipe) |
## Stack Patterns by Variant
- Use: In-app educational UI to guide users to Android Settings → App Info → 3-dot menu → **Allow restricted settings**.
- Because: Android blocks Accessibility Service activation for sideloaded apps by default to prevent malware. The user must manually lift this restriction before enabling the service.
- Use: A `BOOT_COMPLETED` broadcast receiver and persistent foreground service.
- Because: The app needs to immediately resume monitoring for prayer times. Note: AccessibilityServices are sometimes disabled by the OS after an app update, so we must prompt the user to re-enable it on launch.
## Version Compatibility
| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `com.google.dagger:hilt-android:2.59.2` | `Kotlin 2.4.0` | Ensure KAPT (or KSP if using Dagger KSP alpha) is correctly configured for Kotlin 2.4.0. |
| `androidx.room:room-compiler:2.8.4` | `KSP` | Room strongly recommends migrating from KAPT to KSP for better compilation speeds. |
## Sources
- Official Android CameraX Documentation — Verified stable version `1.6.1` and Jetpack Compose Viewfinder updates.
- Google MediaPipe Tasks Vision Docs — Verified `0.10.35` as the current standard for Android image classification, replacing raw TFLite.
- Jetpack Compose Releases — Verified BOM `2026.04.01`.
- Android Accessibility Constraints (Android 14/15) — Verified the "Restricted Settings" constraints for sideloaded accessibility apps.
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.agent/skills/`, `.agents/skills/`, `.cursor/skills/`, or `.github/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
