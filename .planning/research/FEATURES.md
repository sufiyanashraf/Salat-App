# Feature Research

**Domain:** Android Digital Wellness / Habit Enforcement
**Researched:** 2026-06-14
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Accurate Prayer Times | App must know when to lock. Needs reliable fetching based on location | MEDIUM | Use free Aladhan API or similar. Needs location permissions. |
| Accessibility Service Lock | Needs to reliably block distractions with a full-screen overlay | HIGH | Android Accessibility Service implementation requires careful system-level handling. |
| Whitelist / Emergency Access | Users must be able to use phone for emergencies (calls/messages) | MEDIUM | Standard Android package allowlist overlay implementation. |
| Manual Unlock Fallback | Fallback mechanism if the camera/ML system fails or they are travelling | MEDIUM | Time-delay or emergency bypass to prevent permanent lockouts. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Physical Action Verification (Camera) | Forces the user to physically go to their prayer mat, solving procrastination | HIGH | Needs camera intent/integration directly within the locked context. |
| On-Device ML Verification | Ensures privacy (no cloud uploads), zero backend cost, and immediate validation | HIGH | TFLite/ML Kit model looking for fabric/rug textures (honest but obvious check). |
| Zero Data / No Accounts | Purely local utility builds immense trust, dodging the "bloated tracking app" stereotype | LOW | Architectural constraint. No servers or DBs, purely local state. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Full Device Admin Lock | Total commitment to no distractions | Hard to uninstall, risky if bugs occur, extreme UX | Accessibility Service Overlay |
| Cloud-based AI Vision | Better accuracy in detecting specific Islamic prayer mats | Privacy violation, high backend costs, needs internet | Basic on-device ML checking for fabric/rug patterns |
| "Kitchen Sink" (Qibla, Quran) | Users want one app for everything Muslim-related | Causes bloat, distraction, loses focus on core value | Laser-focus on the App Locker / Habit Tracker functionality |
| Strict ML Verification | Perfect detection to prevent any cheating | Too complex, heavy model needed, frustrates users | "Honest but obvious" check (e.g. reject face, accept rug) |

## Feature Dependencies

```text
[Accessibility Service Lock]
    └──requires──> [Accurate Prayer Times]

[Whitelist / Emergency Access]
    └──requires──> [Accessibility Service Lock]

[Camera Capture Flow]
    └──requires──> [Accessibility Service Lock]

[On-Device ML Verification]
    └──requires──> [Camera Capture Flow]

[Unlock State Management]
    └──requires──> [On-Device ML Verification]
```

### Dependency Notes

- **[Accessibility Service Lock] requires [Accurate Prayer Times]:** The lock only knows when to trigger based on accurate local time tracking.
- **[Whitelist / Emergency Access] requires [Accessibility Service Lock]:** The exception list modifies the core blocking mechanism.
- **[Camera Capture Flow] requires [Accessibility Service Lock]:** The camera interface must be exposed securely *over* the lock screen without letting the user escape to other apps.
- **[On-Device ML Verification] requires [Camera Capture Flow]:** The system needs an image buffer captured from the user to evaluate.
- **[Unlock State Management] requires [On-Device ML Verification]:** The lock is only lifted upon successful verification from the ML model.

## MVP Definition

### Launch With (v1)

Minimum viable product — what's needed to validate the concept.

- [x] Accurate Prayer Times API integration — Trigger for the lock mechanism
- [x] Accessibility Service Overlay — Core mechanism to block non-essential apps
- [x] Hardcoded Whitelist (Phone, Messages) — Essential safety feature so the device remains a phone
- [x] Camera Capture Flow — UI to take the verification photo
- [x] Basic On-Device ML Verification — Core differentiator to verify fabric/rug
- [x] Local State Management — Handles tracking "prayed" vs "not prayed" for the current window

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] Customizable Whitelist — Allow users to add their own safe apps (e.g., specific Quran app)
- [ ] Grace Period / Warning — e.g., 5-minute notification before the lock fully engages
- [ ] Streak Tracker — Positive reinforcement statistics for habit building

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] Advanced Custom Schedules — For users who pray at specific times within the overall window
- [ ] Hardcore Mode — Completely disabling the manual emergency bypass
- [ ] Fine-Tuned Model Training — More accurate detection of specific prayer mat types if basic fabric detection is easily spoofed

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Accessibility Lock Overlay | HIGH | HIGH | P1 |
| Prayer Times Fetching | HIGH | LOW | P1 |
| Hardcoded Whitelist | HIGH | LOW | P1 |
| Camera & ML Verification | HIGH | HIGH | P1 |
| Manual Unlock Fallback | HIGH | LOW | P1 |
| Customizable Whitelist | MEDIUM | MEDIUM | P2 |
| Grace Period Warning | MEDIUM | LOW | P2 |
| Streak Tracking | MEDIUM | LOW | P2 |
| Advanced AI Model | LOW | HIGH | P3 |

## Competitor Feature Analysis

| Feature | Competitor A (SalatGuard / FivePrayer) | Competitor B (Standard App Lockers) | Our Approach |
|---------|----------------------------------------|-------------------------------------|--------------|
| **Blocking Mechanism** | Accessibility Service | Accessibility Service / Usage Stats | Accessibility Service |
| **Unlock Method** | Time-based delay, emergency button | Password, Time, or Location | **Physical Photo Verification (ML)** |
| **Privacy / Business Model** | Free/Ads, Local | Freemium/Subscriptions | Local-only ML, No Accounts, Free |
| **App Scope** | Often adds Qibla, Adhan, Quran features | General purpose app blocking | Laser-focused solely on prayer habit enforcement |

## Sources

- Initial project requirements (`PROJECT.md`)
- Competitive landscape analysis of Android prayer focus apps (SalatGuard, FivePrayer)
- Reviews of typical digital wellness and app-blocking tools (AppBlock)
- Industry trends showing user fatigue with bloated, data-harvesting "all-in-one" lifestyle apps

---
*Feature research for: Android Prayer Lock App*
*Researched: 2026-06-14*
