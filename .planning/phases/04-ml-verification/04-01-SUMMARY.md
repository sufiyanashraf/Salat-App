# Phase 04 Plan 01 — Summary

## What was done

This plan integrated on-device ML image classification and CameraX camera capture into the existing lock screen overlay, enabling users to unlock their phone by photographing a prayer mat.

### Tasks Completed

| Task | Description | Commit |
|------|-------------|--------|
| 4-01-01 | Added CameraX 1.6.1 and MediaPipe 0.10.35 dependencies to version catalog and build file; added CAMERA permission and hardware feature to manifest | `5363c7f` |
| 4-01-02 | Downloaded EfficientNet-Lite0 model to assets; created `ImageClassifierHelper` with lenient 50% confidence threshold for mat-like labels (rug, doormat, fabric, velvet, window shade) | `ed398e1` |
| 4-01-03 | Added runtime camera permission request to `MainScreen` using `rememberLauncherForActivityResult` and `LaunchedEffect` | `d8956e7` |
| 4-01-04 | Rewrote `LockScreenOverlay` to include "Verify with Camera" button, CameraX preview via `AndroidView`, image capture with `captureAndVerify`, permission-denied fallback UI, and proper `imageProxy.close()` in finally block | `912b023` |
| 4-01-05 | Added `FLAG_SHOW_WHEN_LOCKED` to overlay params and wired `onVerifySuccess = { LockManager.setLocked(false) }` in `AppLockAccessibilityService` | `dba7b1f` |

### Files Modified

- `app/gradle/libs.versions.toml` — Added `camerax` and `mediapipe` versions + 5 library entries
- `app/app/build.gradle.kts` — Added CameraX and MediaPipe implementation dependencies
- `app/app/src/main/AndroidManifest.xml` — Added `CAMERA` permission and `camera` hardware feature
- `app/app/src/main/assets/efficientnet_lite0.tflite` — Downloaded pre-trained EfficientNet-Lite0 model
- `app/app/src/main/java/com/example/salattracker/ml/ImageClassifierHelper.kt` — New file: MediaPipe-based image classifier
- `app/app/src/main/java/com/example/salattracker/ui/main/MainScreen.kt` — Added camera permission request on launch
- `app/app/src/main/java/com/example/salattracker/ui/lock/LockScreenOverlay.kt` — Added camera verification UI with CameraX preview and capture
- `app/app/src/main/java/com/example/salattracker/service/AppLockAccessibilityService.kt` — Added `FLAG_SHOW_WHEN_LOCKED` and `onVerifySuccess` callback

### Verification

- [x] All 5 tasks executed
- [x] Each task committed individually (5 atomic commits)
- [ ] `./gradlew assembleDebug` — Could not run: JDK not available in the current shell environment (JAVA_HOME not set, no java.exe found on system PATH). Build verification should be done when JDK is available.

### Notes

- The `ImageClassifierHelper` uses MediaPipe's `ImageClassifier` with a pre-trained EfficientNet-Lite0 model, matching labels like "rug", "doormat", "fabric", "velvet", and "window shade" at >50% confidence per user decision D-02 (lenient verification).
- The camera UI includes a fallback screen when camera permission is denied to prevent `SecurityException` crashes in the accessibility service.
- `imageProxy.close()` is properly called in a `finally` block to prevent CameraX from freezing.
