package com.quickcommand

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.quickcommand.databinding.ActivityGestureCaptureBinding
import com.quickcommand.gesture.GestureMatcher
import com.quickcommand.model.CommandConverters
import com.quickcommand.model.GesturePoint
import com.quickcommand.service.CommandExecutor
import com.quickcommand.viewmodel.CommandViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 手势捕获界面 — 用户在此绘制手势
 * 由悬浮球点击触发，也可用于录制自定义手势
 */
class GestureCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGestureCaptureBinding
    private lateinit var viewModel: CommandViewModel

    private var isForCapture = false // true = 录制模式, false = 识别模式

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestureCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(CommandViewModel::class.java)

        isForCapture = intent.getBooleanExtra("capture_mode", false)

        setupUI()
    }

    private fun setupUI() {
        if (isForCapture) {
            binding.tvTitle.text = "录制自定义手势"
            binding.tvHint.text = "请在手势区域绘制你的自定义手势"
            binding.btnCancel.visibility = View.GONE
        } else {
            binding.tvTitle.text = "绘制手势触发命令"
            binding.tvHint.text = "在下方区域绘制手势"
        }

        binding.gestureDrawView.onGestureComplete = { points ->
            if (points.isNotEmpty()) {
                if (isForCapture) {
                    onGestureCaptured(points)
                } else {
                    onGestureDrawn(points)
                }
            }
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { binding.gestureDrawView.clear() }
    }

    private fun onGestureDrawn(rawPoints: List<GesturePoint>) {
        vibrate()
        Log.d("GestureCapture", "onGestureDrawn called with ${rawPoints.size} points")

        lifecycleScope.launch {
            // 直接从数据库查所有启用的命令
            val allCommands = viewModel.getAllCommandsDirect()
            Log.d("GestureCapture", "All commands from DB: ${allCommands.size}")

            // 收集自定义手势
            val customGestures = mutableMapOf<String, List<GesturePoint>>()
            allCommands
                .filter { it.gestureType.name == "CUSTOM" && it.isEnabled }
                .forEach { cmd ->
                    val pts = CommandConverters.gesturePointsFromJson(cmd.customGesturePoints)
                    if (pts.isNotEmpty()) {
                        customGestures[cmd.id.toString()] = pts
                    }
                }
            Log.d("GestureCapture", "Custom gestures: ${customGestures.size}")

            // 先匹配形状手势（圆圈/三角形/方形/V形/对勾）
            val matchResult = withContext(Dispatchers.Default) {
                try {
                    GestureMatcher.match(rawPoints, customGestures)
                } catch (e: Exception) {
                    Log.e("GestureCapture", "match() EXCEPTION", e)
                    null
                }
            }
            Log.d("GestureCapture", "Shape match result: $matchResult")

            if (matchResult != null && tryExecute(matchResult, allCommands)) {
                finish()
                return@launch
            }

            // 形状匹配失败，再检查滑动手势
            val swipeResult = withContext(Dispatchers.Default) {
                GestureMatcher.detectSwipe(rawPoints)
            }
            Log.d("GestureCapture", "Swipe result: $swipeResult")

            if (swipeResult != null && tryExecute(swipeResult, allCommands)) {
                finish()
                return@launch
            }

            showNoMatch()
        }
    }

    private fun showNoMatch() {
        Toast.makeText(
            this@GestureCaptureActivity,
            "未匹配到任何命令，请重试",
            Toast.LENGTH_SHORT
        ).show()
        binding.gestureDrawView.clear()
    }

    private suspend fun tryExecute(gestureName: String, commands: List<com.quickcommand.model.Command>): Boolean {
        val cmd = commands.find {
            it.gestureType.name == gestureName && it.isEnabled
        }
        Log.d("GestureCapture", "tryExecute: looking for gesture=$gestureName, found=${cmd != null}")
        if (cmd != null) {
            withContext(Dispatchers.Main) {
                CommandExecutor.execute(this@GestureCaptureActivity, cmd)
                Toast.makeText(
                    this@GestureCaptureActivity,
                    "触发: ${cmd.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        }
        return false
    }

    private fun onGestureCaptured(points: List<GesturePoint>) {
        vibrate()
        val json = CommandConverters.gesturePointsToJson(points)
        intent.putExtra("gesture_points_json", json)
        setResult(RESULT_OK, intent)
        Toast.makeText(this, "手势已录制", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun vibrate() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}
