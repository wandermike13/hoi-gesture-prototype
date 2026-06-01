# Gesture Thresholds & Calibration Guide

## Overview

Both the iOS and Android implementations use threshold values to determine when a gesture or gaze direction "counts". These are the starting defaults and how to tune them.

---

## Hand Gesture Detection (RPS)

### iOS (Vision framework)
- **Threshold:** `0.05` (normalized coordinate offset between fingertip and MCP joint)
- If too many false positives (wrong gesture detected): increase to `0.08`
- If gestures aren't registering: decrease to `0.03`

### Android (ML Kit Hands)
- **Threshold:** `20px` (pixel distance between fingertip and MCP joint)
- Scales with image resolution — may need adjustment for different device cameras
- If false positives: increase to `30px`
- If misses: decrease to `15px`

---

## Eye/Head Tracking (Pointing Phase)

### iOS (ARKit)
- **Default threshold:** `0.12` (offset from calibrated baseline in normalized look vector units)
- **Easier (larger range):** `0.08` — responds to subtler eye movements
- **Harder (tighter range):** `0.18` — requires more deliberate movement

### Android (ML Kit Face)
- **Default threshold:** `20px` (pixel offset from calibrated baseline)
- **Easier:** `12px`
- **Harder:** `30px`

---

## Consecutive Frame Confirmation

Both platforms require **3 consecutive matching frames** before a direction is confirmed. This prevents flickering or accidental triggers.

- At 60fps: 3 frames = ~50ms — fast enough to feel responsive
- Increase to 5 frames if players report accidental triggers
- Decrease to 2 frames if response feels too slow

---

## Speed Settings (Pointing Phase Detection Window)

| Speed | Pointer Lock Delay | Detection Window |
|---|---|---|
| Low    | 1500ms | 800ms |
| Medium | 1000ms | 600ms |
| High   |  600ms | 400ms |

---

## Calibration Protocol (Per Session)

1. App displays: "Look straight ahead"
2. Collects 30 frames (~0.5s) — averages to baseline
3. App displays: "Look LEFT" → validates left threshold
4. App displays: "Look RIGHT" → validates right threshold
5. App displays: "Look UP" → validates up threshold
6. App displays: "Look DOWN" → validates down threshold
7. Calibration complete — thresholds auto-adjusted to user's range

Optional: Full 4-direction calibration can set per-direction thresholds independently for users with asymmetric eye movement.

---

## Notes

- Re-calibrate if user changes seating position or lighting changes significantly
- On Android, threshold is pixel-based so it may need to scale by device screen density (`dp` instead of `px` in future version)
- Blink detection (iOS only): if `eyeBlinkLeft` or `eyeBlinkRight` blendshape > 0.8, pause detection for 100ms (one extension max per round)
