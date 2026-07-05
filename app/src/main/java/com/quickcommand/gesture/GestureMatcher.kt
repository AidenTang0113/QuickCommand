package com.quickcommand.gesture

import android.graphics.PointF
import android.util.Log
import com.quickcommand.model.GesturePoint
import kotlin.math.*

/**
 * 完整版 $1 Recognizer
 *
 * 实现 "$1 Unistroke Recognizer" 论文原版算法：
 * Wobbrock, J.O., Wilson, A.D., and Li, Y. (2007).
 * "Gestures without libraries, toolkits or training:
 *  A $1 recognizer for user interface prototypes."
 * UIST '07, pp. 159-168.
 *
 * 完整步骤：
 *   1. Resample Path → 等间距重采样到 N 个点
 *   2. Rotate To Zero → 旋转使指示角为 0°
 *   3. Scale To Square → 缩放到参考正方形
 *   4. Translate To Origin → 平移使质心在原点
 *   5. Distance At Best Angle → 黄金分割搜索最佳旋转角度下的路径距离
 *
 * 支持多模板：每个手势类型可以有多个样本模板，取最佳匹配。
 */
object GestureMatcher {

    private const val TAG = "GestureMatcher"

    // ─── 论文常量 ───
    /** 重采样点数 (论文推荐 64) */
    private const val NUM_POINTS = 64

    /** 参考正方形边长 (论文推荐 250) */
    private const val SQUARE_SIZE = 250.0f

    /** 识别阈值：路径距离低于此值视为匹配 (论文未给出固定值，经验值) */
    private const val MATCH_THRESHOLD = 0.45f

    /** 黄金分割搜索的角度范围 (论文: ±45°) */
    private const val ANGLE_RANGE = 45.0f

    /** 黄金分割搜索的精度 (论文: 2°) */
    private const val ANGLE_PRECISION = 2.0f

    /** PHI = (√5 - 1) / 2 ≈ 0.618034 (黄金分割比) */
    private const val PHI = 0.6180339887f

    // ─── 模板存储 ───
    /** 预定义手势模板：手势名 → 多个归一化模板点集 */
    private val templates: MutableMap<String, MutableList<List<PointF>>> = mutableMapOf()

    init {
        Log.d(TAG, "Initializing $1 Recognizer (full implementation)")
        generatePredefinedTemplates()
        Log.d(TAG, "Templates initialized: ${templates.keys.joinToString()} (total samples: ${templates.values.sumOf { it.size }})")
    }

    // ─── 公开 API ───

    /**
     * 匹配手势 — 同时检查预定义模板和自定义手势
     *
     * @param rawPoints 用户绘制的原始触摸点
     * @param customGestures 自定义手势：手势名 → 原始点列表（可含多个样本）
     * @return 匹配到的手势名称，或 null
     */
    fun match(
        rawPoints: List<GesturePoint>,
        customGestures: Map<String, List<GesturePoint>> = emptyMap()
    ): String? {
        Log.d(TAG, "match() called with ${rawPoints.size} raw points")

        // 论文要求至少 10 个点才有意义
        if (rawPoints.size < 10) {
            Log.d(TAG, "Too few points (<10), returning null")
            return null
        }

        // 将候选点转为 PointF
        val candidateRaw = rawPoints.map { PointF(it.x, it.y) }
        Log.d(TAG, "Normalizing candidate...")

        // 完整规范化：resample → rotate → scale → translate
        val candidate = normalizePath(candidateRaw)
        Log.d(TAG, "Candidate normalized: ${candidate.size} points, bbox=${boundingBox(candidate)}")

        var bestScore = Float.MAX_VALUE
        var bestMatch: String? = null

        // ─── 匹配预定义模板（多模板支持）───
        Log.d(TAG, "Checking ${templates.size} predefined gesture types")
        for ((name, templateList) in templates) {
            for ((idx, template) in templateList.withIndex()) {
                val score = distanceAtBestAngle(candidate, template)
                Log.d(TAG, "  Template $name[$idx] score=$score (threshold=${MATCH_THRESHOLD * SQUARE_SIZE})")
                if (score < bestScore) {
                    bestScore = score
                    bestMatch = name
                }
            }
        }

        // ─── 匹配自定义手势 ───
        Log.d(TAG, "Checking ${customGestures.size} custom gestures")
        for ((name, gesturePoints) in customGestures) {
            val template = normalizePath(gesturePoints.map { PointF(it.x, it.y) })
            val score = distanceAtBestAngle(candidate, template)
            Log.d(TAG, "  Custom $name score=$score")
            if (score < bestScore) {
                bestScore = score
                bestMatch = name
            }
        }

        // 论文：分数为平均点距离，需要除以 SQUARE_SIZE 归一化
        val normalizedScore = bestScore / SQUARE_SIZE
        Log.d(TAG, "Best match: $bestMatch, normalized score=$normalizedScore, threshold=$MATCH_THRESHOLD")

        return if (normalizedScore < MATCH_THRESHOLD) bestMatch else null
    }

