package com.quickcommand.gesture

import android.graphics.PointF
import com.quickcommand.model.GesturePoint
import kotlin.math.*

/**
 * 手势匹配引擎 — 基于简化的 $1 Recognizer 算法
 * 支持预定义模板匹配和自定义手势比对
 */
object GestureMatcher {

    // 重采样点数
    private const val NUM_POINTS = 64
    // 参考正方形大小
    private const val SQUARE_SIZE = 250f
    // 匹配阈值（越低越严格）
    private const val MATCH_THRESHOLD = 80f  // 归一化后的平均点距离阈值（SQUARE_SIZE=250）

    // 预定义手势模板（归一化后的 64 点路径）
    private val templates: MutableMap<String, List<PointF>> by lazy {
        android.util.Log.d("GestureMatcher", "Generating templates...")
        val map = mutableMapOf<String, List<PointF>>()
        try {
            map["CIRCLE"] = generateCircleTemplate()
            map["TRIANGLE"] = generateTriangleTemplate()
            map["SQUARE"] = generateSquareTemplate()
            map["V_SHAPE"] = generateVShapeTemplate()
            map["CHECKMARK"] = generateCheckmarkTemplate()
            android.util.Log.d("GestureMatcher", "Templates generated: ${map.size}")
            for ((name, pts) in map) {
                android.util.Log.d("GestureMatcher", "  $name -> ${pts.size} points")
            }
        } catch (e: Exception) {
            android.util.Log.e("GestureMatcher", "Template generation FAILED", e)
        }
        map
    }



