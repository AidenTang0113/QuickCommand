package com.quickcommand.gesture

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.quickcommand.model.GesturePoint

/**
 * 手势绘制视图 — 捕获用户触摸轨迹并在 Canvas 上实时绘制
 */
class GestureDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pathPaint = Paint().apply {
        color = Color.parseColor("#4FC3F7")
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#1A1A2E")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val hintPaint = Paint().apply {
        color = Color.parseColor("#66FFFFFF")
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val path = Path()
    private val points = mutableListOf<GesturePoint>()

    var onGestureComplete: ((List<GesturePoint>) -> Unit)? = null
    var hintText: String = "在此绘制手势"

    private val startTime = System.currentTimeMillis()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 背景
        canvas.drawRoundRect(20f, 20f, width - 20f, height - 20f, 30f, 30f, bgPaint)

        // 提示文字
        if (points.isEmpty()) {
            canvas.drawText(hintText, width / 2f, height / 2f, hintPaint)
        }

        // 绘制路径
        canvas.drawPath(path, pathPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                points.clear()
                path.moveTo(x, y)
                points.add(GesturePoint(x, y, System.currentTimeMillis() - startTime))
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                points.add(GesturePoint(x, y, System.currentTimeMillis() - startTime))
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                Log.d("GestureDrawView", "ACTION_UP, points size=${points.size}")
                if (points.size >= 5) {
                    Log.d("GestureDrawView", "Invoking onGestureComplete")
                    onGestureComplete?.invoke(points.toList())
                } else {
                    Log.d("GestureDrawView", "Not enough points, skipping")
                }
            }
        }
        return true
    }

    fun clear() {
        path.reset()
        points.clear()
        invalidate()
    }
}