    /**
     * 检测简单的滑动手势（上/下/左/右）
     *
     * 不属于 $1 论文范畴，作为补充功能保留。
     * 基于起止点位移 + 直线度判定。
     */
    fun detectSwipe(rawPoints: List<GesturePoint>): String? {
        if (rawPoints.size < 10) return null

        val first = rawPoints.first()
        val last = rawPoints.last()
        val dx = last.x - first.x
        val dy = last.y - first.y
        val minDistance = 300f

        // 计算路径总长度
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
        if (straightness < 0.85f) return null

        if (abs(dx) < minDistance && abs(dy) < minDistance) return null

        return when {
            abs(dx) > abs(dy) && dx > minDistance -> "SWIPE_RIGHT"
            abs(dx) > abs(dy) && dx < -minDistance -> "SWIPE_LEFT"
            abs(dy) > abs(dx) && dy > minDistance -> "SWIPE_DOWN"
            abs(dy) > abs(dx) && dy < -minDistance -> "SWIPE_UP"
            else -> null
        }
    }

    /**
     * 将原始触摸点列表转换为归一化路径的 JSON 字符串（用于存储自定义手势）
     */
    fun normalizePath(rawPoints: List<GesturePoint>): String {
        val points = rawPoints.map { PointF(it.x, it.y) }
        val normalized = normalizePath(points)
        return com.google.gson.Gson().toJson(normalized.map { mapOf("x" to it.x, "y" to it.y) })
    }

    // ─── $1 算法核心 ───

    /**
     * 完整规范化路径：resample → rotateToZero → scaleToSquare → translateToOrigin
     *
     * 这是论文中的核心预处理流程，每一步都不可省略。
     */
    private fun normalizePath(points: List<PointF>): List<PointF> {
        // Step 1: 重采样到 NUM_POINTS 个等间距点
        val resampled = resamplePath(points, NUM_POINTS)
        Log.v(TAG, "  Resampled: ${points.size} → ${resampled.size} points")

        // Step 2: 旋转使指示角（起始点→质心）为 0°
        val rotated = rotateToZero(resampled)
        Log.v(TAG, "  Rotated to zero indicative angle")

        // Step 3: 缩放到参考正方形
        val scaled = scaleToSquare(rotated, SQUARE_SIZE)
        Log.v(TAG, "  Scaled to ${SQUARE_SIZE}x${SQUARE_SIZE}")

        // Step 4: 平移使质心在原点
        val translated = translateToOrigin(scaled)
        Log.v(TAG, "  Translated to origin, centroid=${centroid(translated)}")

        return translated
    }

    /**
     * Step 1: Resample Path
     *
     * 将路径重采样为 n 个等弧长间距的点。
     * 论文公式：
     *   I = pathLength / (n - 1)    — 每个区间的目标间距
     *   遍历原始点，当累积距离 D + d >= I 时，在两点间插入新点
     */
    private fun resamplePath(points: List<PointF>, n: Int): List<PointF> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return List(n) { points[0] }

        // 计算 I = 总路径长度 / (n - 1)
        val totalLen = pathLength(points)
        val I = totalLen / (n - 1).toFloat()
        if (I <= 0f) return List(n) { points.first() }

        Log.v(TAG, "    Resample: totalLen=$totalLen, I=$I, target=$n points")

        // 论文算法：从第一个点开始，逐段插入等间距点
        val newPoints = mutableListOf<PointF>()
        newPoints.add(points.first())

        // 使用可变副本进行遍历（需要在中间插入新点）
        val pts = points.toMutableList()
        var D = 0.0f  // 累积距离
        var i = 0

