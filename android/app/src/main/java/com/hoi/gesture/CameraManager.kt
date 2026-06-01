package com.hoi.gesture

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages the CameraX front-camera session for Hoi!
 * Feeds frames to HandGestureDetector and EyeTrackingDetector via ImageAnalysis.
 */
class CameraManager(private val context: Context) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageAnalyzer: ImageAnalysis? = null

    var onFrameAvailable: ((ImageProxy) -> Unit)? = null

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        onFrameAvailable?.invoke(imageProxy)
                        // Note: caller must close imageProxy after use
                    }
                }

            imageAnalyzer = imageAnalysis

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
                println("[CameraManager] Front camera started")
            } catch (e: Exception) {
                println("[CameraManager] Failed to bind camera: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
