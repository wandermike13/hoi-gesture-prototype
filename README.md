# Hoi! — Gesture Recognition Prototype

**App Working Title:** Hoi! (Look, Don't Look!)
**Prototype Phase:** 1 — Camera + Gesture Recognition
**Date:** June 2, 2026

---

## Goal of This Prototype

Validate that the front-facing camera can reliably detect:
1. **Hand gestures** — Rock, Paper, Scissors (for RPS phase)
2. **Eye/head direction** — Up, Down, Left, Right (for pointing phase)

No scoring, no UI polish, no multiplayer. Just the camera talking to the app.

---

## Structure

```
hoi_prototype/
├── ios/                    # Swift/SwiftUI prototype (Xcode project)
│   └── HoiApp/
│       └── Sources/
│           └── GestureRecognition/
│               ├── HandGestureDetector.swift     # RPS hand recognition via Vision
│               ├── EyeTrackingDetector.swift     # Eye/head direction via ARKit
│               └── CameraSessionManager.swift    # Manages AVCaptureSession
├── android/                # Kotlin prototype (Android Studio project)
│   └── app/src/main/java/com/hoi/gesture/
│       ├── HandGestureDetector.kt                # RPS hand recognition via ML Kit
│       ├── EyeTrackingDetector.kt                # Eye/head direction via ML Kit
│       └── CameraManager.kt                      # Manages CameraX session
├── shared/
│   └── docs/
│       └── gesture_thresholds.md                 # Calibration notes & tuning guide
└── README.md
```

---

## Phase 1 Checklist

- [ ] iOS: Camera session opens on front camera
- [ ] iOS: Hand landmarks detected in real time (Vision framework)
- [ ] iOS: Rock / Paper / Scissors classified from hand pose
- [ ] iOS: ARKit detects eye/head direction (Up/Down/Left/Right)
- [ ] iOS: Calibration baseline captured at session start
- [ ] Android: CameraX opens front camera
- [ ] Android: ML Kit detects hand landmarks
- [ ] Android: Rock / Paper / Scissors classified
- [ ] Android: ML Kit Face Mesh detects eye/head direction
- [ ] Android: Calibration baseline captured

---

## Speed Settings (for later integration)

| Setting | Pointer Lock Delay | Detection Window |
|---|---|---|
| Low | 1500ms | 800ms |
| Medium | 1000ms | 600ms |
| High | 600ms | 400ms |

---

## Notes

- All processing is **on-device** — no camera data sent to server
- Calibration runs once per session (user looks L/R/U/D on prompt)
- Streak tracking replaces fixed win count — longest unbroken streak is the score
