package com.handsfree.scroll.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation

class GestureFeedbackView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    fun showFeedback(isUp: Boolean) {
        paint.color = if (isUp) Color.GREEN else Color.BLUE
        
        val scaleAnim = ScaleAnimation(
            0.5f, 1.5f, 0.5f, 1.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnim.duration = 500
        
        val alphaAnim = AlphaAnimation(1.0f, 0.0f)
        alphaAnim.duration = 500
        
        startAnimation(scaleAnim)
        startAnimation(alphaAnim)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(width / 2f, height / 2f, (width / 2f) * 0.8f, paint)
    }
}
