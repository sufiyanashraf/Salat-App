# Roadmap: Salat Tracker

## Overview

Salat Tracker will be built in five distinct phases. It starts by establishing reliable offline data for prayer times, followed by implementing the core AccessibilityService lock mechanism. Once locking works manually, scheduling automation is introduced to trigger locks. The core differentiator—on-device ML camera verification—is then integrated. Finally, the app is hardened against OS battery optimizations to ensure the lock triggers reliably.

## Phases

- [x] **Phase 1: Foundation & Data** - Establish local storage and API integration for prayer times. (completed 2026-06-14)
- [ ] **Phase 2: Core Locking Engine** - Build the robust AccessibilityService lock overlay and whitelisting.
- [ ] **Phase 3: Scheduling Automation** - Automatically trigger locks at specific daily prayer times.
- [ ] **Phase 4: ML & Verification** - Implement CameraX and on-device MediaPipe ML for mat verification.
- [ ] **Phase 5: Polish & Hardening** - Ensure service persistence and guide users to disable battery optimizations.

## Phase Details

### Phase 1: Foundation & Data
**Goal**: Fetch and persist accurate prayer times based on user location.
**Depends on**: Nothing
**Requirements**: [PRAY-01]
**Success Criteria**:
  1. App can fetch accurate prayer times from the API for the user's location.
  2. Prayer times are persisted locally to ensure offline availability.
**Plans**: TBD

Plans:
- [x] 01-01: Initialize DataStore, Room DB, and basic API client.

### Phase 2: Core Locking Engine
**Goal**: Build the robust blocking mechanism using AccessibilityService.
**Depends on**: Phase 1
**Requirements**: [LOCK-01, LOCK-02, LOCK-03, VERI-04]
**Success Criteria**:
  1. Non-essential apps are blocked by a full-screen overlay when a lock is triggered.
  2. Hardcoded essential apps (e.g., Phone, Messages) remain accessible during the lock.
  3. Overlay cannot be bypassed using system navigation buttons (back/home).
  4. Users can manually dismiss the lock via a fallback button.
**Plans**: TBD

Plans:
- [ ] 02-01: Implement AppLockAccessibilityService and LockManager.
- [ ] 02-02: Create OverlayActivity UI with manual fallback unlock.

### Phase 3: Scheduling Automation
**Goal**: Automatically trigger locks at specific scheduled prayer times.
**Depends on**: Phase 2
**Requirements**: [PRAY-02]
**Success Criteria**:
  1. System reliably schedules upcoming daily prayer times as background events.
  2. The core lock screen activates automatically when a scheduled prayer time is reached.
**Plans**: TBD

Plans:
- [ ] 03-01: Implement AlarmManager scheduling and PrayerTimeWorker.

### Phase 4: ML & Verification
**Goal**: Enable users to unlock the device by taking a picture of a prayer mat.
**Depends on**: Phase 2
**Requirements**: [VERI-01, VERI-02, VERI-03]
**Success Criteria**:
  1. User can access a camera view directly from the lock screen overlay.
  2. On-device ML correctly detects mat/fabric patterns from the camera feed.
  3. The phone unlocks and the overlay is dismissed upon successful image verification.
**Plans**: TBD

Plans:
- [x] 04-01: Integrate CameraX and MediaPipe for on-device fabric detection.

### Phase 5: Polish & Hardening
**Goal**: Ensure the app survives OEM battery managers and device reboots.
**Depends on**: Phase 3
**Requirements**: []
**Success Criteria**:
  1. App lock service restarts automatically after a device reboot.
  2. Users are successfully guided through an onboarding flow to disable battery optimizations.
  3. Emergency actions and system UI are safely whitelisted to prevent critical lockouts.
**Plans**: TBD

Plans:
- [ ] 05-01: Add boot completion receivers and battery optimization onboarding UI.
- [ ] 05-02: Finalize dynamic whitelisting for critical intents.

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Data | 1/1 | Complete    | 2026-06-14 |
| 2. Core Locking Engine | 0/2 | Not started | - |
| 3. Scheduling Automation | 0/1 | Not started | - |
| 4. ML & Verification | 0/1 | Not started | - |
| 5. Polish & Hardening | 0/2 | Not started | - |
