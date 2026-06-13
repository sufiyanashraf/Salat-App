# Stack Research

**Domain:** Android App (Native, Accessibility, On-device ML)
**Researched:** 2026-06-14
**Confidence:** HIGH

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

```gradle
// app/build.gradle.kts
dependencies {
    // Core Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // CameraX
    val camerax_version = "1.6.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // MediaPipe for On-device ML
    implementation("com.google.mediapipe:tasks-vision:0.10.35")

    // Ktor Client
    val ktor_version = "3.5.0"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-android:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.59.2")
    kapt("com.google.dagger:hilt-compiler:2.59.2")
    
    // Room
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
}
```

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

**If the app is sideloaded (APK install on Android 13+):**
- Use: In-app educational UI to guide users to Android Settings → App Info → 3-dot menu → **Allow restricted settings**.
- Because: Android blocks Accessibility Service activation for sideloaded apps by default to prevent malware. The user must manually lift this restriction before enabling the service.

**If the device reboots or updates:**
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

---
*Stack research for: Android Native / Prayer Lock App*
*Researched: 2026-06-14*
