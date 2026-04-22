package com.handsfree.scroll.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import androidx.camera.view.PreviewView
import com.handsfree.scroll.R
import com.handsfree.scroll.utils.GestureEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_overlay, null)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 20
        params.y = 100

        windowManager.addView(floatingView, params)

        setupDragLogic()
        listenForGestureFeedback()
    }

    private fun setupDragLogic() {
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (initialTouchX - event.rawX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun listenForGestureFeedback() {
        val indicator = floatingView?.findViewById<ImageView>(R.id.gestureIndicator)
        serviceScope.launch {
            GestureEventBus.events.collect { event ->
                indicator?.visibility = View.VISIBLE
                when (event) {
                    GestureEventBus.GestureEvent.SWIPE_UP -> indicator?.setImageResource(android.R.drawable.ic_menu_upload)
                    GestureEventBus.GestureEvent.SWIPE_DOWN -> indicator?.setImageResource(android.R.drawable.ic_menu_save)
                    GestureEventBus.GestureEvent.LIKE_GESTURE -> indicator?.setImageResource(android.R.drawable.btn_star_big_on)
                    GestureEventBus.GestureEvent.PAUSE_PLAY_GESTURE -> indicator?.setImageResource(android.R.drawable.ic_media_pause)
                }
                
                // Hide after 500ms
                kotlinx.coroutines.delay(500)
                indicator?.visibility = View.GONE
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) windowManager.removeView(floatingView)
    }
}
