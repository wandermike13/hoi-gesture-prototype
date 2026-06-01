import Vision
import CoreImage

/// Detects Rock, Paper, or Scissors hand gestures using Apple's Vision framework.
/// Hand pose detection available on iOS 14+.

enum RPSGesture {
    case rock       // All fingers curled
    case paper      // All fingers extended
    case scissors   // Index + middle extended, others curled
    case unknown
}

class HandGestureDetector {

    // MARK: - Vision Request
    private lazy var handPoseRequest: VNDetectHumanHandPoseRequest = {
        let request = VNDetectHumanHandPoseRequest()
        request.maximumHandCount = 1
        return request
    }()

    // MARK: - Detection

    /// Call this with each camera frame buffer to get a gesture classification.
    func detect(in sampleBuffer: CMSampleBuffer, completion: @escaping (RPSGesture) -> Void) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            completion(.unknown)
            return
        }

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer,
                                            orientation: .leftMirrored,
                                            options: [:])
        do {
            try handler.perform([handPoseRequest])
            guard let observation = handPoseRequest.results?.first else {
                completion(.unknown)
                return
            }
            let gesture = classify(observation: observation)
            completion(gesture)
        } catch {
            print("[HandGestureDetector] Vision error: \(error)")
            completion(.unknown)
        }
    }

    // MARK: - Classification Logic

    private func classify(observation: VNHumanHandPoseObservation) -> RPSGesture {
        guard let indexTip   = try? observation.recognizedPoint(.indexTip),
              let middleTip  = try? observation.recognizedPoint(.middleTip),
              let ringTip    = try? observation.recognizedPoint(.ringTip),
              let littleTip  = try? observation.recognizedPoint(.littleTip),
              let thumbTip   = try? observation.recognizedPoint(.thumbTip),
              let indexMCP   = try? observation.recognizedPoint(.indexMCP),
              let middleMCP  = try? observation.recognizedPoint(.middleMCP),
              let ringMCP    = try? observation.recognizedPoint(.ringMCP),
              let littleMCP  = try? observation.recognizedPoint(.littleMCP)
        else { return .unknown }

        // Only process high-confidence detections
        let minConfidence: Float = 0.7
        guard indexTip.confidence > minConfidence,
              middleTip.confidence > minConfidence else { return .unknown }

        // A finger is "extended" if its tip is significantly above its MCP joint
        // (In Vision coordinates, y increases downward on screen but is normalized 0-1)
        let threshold: CGFloat = 0.05

        let indexExtended  = indexTip.y > indexMCP.y + threshold
        let middleExtended = middleTip.y > middleMCP.y + threshold
        let ringExtended   = ringTip.y  > ringMCP.y  + threshold
        let littleExtended = littleTip.y > littleMCP.y + threshold

        switch (indexExtended, middleExtended, ringExtended, littleExtended) {
        case (false, false, false, false):
            return .rock       // All curled
        case (true, true, true, true):
            return .paper      // All extended
        case (true, true, false, false):
            return .scissors   // Index + middle only
        default:
            return .unknown
        }
    }
}

// MARK: - RPS Game Logic Helper
extension RPSGesture {
    /// Returns true if self beats other
    func beats(_ other: RPSGesture) -> Bool {
        switch (self, other) {
        case (.rock, .scissors), (.scissors, .paper), (.paper, .rock):
            return true
        default:
            return false
        }
    }

    var displayName: String {
        switch self {
        case .rock:     return "✊ Rock"
        case .paper:    return "✋ Paper"
        case .scissors: return "✌️ Scissors"
        case .unknown:  return "❓ Unknown"
        }
    }
}
