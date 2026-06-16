# Phase 4: ML & Verification - Context

**Gathered:** 2026-06-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Enable users to unlock the device by taking a picture of a prayer mat. This involves integrating CameraX into the existing lock screen overlay and using on-device ML (MediaPipe Tasks Vision) to verify the presence of a mat/fabric before unlocking.

</domain>

<decisions>
## Implementation Decisions

### Camera UI/UX
- **D-01:** The camera should be hidden behind a "Verify with Camera" button. The lock screen will show text/timer by default, and tapping the button launches the camera view in a modal/overlay.

### Verification Strictness
- **D-02:** The verification should be lenient (e.g. ~50% confidence). It should accept anything that vaguely looks like a mat/fabric/rug, while rejecting completely unrelated objects like faces or electronics. This avoids user frustration, especially in low light.

### Model Selection
- **D-03:** We will use a pre-trained generic model (like MobileNet) that already includes labels for "rugs" and "fabrics". We will not build a custom model in order to keep the implementation lightweight and fast.

### Failure State Handling
- **D-04:** Provide visual feedback with an instant retry loop. If verification fails, show a brief "Mat not detected" toast/message, keep the camera open, and allow the user to immediately try again.

### the agent's Discretion
- Exact layout and styling of the camera overlay.
- Resolution mapping and optimization for MediaPipe inference.
- Exact label mappings to filter (e.g. "rug", "prayer rug", "fabric", "carpet").

</decisions>

<specifics>
## Specific Ideas

- Ensure the verification allows some tolerance since lighting might be poor during early morning (Fajr) or night (Isha) prayers.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Tech Stack Constraints
- `.planning/research/STACK.md` — Specifies using MediaPipe Tasks Vision (0.10.35) and CameraX (1.6.1), and forbids cloud APIs.

### Requirements
- `.planning/REQUIREMENTS.md` — Phase 4 requirements VERI-01 to VERI-03.

</canonical_refs>

<deferred>
## Deferred Ideas

- None — discussion stayed within phase scope.

</deferred>

---

*Phase: 04-ml-verification*
*Context gathered: 2026-06-16*
