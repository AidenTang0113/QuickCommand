package com.quickcommand.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.quickcommand.GestureCaptureActivity
import com.quickcommand.R
import java.io.File

/**
 * 手势悬浮窗服务 — 在屏幕上显示一个小型触发按钮
 * 点击展开手势识别区域，识别后执行对应命令
 */
class GestureOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private var triggerView: ImageView? = null
    private var gestureOverlay: ViewGroup? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("quickcommand_settings", Context.MODE_PRIVATE)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showTrigger()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "gesture_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "手势检测服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "快捷命令手势检测正在运行"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("快捷命令")
            .setContentText("手势检测已就绪")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun showTrigger() {
        if (triggerView != null) return

        val triggerSize = prefs.getInt("overlay_size", 60).dpToPx()
        val alpha = prefs.getFloat("overlay_alpha", 0.5f)
        val color = prefs.getInt("overlay_color", 0x4FC3F7)
        val useCustomIcon = prefs.getBoolean("use_custom_icon", false)
        val customIconPath = prefs.getString("custom_icon_path", null)

        triggerView = ImageView(this).apply {
            // 加载图标：自定义图片 or 默认图标
            if (useCustomIcon && customIconPath != null && File(customIconPath).exists()) {
                val bitmap = BitmapFactory.decodeFile(customIconPath)
                if (bitmap != null) {
                    setImageBitmap(bitmap)
                    setPadding(0, 0, 0, 0)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    setImageResource(android.R.drawable.ic_menu_compass)
                    setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                }
            } else {
                setImageResource(android.R.drawable.ic_menu_compass)
                setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
            }

            setBackgroundColor(applyAlpha(color, alpha))
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }

        val params = WindowManager.LayoutParams(
            triggerSize,
            triggerSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 400
        }

        // 拖拽触发器
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        triggerView!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                    }
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(triggerView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        vibrate()
                        val intent = Intent(this@GestureOverlayService, GestureCaptureActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(triggerView, params)
    }

    private fun applyAlpha(rgb: Int, alpha: Float): Int {
        val a = (255 * alpha).toInt().coerceIn(0, 255)
        return (a shl 24) or rgb
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        triggerView?.let { windowManager.removeView(it) }
        gestureOverlay?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, GestureOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GestureOverlayService::class.java))
        }
    }
}