    private fun generateCircleTemplate(): List<PointF> {
        val cx = SQUARE_SIZE / 2f
        val cy = SQUARE_SIZE / 2f
        val r = SQUARE_SIZE / 2f * 0.8f
        return (0 until NUM_POINTS).map { i ->
            val angle = 2.0 * Math.PI * i / NUM_POINTS
            PointF(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
        }
    }

    private fun generateTriangleTemplate(): List<PointF> {
        val margin = SQUARE_SIZE * 0.1f
        val points = listOf(
            PointF(SQUARE_SIZE / 2f, margin),           // 顶点
            PointF(margin, SQUARE_SIZE - margin),        // 左下
            PointF(SQUARE_SIZE - margin, SQUARE_SIZE - margin) // 右下
        )
        return resamplePath(interpolatePath(points), NUM_POINTS)
    }

    private fun generateSquareTemplate(): List<PointF> {
        val margin = SQUARE_SIZE * 0.1f
        val points = listOf(
            PointF(margin, margin),
            PointF(SQUARE_SIZE - margin, margin),
            PointF(SQUARE_SIZE - margin, SQUARE_SIZE - margin),
            PointF(margin, SQUARE_SIZE - margin)
        )
        return resamplePath(interpolatePath(points), NUM_POINTS)
    }

    private fun generateVShapeTemplate(): List<PointF> {
        val margin = SQUARE_SIZE * 0.1f
        val points = listOf(
            PointF(margin, margin),
            PointF(SQUARE_SIZE / 2f, SQUARE_SIZE - margin),
            PointF(SQUARE_SIZE - margin, margin)
        )
        return resamplePath(interpolatePath(points), NUM_POINTS)
    }

    private fun generateCheckmarkTemplate(): List<PointF> {
        val margin = SQUARE_SIZE * 0.15f
        val points = listOf(
            PointF(margin, SQUARE_SIZE / 2f),
            PointF(SQUARE_SIZE * 0.4f, SQUARE_SIZE - margin),
            PointF(SQUARE_SIZE - margin, margin)
        )
        return resamplePath(interpolatePath(points), NUM_POINTS)
    }

    /**
     * 将原始触摸点列表转换为归一化路径（用于存储自定义手势）
     */
    fun normalizePath(rawPoints: List<GesturePoint>): String {
        val points = rawPoints.map { PointF(it.x, it.y) }
        val resampled = resamplePath(points, NUM_POINTS)
        val rotated = rotateToZero(resampled)
        val scaled = scaleToSquare(rotated, SQUARE_SIZE)
        val translated = translateToOrigin(scaled)
        return com.google.gson.Gson().toJson(translated.map { mapOf("x" to it.x, "y" to it.y) })
    }

    /**
     * 匹配手势 — 同时检查预定义模板和自定义手势
     * @return 匹配到的 GestureType 名称，或 null
     */
    fun match(
        rawPoints: List<GesturePoint>,
        customGestures: Map<String, List<GesturePoint>> = emptyMap()
    ): String? {
        android.util.Log.d("GestureMatcher", "match() called with ${rawPoints.size} raw points")
        if (rawPoints.size < 10) {
            android.util.Log.d("GestureMatcher", "Too few points (<10), returning null")
            return null
        }

        val points = rawPoints.map { PointF(it.x, it.y) }
        android.util.Log.d("GestureMatcher", "Normalizing candidate...")
        val candidate = normalizePoints(points)
        android.util.Log.d("GestureMatcher", "Candidate normalized: ${candidate.size} points")

        android.util.Log.d("GestureMatcher", "Templates count: ${templates.size}")
        var bestScore = Float.MAX_VALUE
        var bestMatch: String? = null

        // 匹配预定义模板
        for ((name, template) in templates) {
            android.util.Log.d("GestureMatcher", "Matching against template: $name (${template.size} pts)")
            val score = distanceAtBestAngle(candidate, template)
            android.util.Log.d("GestureMatcher", "Template $name score=$score (threshold=$MATCH_THRESHOLD)")
            if (score < bestScore) {
                bestScore = score
                bestMatch = name
            }
        }

        // 匹配自定义手势
        for ((name, gesturePoints) in customGestures) {
            val template = normalizePoints(
                gesturePoints.map { PointF(it.x, it.y) }
            )
            val score = distanceAtBestAngle(candidate, template)
            android.util.Log.d("GestureMatcher", "Custom $name score=$score")
            if (score < bestScore) {
                bestScore = score
                bestMatch = name
            }
        }

        android.util.Log.d("GestureMatcher", "Best match: $bestMatch score=$bestScore threshold=$MATCH_THRESHOLD")
        return if (bestScore < MATCH_THRESHOLD) bestMatch else null
    }

    /**
     * 检测简单的滑动手势
     */
    fun detectSwipe(rawPoints: List<GesturePoint>): String? {
        if (rawPoints.size < 10) return null
        val first = rawPoints.first()
        val last = rawPoints.last()
        val dx = last.x - first.x
        val dy = last.y - first.y
        val minDistance = 300f

        // 计算路径总长度，滑动应该是比较直的路径
        var totalLen = 0f
        for (i in 0 until rawPoints.size - 1) {
            totalLen += sqrt(
                (rawPoints[i + 1].x - rawPoints[i].x).pow(2) +
                (rawPoints[i + 1].y - rawPoints[i].y).pow(2)
            )
        }
        val directDist = sqrt(dx * dx + dy * dy)
        // 直线度 = 直线距离 / 路径总长度，越接近 1 越直
        val straightness = if (totalLen > 0) directDist / totalLen else 0f
        if (straightness < 0.85f) return null  // 直线度要求更高，避免对勾/V形被误判

        if (abs(dx) < minDistance && abs(dy) < minDistance) return null

        return when {
            abs(dx) > abs(dy) && dx > minDistance -> "SWIPE_RIGHT"
            abs(dx) > abs(dy) && dx < -minDistance -> "SWIPE_LEFT"
            abs(dy) > abs(dx) && dy > minDistance -> "SWIPE_DOWN"
            abs(dy) > abs(dx) && dy < -minDistance -> "SWIPE_UP"
            else -> null
        }
    }

    // ─── 规范化处理 ───

    private fun normalizePoints(points: List<PointF>): List<PointF> {
        val resampled = resamplePath(points, NUM_POINTS)
        val rotated = rotateToZero(resampled)
        val scaled = scaleToSquare(rotated, SQUARE_SIZE)
        return translateToOrigin(scaled)
    }

    private fun resamplePath(points: List<PointF>, n: Int): List<PointF> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return List(n) { points[0] }

        val I = pathLength(points) / (n - 1)
        if (I <= 0f) return List(n) { points.first() }

        var D = 0f
        val newPoints = mutableListOf(points.first())
        val pts = points.toMutableList()
        var i = 0
        val maxIter = 5000 // 安全上限
        var iter = 0
        while (i < pts.size - 1 && newPoints.size < n && iter < maxIter) {
            iter++
            val d = distance(pts[i], pts[i + 1])
            if (d == 0f) {
                i++
                continue
            }
            if (D + d >= I) {
                val t = (I - D) / d
                val qx = pts[i].x + t * (pts[i + 1].x - pts[i].x)
                val qy = pts[i].y + t * (pts[i + 1].y - pts[i].y)
                val q = PointF(qx, qy)
                newPoints.add(q)
                pts.add(i + 1, q)
                D = 0f
                i++
            } else {
                D += d
                i++
            }
        }
        // 补齐到 n 个点
        while (newPoints.size < n) {
            newPoints.add(pts.last())
        }
        return newPoints.take(n)
    }

