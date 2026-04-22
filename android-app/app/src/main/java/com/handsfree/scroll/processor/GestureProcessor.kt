package com.handsfree.scroll.processor

import android.util.Log
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class GestureProcessor {
    companion object {
        private const val TAG = "GestureProcessor"
        private const val BUFFER_SIZE = 7
        private const val GESTURE_COOLDOWN = 1200L
    }

    private val yBuffer = mutableListOf<Float>()
    private var lastGestureTime = 0L

    enum class GestureResult {
        SWIPE_UP, SWIPE_DOWN, LIKE, PAUSE_PLAY, NONE
    }

    fun process(result: HandLandmarkerResult): GestureResult {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGestureTime < GESTURE_COOLDOWN) return GestureResult.NONE

        if (result.landmarks().isEmpty()) {
            yBuffer.clear()
            return GestureResult.NONE
        }

        val landmarks = result.landmarks()[0]
        
        // 1. Detection: Index Finger Tip (Landmark 8) for Scrolling
        val indexTipY = landmarks[8].y()
        yBuffer.add(indexTipY)
        if (yBuffer.size > BUFFER_SIZE) yBuffer.removeAt(0)

        if (yBuffer.size == BUFFER_SIZE) {
            val deltaY = yBuffer.last() - yBuffer.first()
            if (Math.abs(deltaY) > com.handsfree.scroll.utils.AppConfig.sensitivity) {
                lastGestureTime = currentTime
                yBuffer.clear()
                return if (deltaY < 0) GestureResult.SWIPE_UP else GestureResult.SWIPE_DOWN
            }
        }

        // 2. Detection: Thumb Up for LIKE (Landmark 4 vs Landmarks 2, 3)
        // Simple logic: Thumb tip higher than thumb IP and all other fingers curled
        if (isThumbUp(landmarks)) {
            lastGestureTime = currentTime
            return GestureResult.LIKE
        }

        // 3. Detection: Open Palm for PAUSE/PLAY
        if (isOpenPalm(landmarks)) {
            lastGestureTime = currentTime
            return GestureResult.PAUSE_PLAY
        }

        return GestureResult.NONE
    }

    private fun isThumbUp(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Boolean {
        val thumbTip = landmarks[4]
        val thumbIp = landmarks[3]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        
        // Thumb is up if it's the highest point and other fingers are below palm center
        return thumbTip.y() < thumbIp.y() && thumbTip.y() < indexTip.y() && thumbTip.y() < middleTip.y()
    }

    private fun isOpenPalm(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Boolean {
        // Simple heuristic: distance between wrist (0) and middle tip (12) is large
        // and fingers are spread out (index tip 8 distance from pinky tip 20)
        val wrist = landmarks[0]
        val middleTip = landmarks[12]
        val distY = Math.abs(wrist.y() - middleTip.y())
        return distY > 0.4f // Normalized height
    }
}