        while (i < pts.size - 1) {
            val d = distance(pts[i], pts[i + 1])
            if (d == 0f) {
                i++
                continue
            }

            if (D + d >= I) {
                // 在 pts[i] 和 pts[i+1] 之间插入新点 q
                // 论文公式：q = pts[i] + t * (pts[i+1] - pts[i])
                // 其中 t = (I - D) / d
                val t = (I - D) / d
                val qx = pts[i].x + t * (pts[i + 1].x - pts[i].x)
                val qy = pts[i].y + t * (pts[i + 1].y - pts[i].y)
                val q = PointF(qx, qy)

                newPoints.add(q)
                pts.add(i + 1, q)  // 插入新点到原始路径中

                D = 0.0f  // 重置累积距离
                i++  // 跳到新插入的点继续
            } else {
                D += d
                i++
            }
        }

        // 补齐到 n 个点（如果因为浮点精度问题少了一点）
        while (newPoints.size < n) {
            newPoints.add(pts.last())
        }

        // 确保不超过 n 个点
        return newPoints.take(n)
    }

    /**
     * Step 2: Rotate To Zero
     *
     * 旋转路径使指示角为 0°。
     * 指示角 = atan2(centroid.y - points[0].y, centroid.x - points[0].x)
     * 旋转角度 = -indicativeAngle
     */
    private fun rotateToZero(points: List<PointF>): List<PointF> {
        val c = centroid(points)
        val angle = atan2(c.y - points[0].y, c.x - points[0].x)
        return rotateBy(points, -angle)
    }

    /**
     * 旋转路径指定角度（绕质心旋转）
     *
     * 论文公式：
     *   q.x = (p.x - c.x) * cos(θ) - (p.y - c.y) * sin(θ) + c.x
     *   q.y = (p.x - c.x) * sin(θ) + (p.y - c.y) * cos(θ) + c.y
     */
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

    /**
     * Step 3: Scale To Square
     *
     * 将路径缩放到 size × size 的参考正方形。
     * 论文：按边界框的较大边等比缩放。
     */
    private fun scaleToSquare(points: List<PointF>, size: Float): List<PointF> {
        val b = boundingBox(points)
        val bWidth = b.right - b.left
        val bHeight = b.bottom - b.top

        // 防止除零
        val maxDim = maxOf(bWidth, bHeight)
        if (maxDim == 0f) return points.map { PointF(it.x, it.y) }

        val scale = size / maxDim
        Log.v(TAG, "    Scale: bbox=${bWidth}x${bHeight}, scale=$scale")
        return points.map { PointF(it.x * scale, it.y * scale) }
    }

    /**
     * Step 4: Translate To Origin
     *
     * 平移路径使质心在原点 (0, 0)。
     */
    private fun translateToOrigin(points: List<PointF>): List<PointF> {
        val c = centroid(points)
        return points.map { PointF(it.x - c.x, it.y - c.y) }
    }

    /**
     * Distance At Best Angle
     *
     * 使用黄金分割搜索 (Golden Section Search) 在 [-θ, +θ] 范围内
     * 寻找使路径距离最小的旋转角度。
     *
     * 论文：在 [-45°, +45°] 范围内搜索，精度 2°
     */
    private fun distanceAtBestAngle(candidate: List<PointF>, template: List<PointF>): Float {
        // 搜索范围：[-ANGLE_RANGE, +ANGLE_RANGE] 弧度
        var thetaA = -ANGLE_RANGE.toRadians()
        var thetaB = ANGLE_RANGE.toRadians()

        // 黄金分割搜索第一步
        var x1 = PHI * thetaA + (1 - PHI) * thetaB
        var f1 = pathDistance(candidate, template, x1)

        var x2 = (1 - PHI) * thetaA + PHI * thetaB
        var f2 = pathDistance(candidate, template, x2)

        Log.v(TAG, "    Golden section search: range=[${thetaA}, ${thetaB}]")
        Log.v(TAG, "    Initial: x1=$x1 f1=$f1, x2=$x2 f2=$f2")

        // 黄金分割搜索迭代
        while (abs(thetaB - thetaA) > ANGLE_PRECISION.toRadians()) {
            if (f1 < f2) {
                thetaB = x2
                x2 = x1
                f2 = f1
                x1 = PHI * thetaA + (1 - PHI) * thetaB
                f1 = pathDistance(candidate, template, x1)
            } else {
                thetaA = x1
                x1 = x2
                f1 = f2
                x2 = (1 - PHI) * thetaA + PHI * thetaB
                f2 = pathDistance(candidate, template, x2)
            }
        }

        val bestScore = minOf(f1, f2)
        Log.v(TAG, "    Best angle score: $bestScore")
        return bestScore
    }

    /**
     * Path Distance at Angle
     *
     * 将 template 旋转 angle 弧度后，计算 candidate 与 template 的平均点距离。
     *
     * 论文公式：
     *   distance = Σ distance(candidate[i], rotatedTemplate[i]) / n
     */
    private fun pathDistance(pts1: List<PointF>, pts2: List<PointF>, angle: Float): Float {
        val rotated = rotateBy(pts2, angle)
        var d = 0.0f
        for (i in pts1.indices) {
            d += distance(pts1[i], rotated[i])
        }
        return d / pts1.size
    }

    // ─── 辅助函数 ───

    /** 计算路径总长度 */
    private fun pathLength(points: List<PointF>): Float {
        var d = 0.0f
        for (i in 0 until points.size - 1) {
            d += distance(points[i], points[i + 1])
        }
        return d
    }

    /** 计算质心 (Centroid) */
    private fun centroid(points: List<PointF>): PointF {
        var cx = 0.0f
        var cy = 0.0f
        for (p in points) {
            cx += p.x
            cy += p.y
        }
        cx /= points.size
        cy /= points.size
        return PointF(cx, cy)
    }

    /** 计算两点间欧氏距离 */
    private fun distance(a: PointF, b: PointF): Float =
        sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))

    /** 计算边界框 */
    private fun boundingBox(points: List<PointF>): android.graphics.RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return android.graphics.RectF(minX, minY, maxX, maxY)
    }

    /** 角度转弧度 */
    private fun Float.toRadians(): Float = this * PI.toFloat() / 180.0f

    // ─── 预定义模板生成 ───

    /**
     * 生成预定义手势模板。
     *
     * 每个模板通过定义关键点 → 插值 → 规范化生成。
     * 为提高识别率，每种形状生成多个变体模板。
     */
    private fun generatePredefinedTemplates() {
        try {
            // ─── 圆圈：完整 360° 圆 ───
            templates["CIRCLE"] = mutableListOf()
            // 模板 1：标准圆（顺时针，从右侧起笔）
            templates["CIRCLE"]!!.add(
                normalizePath(generateCirclePath(
                    cx = 0.5f, cy = 0.5f, radius = 0.4f,
                    startAngle = 0f, sweepAngle = 360f, clockwise = true
                ))
            )
            // 模板 2：圆（逆时针，从顶部起笔）
            templates["CIRCLE"]!!.add(
                normalizePath(generateCirclePath(
                    cx = 0.5f, cy = 0.5f, radius = 0.4f,
                    startAngle = -90f, sweepAngle = 360f, clockwise = false
                ))
            )

            // ─── 三角形：等边三角形 ───
            templates["TRIANGLE"] = mutableListOf()
            // 模板 1：顶点朝上，顺时针
            templates["TRIANGLE"]!!.add(
                normalizePath(generateTrianglePath(
                    apex = PointF(0.5f, 0.1f),
                    bottomLeft = PointF(0.1f, 0.9f),
                    bottomRight = PointF(0.9f, 0.9f)
                ))
            )
            // 模板 2：顶点朝下
            templates["TRIANGLE"]!!.add(
                normalizePath(generateTrianglePath(
                    apex = PointF(0.5f, 0.9f),
                    bottomLeft = PointF(0.1f, 0.1f),
                    bottomRight = PointF(0.9f, 0.1f)
                ))
            )

            // ─── 方形 ───
            templates["SQUARE"] = mutableListOf()
            // 模板 1：从左上角顺时针
            templates["SQUARE"]!!.add(
                normalizePath(generateSquarePath(
                    topLeft = PointF(0.1f, 0.1f),
                    topRight = PointF(0.9f, 0.1f),
                    bottomRight = PointF(0.9f, 0.9f),
                    bottomLeft = PointF(0.1f, 0.9f)
                ))
            )
            // 模板 2：从左上角逆时针
            templates["SQUARE"]!!.add(
                normalizePath(generateSquarePath(
                    topLeft = PointF(0.1f, 0.1f),
                    topRight = PointF(0.9f, 0.1f),
                    bottomRight = PointF(0.9f, 0.9f),
                    bottomLeft = PointF(0.1f, 0.9f),
                    clockwise = false
                ))
            )

            // ─── V 形 ───
            templates["V_SHAPE"] = mutableListOf()
            // 模板 1：标准 V
            templates["V_SHAPE"]!!.add(
                normalizePath(generatePolylinePath(listOf(
                    PointF(0.1f, 0.1f),
                    PointF(0.5f, 0.9f),
                    PointF(0.9f, 0.1f)
                )))
            )
            // 模板 2：宽 V
            templates["V_SHAPE"]!!.add(
                normalizePath(generatePolylinePath(listOf(
                    PointF(0.05f, 0.15f),
                    PointF(0.5f, 0.85f),
                    PointF(0.95f, 0.15f)
                )))
            )

            // ─── 对勾 ✓ ───
            templates["CHECKMARK"] = mutableListOf()
            // 模板 1：标准对勾
            templates["CHECKMARK"]!!.add(
                normalizePath(generatePolylinePath(listOf(
                    PointF(0.1f, 0.5f),
                    PointF(0.4f, 0.8f),
                    PointF(0.9f, 0.2f)
                )))
            )
            // 模板 2：小对勾
            templates["CHECKMARK"]!!.add(
                normalizePath(generatePolylinePath(listOf(
                    PointF(0.2f, 0.45f),
                    PointF(0.45f, 0.75f),
                    PointF(0.85f, 0.25f)
                )))
            )

        } catch (e: Exception) {
            Log.e(TAG, "Template generation FAILED", e)
        }
    }

    /**
     * 生成圆形路径点
     *
     * @param cx, cy 圆心（归一化坐标 0~1）
     * @param radius 半径（归一化坐标）
     * @param startAngle 起始角度（度）
     * @param sweepAngle 扫过角度（度，正值=顺时针）
     * @param clockwise 是否顺时针
     */
    private fun generateCirclePath(
        cx: Float, cy: Float, radius: Float,
        startAngle: Float, sweepAngle: Float, clockwise: Boolean
    ): List<PointF> {
        val points = mutableListOf<PointF>()
        val steps = 128  // 高密度采样，后续 resample 会规范化
        val direction = if (clockwise) 1f else -1f
        for (i in 0 until steps) {
            val t = i.toFloat() / (steps - 1)
            val angle = Math.toRadians((startAngle + direction * sweepAngle * t).toDouble())
            points.add(PointF(
                (cx + radius * cos(angle)).toFloat(),
                (cy + radius * sin(angle)).toFloat()
            ))
        }
        return points
    }

    /**
     * 生成三角形路径点（三个顶点连线）
     */
    private fun generateTrianglePath(
        apex: PointF, bottomLeft: PointF, bottomRight: PointF
    ): List<PointF> {
        return generatePolylinePath(listOf(apex, bottomLeft, bottomRight, apex))
    }

    /**
     * 生成方形路径点（四个角连线）
     */
    private fun generateSquarePath(
        topLeft: PointF, topRight: PointF,
        bottomRight: PointF, bottomLeft: PointF,
        clockwise: Boolean = true
    ): List<PointF> {
        val vertices = if (clockwise) {
            listOf(topLeft, topRight, bottomRight, bottomLeft, topLeft)
        } else {
            listOf(topLeft, bottomLeft, bottomRight, topRight, topLeft)
        }
        return generatePolylinePath(vertices)
    }

    /**
     * 生成折线路径点（关键点之间线性插值）
     *
     * 论文的 $1 算法要求输入为连续路径点，不是关键点。
     * 此函数将关键顶点列表转换为高密度采样路径。
     */
    private fun generatePolylinePath(vertices: List<PointF>): List<PointF> {
        if (vertices.size < 2) return vertices

        val points = mutableListOf<PointF>()
        points.add(vertices.first())

        for (i in 0 until vertices.size - 1) {
            val a = vertices[i]
            val b = vertices[i + 1]
            val segLen = distance(a, b)
            // 插值步长：固定 0.005（归一化坐标），确保足够密度
            val steps = maxOf(1, (segLen / 0.005f).toInt())
            for (s in 1..steps) {
                val t = s.toFloat() / steps
                points.add(PointF(
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t
                ))
            }
        }
        return points
    }
}