    private fun pathLength(points: List<PointF>): Float {
        var d = 0f
        for (i in 0 until points.size - 1) {
            d += distance(points[i], points[i + 1])
        }
        return d
    }

    private fun centroid(points: List<PointF>): PointF {
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        return PointF(cx, cy)
    }

    private fun rotateToZero(points: List<PointF>): List<PointF> {
        val c = centroid(points)
        val angle = atan2(c.y - points[0].y, c.x - points[0].x)
        return rotateBy(points, -angle)
    }

    private fun rotateBy(points: List<PointF>, angle: Float): List<PointF> {
        val c = centroid(points)
        val cosA = cos(angle)
        val sinA = sin(angle)
        return points.map { p ->
            val dx = p.x - c.x
            val dy = p.y - c.y
            PointF(
                dx * cosA - dy * sinA + c.x,
                dx * sinA + dy * cosA + c.y
            )
        }
    }

    private fun scaleToSquare(points: List<PointF>, size: Float): List<PointF> {
        val b = boundingBox(points)
        val scale = if (b.width() > b.height()) size / b.width() else size / b.height()
        return points.map { PointF(it.x * scale, it.y * scale) }
    }

    private fun translateToOrigin(points: List<PointF>): List<PointF> {
        val c = centroid(points)
        return points.map { PointF(it.x - c.x, it.y - c.y) }
    }

    private fun boundingBox(points: List<PointF>): RectF {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

    private fun distanceAtBestAngle(points: List<PointF>, template: List<PointF>): Float {
        val phi = 0.5f * (-1.0 + sqrt(5.0)).toFloat() // golden ratio
        var a = -45f.toRad()
        var b = 45f.toRad()
        var x1 = phi * a + (1 - phi) * b
        var f1 = pathDistance(points, template, x1)
        var x2 = (1 - phi) * a + phi * b
        var f2 = pathDistance(points, template, x2)

        for (i in 0 until 10) {
            if (f1 < f2) {
                b = x2; x2 = x1; f2 = f1
                x1 = phi * a + (1 - phi) * b
                f1 = pathDistance(points, template, x1)
            } else {
                a = x1; x1 = x2; f1 = f2
                x2 = (1 - phi) * a + phi * b
                f2 = pathDistance(points, template, x2)
            }
        }
        return minOf(f1, f2)
    }

    private fun pathDistance(pts1: List<PointF>, pts2: List<PointF>, angle: Float): Float {
        val rotated = rotateBy(pts2, angle)
        var d = 0f
        for (i in pts1.indices) {
            d += distance(pts1[i], rotated[i])
        }
        return d / pts1.size
    }

    private fun distance(a: PointF, b: PointF): Float =
        sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))

    // 辅助：路径插值（补点）
    private fun interpolatePath(points: List<PointF>): List<PointF> {
        val result = mutableListOf(points[0])
        for (i in 0 until points.size - 1) {
            val a = points[i]; val b = points[i + 1]
            val d = distance(a, b)
            // 限制插值步长，避免点过多导致 OOM
            val steps = maxOf(1, (d / 20).toInt())
            for (s in 1..steps) {
                val t = s.toFloat() / steps
                result.add(PointF(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
            }
        }
        return result
    }

    private fun Float.toRad() = this * Math.PI.toFloat() / 180f

    private data class RectF(
        val left: Float, val top: Float,
        val right: Float, val bottom: Float
    ) {
        fun width() = right - left
        fun height() = bottom - top
    }
}
