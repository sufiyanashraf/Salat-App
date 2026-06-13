# Requirements: Salat Tracker

**Defined:** 2026-06-14
**Core Value:** Enforce prayer consistency by making the user's phone unusable for non-essential tasks until they take an honest photo of a prayer mat.

## v1 Requirements

### Prayer Times

- [ ] **PRAY-01**: App fetches accurate daily prayer times via a reliable API based on user location.
- [ ] **PRAY-02**: App schedules alarms/events for each prayer time.

### App Locking

- [ ] **LOCK-01**: App overlays a full-screen block over all non-essential apps when a prayer time arrives.
- [ ] **LOCK-02**: App provides a hardcoded whitelist (e.g., Phone/Dialer, Messages) that can be accessed during the lock.
- [ ] **LOCK-03**: App prevents dismissal of the overlay using the back button or home button.

### Verification

- [ ] **VERI-01**: User can open a camera view from the lock screen overlay to take a picture of a prayer mat.
- [ ] **VERI-02**: App runs a lightweight on-device ML model (e.g., MediaPipe) to verify the image contains a mat/fabric pattern.
- [ ] **VERI-03**: App unlocks the phone and dismisses the overlay until the next prayer time upon successful verification.
- [ ] **VERI-04**: App provides a manual unlock fallback for instances where the user doesn't have their mat (e.g., traveling).

## v2 Requirements

### Analytics

- **STAT-01**: User can view their prayer consistency streak.
- **STAT-02**: User can view a history of their unlocked prayer times.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Full Device Admin Lock | Too restrictive, high risk of locking users out of emergency functions. Overlay is sufficient. |
| Cloud-based AI Vision | Violates privacy and introduces recurring backend costs. On-device ML is preferred. |
| "Kitchen Sink" Features | Qibla compasses, Quran readers, etc., distract from the core value of app locking. |
| Strict ML Verification | Aim for an "honest but obvious" check. Perfect verification leads to UX frustration, especially in low light. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PRAY-01 | - | Pending |
| PRAY-02 | - | Pending |
| LOCK-01 | - | Pending |
| LOCK-02 | - | Pending |
| LOCK-03 | - | Pending |
| VERI-01 | - | Pending |
| VERI-02 | - | Pending |
| VERI-03 | - | Pending |
| VERI-04 | - | Pending |

**Coverage:**
- v1 requirements: 9 total
- Mapped to phases: 0
- Unmapped: 9 ⚠️

---
*Requirements defined: 2026-06-14*
*Last updated: 2026-06-14 after initial definition*
