package com.hoi.gesture

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.hands.HandDetection
import com.google.mlkit.vision.hands.HandDetectorOptions
import com.google.mlkit.vision.hands.HandLandmark

/**
 * Detects Rock, Paper, Scissors gestures using ML Kit Hands API.
 * Requires ML Kit Hands dependency in build.gradle:
 *   implementation 'com.google.mlkit:hands:2.0.0-beta1'
 */

enum class RPSGesture {
    ROCK, PAPER, SCISSORS, UNKNOWN;

    fun beats(other: RPSGesture): Boolean = when {
        this == ROCK && other == SCISSORS -> true
        this == SCISSORS && other == PAPER -> true
        this == PAPER && other == ROCK -> true
        else -> false
    }

    val displayName: String get() = when (this) {
        ROCK     -> "✊ Rock"
        PAPER    -> "✋ Paper"
        SCISSORS -> "✌️ Scissors"
        UNKNOWN  -> "❓ Unknown"
    }
}

class HandGestureDetector {

    private val handDetector = HandDetection.getClient(
        HandDetectorOptions.Builder()
            .setPerformanceMode(HandDetectorOptions.PERFORMANCE_MODE_STREAM)
            .setMinHandDetectionConfidence(0.7f)
            .setMinTrackingConfidence(0.5f)
            .build()
    )

    /**
     * Process a camera frame and return a gesture classification.
     * Caller must close imageProxy after this returns.
     */
    @androidx.camera.core.ExperimentalGetImage
    fun detect(imageProxy: ImageProxy, callback: (RPSGesture) -> Unit) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            callback(RPSGesture.UNKNOWN)
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        handDetector.process(inputImage)
            .addOnSuccessListener { hands ->
                if (hands.isEmpty()) {
                    callback(RPSGesture.UNKNOWN)
                } else {
                    val gesture = classify(hands[0].allLandmarks())
                    callback(gesture)
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                println("[HandGestureDetector] ML Kit error: ${it.message}")
                callback(RPSGesture.UNKNOWN)
                imageProxy.close()
            }
    }

    private fun classify(landmarks: List<com.google.mlkit.vision.hands.HandLandmark>): RPSGesture {
        // Get key landmark positions
        val indexTip  = landmarks.getOrNull(HandLandmark.INDEX_FINGER_TIP)  ?: return RPSGesture.UNKNOWN
        val middleTip = landmarks.getOrNull(HandLandmark.MIDDLE_FINGER_TIP) ?: return RPSGesture.UNKNOWN
        val ringTip   = landmarks.getOrNull(HandLandmark.RING_FINGER_TIP)   ?: return RPSGesture.UNKNOWN
        val pinkyTip  = landmarks.getOrNull(HandLandmark.PINKY_TIP)         ?: return RPSGesture.UNKNOWN

        val indexMCP  = landmarks.getOrNull(HandLandmark.INDEX_FINGER_MCP)  ?: return RPSGesture.UNKNOWN
        val middleMCP = landmarks.getOrNull(HandLandmark.MIDDLE_FINGER_MCP) ?: return RPSGesture.UNKNOWN
        val ringMCP   = landmarks.getOrNull(HandLandmark.RING_FINGER_MCP)   ?: return RPSGesture.UNKNOWN
        val pinkyMCP  = landmarks.getOrNull(HandLandmark.PINKY_MCP)         ?: return RPSGesture.UNKNOWN

        val threshold = 20f // pixels

        // Finger is "extended" if tip is above (lower y) than MCP joint
        val indexExtended  = indexMCP.position.y  - indexTip.position.y  > threshold
        val middleExtended = middleMCP.position.y - middleTip.position.y > threshold
        val ringExtended   = ringMCP.position.y   - ringTip.position.y   > threshold
        val pinkyExtended  = pinkyMCP.position.y  - pinkyTip.position.y  > threshold

        return when {
            !indexExtended && !middleExtended && !ringExtended && !pinkyExtended -> RPSGesture.ROCK
            indexExtended && middleExtended && ringExtended && pinkyExtended     -> RPSGesture.PAPER
            indexExtended && middleExtended && !ringExtended && !pinkyExtended   -> RPSGesture.SCISSORS
            else -> RPSGesture.UNKNOWN
        }
    }
}
