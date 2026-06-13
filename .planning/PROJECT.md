# Salat Tracker

## What This Is

An Android app designed to help Gen Z users build consistency in their Namaz (Islamic prayer) habits. It acts as an enforcer by locking non-essential apps during prayer times using an Accessibility Service overlay. The lock is only removed when the user takes a picture of a prayer mat, which is verified using a basic, lightweight on-device image classification model (looking for rug/fabric patterns).

## Core Value

Enforce prayer consistency by making the user's phone unusable for non-essential tasks until they take an honest photo of a prayer mat.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Fetch accurate daily prayer times via a reliable API based on user location.
- [ ] Overlay a full-screen block over all non-essential apps when a prayer time arrives.
- [ ] Provide an exception list (whitelist) for essential apps like Phone/Dialer and Messages.
- [ ] Integrate a camera feature to capture a picture.
- [ ] Use a basic, lightweight ML model to verify the image contains a mat/rug/fabric pattern (rejecting obvious non-mats like faces or appliances).
- [ ] Allow the user to confirm they have prayed to lift the app block until the next prayer time.

### Out of Scope

- [Strict AI Verification] — Too complex, requires a heavy model or backend. We rely on lightweight on-device models for "honest but obvious" checks.
- [Full Device Administrator Lock] — Too restrictive and hard to build/install; using an overlay via Accessibility Service provides a better user experience while still effectively blocking apps.
- [Complete App Block] — Blocking essential apps like phone calls is dangerous in emergencies, so a whitelist is necessary.

## Context

- **Platform:** Android
- **Target Audience:** Gen Z users struggling with Namaz consistency who want a strict, self-imposed lock.
- **Permissions:** Needs Accessibility Service permission, which users must manually enable. Requires camera permissions and location/internet for prayer times.
- **Technical approach:** The app needs to run a background service to monitor time and foreground apps. The AI verification should run locally on the device (e.g., using ML Kit or TFLite) to avoid backend costs and ensure privacy.

## Constraints

- **Platform**: Android Only — iOS does not allow overlaying other apps or strictly blocking them in this manner without MDM.
- **Permissions**: Accessibility Service — Requires user to explicitly grant this sensitive permission in Android Settings.
- **Privacy**: On-device ML — Image classification must happen locally without uploading photos to a server.
- **Cost**: Free/Low Cost — Use free APIs (e.g., Aladhan API) and local ML models to keep running costs at zero.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Use Accessibility Service for locking | Provides a full-screen overlay to block apps without needing Device Admin privileges | — Pending |
| Basic ML model for verification | Keeps the app lightweight and respects privacy by running on-device, relying slightly on user honesty | — Pending |
| Whitelist essential apps | Ensures phone remains usable for emergencies (calls/texts) | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-14 after initialization*
