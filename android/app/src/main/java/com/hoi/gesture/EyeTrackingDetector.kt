package com.hoi.gesture

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

/**
 * Detects eye/head gaze direction using ML Kit Face Detection.
 * Used in the pointing phase of Hoi! to determine player look direction.
 * Requires ML Kit Face Detection dependency:
 *   implementation 'com.google.mlkit:face-detection:16.1.5'
 */

enum class GazeDirection {
    UP, DOWN, LEFT, RIGHT, CENTER, UNKNOWN;

    val displayName: String get() = when (this) {
        UP      -> "⬆️ Up"
        DOWN    -> "⬇️ Down"
        LEFT    -> "⬅️ Left"
        RIGHT   -> "➡️ Right"
        CENTER  -> "🎯 Center"
        UNKNOWN -> "❓ Unknown"
    }
}

interface EyeTrackingListener {
    fun onDirectionDetected(direction: GazeDirection)
    fun onCalibrationComplete()
}

class EyeTrackingDetector {

    var listener: EyeTrackingListener? = null
    var detectionThreshold: Float = 20f   // pixels offset from baseline

    // Calibration state
    private var isCalibrating = false
    private var calibrationSamples = mutableListOf<Pair<Float, Float>>()
    private var baselineX: Float = 0f
    private var baselineY: Float = 0f
    private val requiredCalibrationSamples = 30

    // Consecutive frame confirmation
    private var consecutiveFrames = 0
    private val requiredFrames = 3
    private var lastDirection = GazeDirection.CENTER

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    // MARK: - Calibration

    fun beginCalibration() {
        isCalibrating = true
        calibrationSamples.clear()
        println("[EyeTrackingDetector] Calibration started — ask user to look straight ahead")
    }

    private fun finishCalibration() {
        if (calibrationSamples.isEmpty()) return
        baselineX = calibrationSamples.map { it.first }.average().toFloat()
        baselineY = calibrationSamples.map { it.second }.average().toFloat()
        isCalibrating = false
        calibrationSamples.clear()
        println("[EyeTrackingDetector] Calibration complete. Baseline: ($baselineX, $baselineY)")
        listener?.onCalibrationComplete()
    }

    // MARK: - Detection

    @androidx.camera.core.ExperimentalGetImage
    fun detect(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    processFace(faces[0])
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                println("[EyeTrackingDetector] ML Kit error: ${it.message}")
                imageProxy.close()
            }
    }

    private fun processFace(face: Face) {
        val leftEye  = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

        val eyeX: Float
        val eyeY: Float

        when {
            leftEye != null && rightEye != null -> {
                eyeX = (leftEye.position.x + rightEye.position.x) / 2f
                eyeY = (leftEye.position.y + rightEye.position.y) / 2f
            }
            leftEye != null -> {
                eyeX = leftEye.position.x
                eyeY = leftEye.position.y
            }
            rightEye != null -> {
                eyeX = rightEye.position.x
                eyeY = rightEye.position.y
            }
            else -> return
        }

        if (isCalibrating) {
            calibrationSamples.add(Pair(eyeX, eyeY))
            if (calibrationSamples.size >= requiredCalibrationSamples) {
                finishCalibration()
            }
            return
        }

        // Live direction detection
        val deltaX = eyeX - baselineX
        val deltaY = eyeY - baselineY
        val absX = Math.abs(deltaX)
        val absY = Math.abs(deltaY)

        val direction = when {
            absX < detectionThreshold && absY < detectionThreshold -> GazeDirection.CENTER
            absY > absX -> if (deltaY < 0) GazeDirection.UP else GazeDirection.DOWN
            else        -> if (deltaX > 0) GazeDirection.RIGHT else GazeDirection.LEFT
        }

        if (direction == lastDirection) {
            consecutiveFrames++
        } else {
            consecutiveFrames = 1
            lastDirection = direction
        }

        if (consecutiveFrames == requiredFrames &&
            direction != GazeDirection.CENTER &&
            direction != GazeDirection.UNKNOWN) {
            listener?.onDirectionDetected(direction)
            consecutiveFrames = 0
        }
    }
}
