---
phase: 4
slug: ml-verification
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-16
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None (Android Manual/ADB) |
| **Config file** | none |
| **Quick run command** | `./gradlew assembleDebug` |
| **Full suite command** | `./gradlew build` |
| **Estimated runtime** | ~10 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew assembleDebug`
- **After every plan wave:** Verify the lock screen UI has the camera button.
- **Before `/gsd-verify-work`:** End-to-end verification of camera capturing, inference, and unlocking.
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | VERI-01 | — | N/A | build | `./gradlew assembleDebug` | ❌ W0 | ⬜ pending |
| 4-01-02 | 01 | 1 | VERI-02 | — | N/A | build | `./gradlew assembleDebug` | ❌ W0 | ⬜ pending |
| 4-01-03 | 01 | 1 | VERI-03 | — | N/A | build | `./gradlew assembleDebug` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/app/src/main/java/com/example/salattracker/ml/ImageClassifierHelper.kt`
- [ ] `app/app/src/main/assets/efficientnet_lite0.tflite` (Model Asset)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Camera Overlay | VERI-01 | View attached to overlay | Open LockScreen, tap "Verify with Camera", ensure permission prompt & live preview works |
| Image Verification | VERI-02 | Visual / ML inference | Point camera at a rug, take photo, ensure LockScreen dismisses |
| Failure State | VERI-02 | Visual / ML inference | Point camera at a face/laptop, take photo, ensure toast appears and retry is possible |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
