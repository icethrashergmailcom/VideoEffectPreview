package com.example.videoeffectpreview.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "PadControl"

class PadControl(context: Context, attrs: AttributeSet? = null): View(context, attrs) {
    private var callbacks: Callbacks? = null
    private val callbackCs = ReentrantLock()

    private var captured: Boolean = false
    private var lastTouchPosition: PointF? = null
    private var lastPosition: PointF = PointF(0F, 0F)

    private val radius = 50F
    private val backgroundPaint = Paint().apply {
        color = 0xffe0e0e0.toInt()
    }
    private val spotPaint = Paint().apply {
        color = 0xffaa0000.toInt()
    }
    private val activeSpotPaint = Paint().apply {
        color = 0xff0000aa.toInt()
        strokeWidth = 10F
    }

    interface Callbacks {
        fun onPadPositionChanged(x: Float, y: Float)
    }

    val xPosition
        get () = lastPosition.x
    val yPosition
        get() = lastPosition.y

    fun setPosition(x: Float, y:Float) {
        lastPosition = PointF(x.toFloat(), y.toFloat())

        if (width != 0) {
            updateFromPosition(x, y)
        } else {
            callbackCs.withLock {
                callbacks?.onPadPositionChanged(x, y)
            }
        }
    }

    private fun updateFromPosition(x: Float, y: Float) {
        updatePosition(x * width, y * height)
    }

    private fun updatePosition(touchX: Float, touchY: Float) {
        var x: Float = when {
                touchX < 0 -> 0F
                touchX > width -> (1F * width)
                else -> touchX
            }

        var y: Float = when {
            touchY < 0 -> 0F
            touchY > height -> (1F * height)
            else -> touchY
        }

        lastTouchPosition = PointF(x, y)
        lastPosition = PointF(x / width, y / height)
        invalidate()

        callbackCs.withLock {
            callbacks?.onPadPositionChanged(xPosition, yPosition)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                captured = true
                updatePosition(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                captured = true
                updatePosition(event.x, event.y)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_OUTSIDE-> {
                captured = false
                invalidate()
            }
            else -> {}
        }

        return true
    }

    override fun onDraw(canvas: Canvas?) {
        val c = canvas ?: return
        if (width == 0) return
        var p = lastTouchPosition!!

        c.drawPaint(backgroundPaint)

        if (captured) {
            c.drawLine(
                0F,
                p.y,
                1F * width,
                p.y,
                activeSpotPaint)
            c.drawLine(
                p.x,
                0F,
                p.x,
                1F * height,
                activeSpotPaint)
            c.drawCircle(
                p.x,
                p.y,
                radius,
                activeSpotPaint)
        } else {
            c.drawCircle(
                p.x,
                p.y,
                radius,
                spotPaint
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (width != 0 && lastTouchPosition == null) {
            updateFromPosition(lastPosition.x, lastPosition.y)
        }
    }

    fun setCallbacks(callbacks: Callbacks) {
        callbackCs.withLock { this.callbacks = callbacks }
    }
}