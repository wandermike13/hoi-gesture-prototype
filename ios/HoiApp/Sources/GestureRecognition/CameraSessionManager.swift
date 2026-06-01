import AVFoundation
import UIKit

/// Manages the AVCaptureSession for the front-facing camera.
/// Feeds frames to both HandGestureDetector and EyeTrackingDetector.
class CameraSessionManager: NSObject {

    // MARK: - Properties
    private let captureSession = AVCaptureSession()
    private let videoOutput = AVCaptureVideoDataOutput()
    private let sessionQueue = DispatchQueue(label: "com.hoi.cameraQueue")

    var onFrameCaptured: ((CMSampleBuffer) -> Void)?

    // MARK: - Setup

    func requestPermissionAndStart(completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            if granted {
                self.sessionQueue.async {
                    self.configureSession()
                    self.captureSession.startRunning()
                }
            }
            DispatchQueue.main.async { completion(granted) }
        }
    }

    private func configureSession() {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = .high

        // Front camera
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera,
                                                    for: .video,
                                                    position: .front),
              let input = try? AVCaptureDeviceInput(device: device),
              captureSession.canAddInput(input) else {
            print("[CameraSessionManager] Failed to access front camera")
            captureSession.commitConfiguration()
            return
        }
        captureSession.addInput(input)

        // Video output
        videoOutput.setSampleBufferDelegate(self, queue: sessionQueue)
        videoOutput.alwaysDiscardsLateVideoFrames = true
        if captureSession.canAddOutput(videoOutput) {
            captureSession.addOutput(videoOutput)
        }

        captureSession.commitConfiguration()
    }

    func stop() {
        sessionQueue.async { self.captureSession.stopRunning() }
    }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate
extension CameraSessionManager: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput,
                       didOutput sampleBuffer: CMSampleBuffer,
                       from connection: AVCaptureConnection) {
        onFrameCaptured?(sampleBuffer)
    }
}
