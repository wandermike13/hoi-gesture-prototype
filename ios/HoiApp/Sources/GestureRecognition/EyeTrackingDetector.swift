import ARKit
import SceneKit

/// Detects eye/head direction using ARKit Face Tracking.
/// Requires TrueDepth front camera (iPhone X and later).
/// Used in the pointing phase of Hoi! to determine which direction the player looks.

enum GazeDirection {
    case up, down, left, right, center, unknown
}

protocol EyeTrackingDelegate: AnyObject {
    func eyeTrackingDidDetectDirection(_ direction: GazeDirection)
    func eyeTrackingCalibrationComplete(baseline: simd_float3)
}

class EyeTrackingDetector: NSObject {

    // MARK: - Properties
    weak var delegate: EyeTrackingDelegate?

    private var arSession: ARSession?
    private var calibrationBaseline: simd_float3?
    private var isCalibrating = false
    private var calibrationSamples: [simd_float3] = []

    // Threshold for direction detection (tuned post-calibration)
    // Adjustable per user during calibration
    var detectionThreshold: Float = 0.12

    // Consecutive frame confirmation (avoids false positives)
    private var consecutiveFrames = 0
    private let requiredFrames = 3
    private var lastDirection: GazeDirection = .center

    // MARK: - ARKit Session

    func start() {
        guard ARFaceTrackingConfiguration.isSupported else {
            print("[EyeTrackingDetector] ARKit face tracking not supported on this device")
            return
        }

        let config = ARFaceTrackingConfiguration()
        config.isLightEstimationEnabled = false

        arSession = ARSession()
        arSession?.delegate = self
        arSession?.run(config, options: [.resetTracking, .removeExistingAnchors])
        print("[EyeTrackingDetector] ARKit session started")
    }

    func stop() {
        arSession?.pause()
        arSession = nil
    }

    // MARK: - Calibration

    /// Call this to begin calibration. Ask user to look straight ahead.
    /// Collects 30 frames (~0.5s at 60fps) to build a baseline.
    func beginCalibration() {
        isCalibrating = true
        calibrationSamples = []
        print("[EyeTrackingDetector] Calibration started — ask user to look straight ahead")
    }

    private func finishCalibration() {
        guard !calibrationSamples.isEmpty else { return }
        // Average all samples to get baseline center gaze vector
        let sum = calibrationSamples.reduce(simd_float3(0, 0, 0), +)
        calibrationBaseline = sum / Float(calibrationSamples.count)
        isCalibrating = false
        calibrationSamples = []
        print("[EyeTrackingDetector] Calibration complete. Baseline: \(calibrationBaseline!)")
        delegate?.eyeTrackingCalibrationComplete(baseline: calibrationBaseline!)
    }

    // MARK: - Direction Classification

    private func classifyGaze(_ lookAtPoint: simd_float3) -> GazeDirection {
        guard let baseline = calibrationBaseline else { return .unknown }

        let delta = lookAtPoint - baseline

        let absX = abs(delta.x)
        let absY = abs(delta.y)

        // Must exceed threshold to register as a direction
        guard absX > detectionThreshold || absY > detectionThreshold else {
            return .center
        }

        // Dominant axis wins
        if absY > absX {
            return delta.y > 0 ? .up : .down
        } else {
            return delta.x > 0 ? .right : .left
        }
    }
}

// MARK: - ARSessionDelegate
extension EyeTrackingDetector: ARSessionDelegate {

    func session(_ session: ARSession, didUpdate anchors: [ARAnchor]) {
        guard let faceAnchor = anchors.first(where: { $0 is ARFaceAnchor }) as? ARFaceAnchor else {
            return
        }

        let lookAt = faceAnchor.lookAtPoint

        if isCalibrating {
            calibrationSamples.append(lookAt)
            if calibrationSamples.count >= 30 {
                finishCalibration()
            }
            return
        }

        // Live detection
        let direction = classifyGaze(lookAt)

        if direction == lastDirection {
            consecutiveFrames += 1
        } else {
            consecutiveFrames = 1
            lastDirection = direction
        }

        // Only fire event after N consecutive matching frames
        if consecutiveFrames == requiredFrames, direction != .center, direction != .unknown {
            DispatchQueue.main.async {
                self.delegate?.eyeTrackingDidDetectDirection(direction)
            }
            consecutiveFrames = 0
        }
    }

    func session(_ session: ARSession, didFailWithError error: Error) {
        print("[EyeTrackingDetector] ARKit session error: \(error)")
    }
}

// MARK: - GazeDirection Helper
extension GazeDirection {
    var displayName: String {
        switch self {
        case .up:      return "⬆️ Up"
        case .down:    return "⬇️ Down"
        case .left:    return "⬅️ Left"
        case .right:   return "➡️ Right"
        case .center:  return "🎯 Center"
        case .unknown: return "❓ Unknown"
        }
    }
}
