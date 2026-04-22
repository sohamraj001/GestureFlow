package com.handsfree.scroll.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.handsfree.scroll.utils.GestureEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GestureAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        private const val TAG = "GestureService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        
        // Listen for events from GestureEventBus
        serviceScope.launch {
            GestureEventBus.events.collect { event ->
                handleGestureEvent(event)
            }
        }
    }

    private fun handleGestureEvent(event: GestureEventBus.GestureEvent) {
        when (event) {
            GestureEventBus.GestureEvent.SWIPE_UP -> performSwipe(true)
            GestureEventBus.GestureEvent.SWIPE_DOWN -> performSwipe(false)
            GestureEventBus.GestureEvent.LIKE_GESTURE -> performDoubleTap()
            GestureEventBus.GestureEvent.PAUSE_PLAY_GESTURE -> performSingleTap()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun performSwipe(isUp: Boolean) {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        val startX = width / 2f
        val startY = if (isUp) height * 0.8f else height * 0.2f
        val endY = if (isUp) height * 0.2f else height * 0.8f

        path.moveTo(startX, startY)
        path.lineTo(startX, endY)

        val stroke = GestureDescription.StrokeDescription(path, 0, 250)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Dispatched Swipe: ${if (isUp) "UP" else "DOWN"}")
    }

    private fun performDoubleTap() {
        // Simulating double tap for Like (Reels/Shorts)
        val displayMetrics = resources.displayMetrics
        val x = displayMetrics.widthPixels / 2f
        val y = displayMetrics.heightPixels / 2f

        val path = Path()
        path.moveTo(x, y)
        
        val stroke1 = GestureDescription.StrokeDescription(path, 0, 50)
        val stroke2 = GestureDescription.StrokeDescription(path, 150, 50)
        
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .build()
            
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Dispatched Double Tap (Like)")
    }

    private fun performSingleTap() {
        // Simulating single tap for Pause/Play
        val displayMetrics = resources.displayMetrics
        val x = displayMetrics.widthPixels / 2f
        val y = displayMetrics.heightPixels / 2f

        val path = Path()
        path.moveTo(x, y)
        
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Dispatched Single Tap (Pause/Play)")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
