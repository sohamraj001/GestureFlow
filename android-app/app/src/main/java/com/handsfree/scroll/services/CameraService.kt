package com.handsfree.scroll.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.handsfree.scroll.processor.GestureProcessor
import com.handsfree.scroll.utils.GestureEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class CameraService : LifecycleService() {

    private var handLandmarker: HandLandmarker? = null
    private val gestureProcessor = GestureProcessor()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    private var frameCounter = 0
    private val FRAME_SKIP_RATE = 2 // Process every 2nd frame for optimization

    companion object {
        private const val TAG = "CameraService"
        private const val NOTIFICATION_ID = 2024
        private const val CHANNEL_ID = "GestureFlow_Channel"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        setupHandLandmarker()
        startCamera()
    }

    private fun setupHandLandmarker() {
        val baseOptions = com.google.mediapipe.tasks.core.BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setNumHands(1)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::onLandmarkResult)
            .setErrorListener { e -> Log.e(TAG, "MediaPipe Error: ${e.message}") }
            .build()

        try {
            handLandmarker = HandLandmarker.createFromOptions(this, options)
            Log.d(TAG, "AI Engine Initialized")
        } catch (e: Exception) {
            Log.e(TAG, "AI Engine Failed to Load: ${e.message}")
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 640)) // Professional Optimization: Lower resolution
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                frameCounter++
                if (frameCounter % FRAME_SKIP_RATE == 0) {
                    processImage(imageProxy)
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, imageAnalyzer)
                Log.d(TAG, "CameraX Bound to Lifecycle")
            } catch (e: Exception) {
                Log.e(TAG, "Camera Binding Failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, System.currentTimeMillis())
        imageProxy.close()
    }

    private fun onLandmarkResult(result: HandLandmarkerResult, input: com.google.mediapipe.framework.image.MPImage) {
        val gesture = gestureProcessor.process(result)
        if (gesture != GestureProcessor.GestureResult.NONE) {
            lifecycleScope.launch(Dispatchers.Main) {
                val event = when (gesture) {
                    GestureProcessor.GestureResult.SWIPE_UP -> GestureEventBus.GestureEvent.SWIPE_UP
                    GestureProcessor.GestureResult.SWIPE_DOWN -> GestureEventBus.GestureEvent.SWIPE_DOWN
                    GestureProcessor.GestureResult.LIKE -> GestureEventBus.GestureEvent.LIKE_GESTURE
                    GestureProcessor.GestureResult.PAUSE_PLAY -> GestureEventBus.GestureEvent.PAUSE_PLAY_GESTURE
                    else -> null
                }
                event?.let { 
                    Log.d(TAG, "Gesture Detected: $it")
                    GestureEventBus.emit(it) 
                }
            }
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "GestureFLOW Background Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GestureFLOW Active")
            .setContentText("Monitoring gestures for hands-free scrolling")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        handLandmarker?.close()
    }
}
