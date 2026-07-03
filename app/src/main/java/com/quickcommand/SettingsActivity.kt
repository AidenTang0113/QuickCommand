package com.quickcommand

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.quickcommand.databinding.ActivitySettingsBinding
import com.quickcommand.service.GestureOverlayService
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    private var originalAlpha = 0.5f
    private var originalSize = 60
    private var originalColor = 0x4FC3F7
    private var selectedColor = 0x4FC3F7
    private var useCustomIcon = false
    private var customIconPath: String? = null

    private val colorPresets = listOf(
        0x4FC3F7 to "天蓝",
        0x66BB6A to "翠绿",
        0xFF7043 to "珊瑚橙",
        0xAB47BC to "紫罗兰",
        0xEF5350 to "中国红",
        0xFFCA28 to "琥珀金",
        0x26C6DA to "青碧",
        0xEC407A to "玫瑰粉",
        0x7E57C2 to "靛蓝",
        0x9E9E9E to "石墨灰",
        0x42A5F5 to "宝蓝",
        0x26A69A to "松石绿"
    )

    private data class SliderRow(
        val container: LinearLayout,
        val seekBar: SeekBar,
        val valueText: TextView
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    // 将图片复制到应用内部存储
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val destFile = File(filesDir, "custom_icon.png")
                    val outputStream = FileOutputStream(destFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    customIconPath = destFile.absolutePath
                    useCustomIcon = true
                    updateIconButtons()
                    updatePreview()
                    Toast.makeText(this, "图标已选择", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("Settings", "Failed to copy icon", e)
                    Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("quickcommand_settings", MODE_PRIVATE)

        originalAlpha = prefs.getFloat("overlay_alpha", 0.5f)
        originalSize = prefs.getInt("overlay_size", 60)
        originalColor = prefs.getInt("overlay_color", 0x4FC3F7)
        selectedColor = originalColor
        useCustomIcon = prefs.getBoolean("use_custom_icon", false)
        customIconPath = prefs.getString("custom_icon_path", null)

        setupSliders()
        setupColorPicker()
        setupIconButtons()
        setupButtons()
        setupBottomNav()
        updatePreview()
        updateColorRgbText()
        updateIconButtons()
    }

    private fun setupSliders() {
        val alphaProgress = (originalAlpha * 100).toInt()
        binding.seekAlpha.progress = alphaProgress
        binding.tvAlphaValue.text = "当前：$alphaProgress%"
        binding.seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvAlphaValue.text = "当前：$progress%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekSize.progress = originalSize - 40
        binding.tvSizeValue.text = "当前：$originalSize dp"
        binding.seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 40
                binding.tvSizeValue.text = "当前：$size dp"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupColorPicker() {
        val container = binding.colorContainer
        container.removeAllViews()

        val density = resources.displayMetrics.density
        val sizePx = (36 * density).toInt()
        val marginPx = (6 * density).toInt()

        colorPresets.forEach { (color, name) ->
            val colorView = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, marginPx, marginPx, marginPx)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFF000000.toInt() or color)
                }
                isClickable = true
                isFocusable = true
                contentDescription = name
            }

            if (color == selectedColor) {
                (colorView.background as GradientDrawable).apply {
                    setStroke((3 * density).toInt(), 0xFF333333.toInt())
                }
            }

            colorView.setOnClickListener {
                selectedColor = color
                refreshColorSelection()
                updatePreview()
                updateColorRgbText()
            }

            container.addView(colorView)
        }

        val customBtn = View(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    0xFFFF5252.toInt(), 0xFFFFEB3B.toInt(), 0xFF69F0AE.toInt(),
                    0xFF40C4FF.toInt(), 0xFFE040FB.toInt(), 0xFFFF5252.toInt()
                )
                orientation = GradientDrawable.Orientation.TL_BR
            }
            isClickable = true
            isFocusable = true
            contentDescription = "自定义颜色"
        }

        if (colorPresets.none { it.first == selectedColor }) {
            (customBtn.background as GradientDrawable).apply {
                setStroke((3 * density).toInt(), 0xFF333333.toInt())
            }
        }

        customBtn.setOnClickListener {
            showCustomColorDialog()
        }

        container.addView(customBtn)
    }

    private fun refreshColorSelection() {
        val container = binding.colorContainer
        val density = resources.displayMetrics.density

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val drawable = child.background as? GradientDrawable ?: continue

            if (i < colorPresets.size) {
                val childColor = colorPresets[i].first
                if (childColor == selectedColor) {
                    drawable.setStroke((3 * density).toInt(), 0xFF333333.toInt())
                } else {
                    drawable.setStroke(0, 0)
                }
            } else {
                if (colorPresets.none { it.first == selectedColor }) {
                    drawable.setStroke((3 * density).toInt(), 0xFF333333.toInt())
                } else {
                    drawable.setStroke(0, 0)
                }
            }
        }
    }

    private fun makeColorSliderRow(
        ctx: android.content.Context,
        label: String,
        initial: Int,
        density: Float
    ): SliderRow {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }

        val labelTv = TextView(ctx).apply {
            text = label
            textSize = 15f
            setTextColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(
                (24 * density).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val seek = SeekBar(ctx).apply {
            max = 255
            progress = initial
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                marginStart = (8 * density).toInt()
                marginEnd = (8 * density).toInt()
            }
        }

        val valTv = TextView(ctx).apply {
            text = initial.toString()
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            minWidth = (36 * density).toInt()
            gravity = Gravity.CENTER
        }

        row.addView(labelTv)
        row.addView(seek)
        row.addView(valTv)

        return SliderRow(row, seek, valTv)
    }

    private fun showCustomColorDialog() {
        val density = resources.displayMetrics.density
        val ctx = this

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (24 * density).toInt(), (20 * density).toInt(),
                (24 * density).toInt(), (12 * density).toInt()
            )
        }

        val currentR = (selectedColor shr 16) and 0xFF
        val currentG = (selectedColor shr 8) and 0xFF
        val currentB = selectedColor and 0xFF

        val previewCircle = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((56 * density).toInt(), (56 * density).toInt()).apply {
                gravity = Gravity.CENTER
                bottomMargin = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF000000.toInt() or selectedColor)
            }
        }

        val rgbText = TextView(ctx).apply {
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            text = "RGB: $currentR, $currentG, $currentB"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (20 * density).toInt() }
        }

        val rSlider = makeColorSliderRow(ctx, "R", currentR, density)
        val gSlider = makeColorSliderRow(ctx, "G", currentG, density)
        val bSlider = makeColorSliderRow(ctx, "B", currentB, density)

        val updateAction: () -> Unit = {
            val r = rSlider.seekBar.progress
            val g = gSlider.seekBar.progress
            val b = bSlider.seekBar.progress
            val color = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            (previewCircle.background as GradientDrawable).setColor(color)
            rgbText.text = "RGB: $r, $g, $b"
        }

        rSlider.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rSlider.valueText.text = progress.toString()
                updateAction()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        gSlider.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gSlider.valueText.text = progress.toString()
                updateAction()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        bSlider.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                bSlider.valueText.text = progress.toString()
                updateAction()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.addView(previewCircle)
        layout.addView(rgbText)
        layout.addView(rSlider.container)
        layout.addView(gSlider.container)
        layout.addView(bSlider.container)

        AlertDialog.Builder(ctx)
            .setTitle("自定义颜色")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val r = rSlider.seekBar.progress
                val g = gSlider.seekBar.progress
                val b = bSlider.seekBar.progress
                selectedColor = (r shl 16) or (g shl 8) or b
                refreshColorSelection()
                updatePreview()
                updateColorRgbText()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ==================== 图标选择 ====================

    private fun setupIconButtons() {
        binding.btnIconDefault.setOnClickListener {
            useCustomIcon = false
            updateIconButtons()
            updatePreview()
        }

        binding.btnIconCustom.setOnClickListener {
            // 先检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                        REQ_READ_IMAGES
                    )
                    return@setOnClickListener
                }
            } else {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQ_READ_IMAGES
                    )
                    return@setOnClickListener
                }
            }

            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpeg", "image/jpg", "image/webp"))
        }
        pickImageLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_READ_IMAGES) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateIconButtons() {
        if (useCustomIcon && customIconPath != null && File(customIconPath!!).exists()) {
            binding.btnIconDefault.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
            binding.btnIconDefault.setTextColor(0xFF333333.toInt())
            binding.btnIconCustom.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.primary))
            binding.btnIconCustom.setTextColor(0xFFFFFFFF.toInt())
            binding.tvIconStatus.text = "当前：自定义图片"
        } else {
            binding.btnIconDefault.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.primary))
            binding.btnIconDefault.setTextColor(0xFFFFFFFF.toInt())
            binding.btnIconCustom.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
            binding.btnIconCustom.setTextColor(0xFF333333.toInt())
            binding.tvIconStatus.text = "当前：默认图标"
            if (!useCustomIcon) {
                useCustomIcon = false
            }
        }
    }

    private fun updateColorRgbText() {
        val r = (selectedColor shr 16) and 0xFF
        val g = (selectedColor shr 8) and 0xFF
        val b = selectedColor and 0xFF
        binding.tvColorRgb.text = "RGB: $r, $g, $b"
    }

    private fun updatePreview() {
        val alpha = binding.seekAlpha.progress / 100f
        val sizeDp = binding.seekSize.progress + 40
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()

        val alphaInt = (255 * alpha).toInt().coerceIn(0, 255)
        val colorWithAlpha = (alphaInt shl 24) or selectedColor

        // 设置背景颜色
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorWithAlpha)
        }
        binding.previewBallIcon.layoutParams = binding.previewBallIcon.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        binding.previewBallIcon.background = drawable

        // 设置图标
        if (useCustomIcon && customIconPath != null && File(customIconPath!!).exists()) {
            val bitmap = BitmapFactory.decodeFile(customIconPath)
            if (bitmap != null) {
                binding.previewBallIcon.setImageBitmap(bitmap)
                binding.previewBallIcon.setPadding(0, 0, 0, 0)
            } else {
                binding.previewBallIcon.setImageResource(android.R.drawable.ic_menu_compass)
                binding.previewBallIcon.setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt()
                )
            }
        } else {
            binding.previewBallIcon.setImageResource(android.R.drawable.ic_menu_compass)
            binding.previewBallIcon.setPadding(
                (12 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val alpha = binding.seekAlpha.progress / 100f
            val size = binding.seekSize.progress + 40

            prefs.edit()
                .putFloat("overlay_alpha", alpha)
                .putInt("overlay_size", size)
                .putInt("overlay_color", selectedColor)
                .putBoolean("use_custom_icon", useCustomIcon)
                .putString("custom_icon_path", customIconPath)
                .apply()

            if (isServiceRunning()) {
                GestureOverlayService.stop(this)
                GestureOverlayService.start(this)
            }

            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_settings
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    true
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == GestureOverlayService::class.java.name }
    }

    companion object {
        private const val REQ_READ_IMAGES = 1001
    }
}
