package com.quickcommand

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quickcommand.adapter.CommandAdapter
import com.quickcommand.databinding.ActivityMainBinding
import com.quickcommand.service.GestureOverlayService
import com.quickcommand.viewmodel.CommandViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: CommandViewModel
    private lateinit var adapter: CommandAdapter
    private lateinit var prefs: SharedPreferences

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            GestureOverlayService.start(this)
            updateOverlayStatus()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(CommandViewModel::class.java)

        prefs = getSharedPreferences("quickcommand_settings", MODE_PRIVATE)

        setupRecyclerView()
        setupListeners()
        setupBottomNav()
        observeCommands()
        requestPermissions()
    }

    private fun setupRecyclerView() {
        adapter = CommandAdapter(
            onToggle = { cmd, enabled ->
                viewModel.setEnabled(cmd.id, enabled)
            },
            onEdit = { cmd ->
                val intent = Intent(this, AddEditCommandActivity::class.java)
                intent.putExtra("command_id", cmd.id)
                startActivity(intent)
            },
            onDelete = { cmd ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("删除命令")
                    .setMessage("确定删除「${cmd.name}」吗？")
                    .setPositiveButton("删除") { _, _ -> viewModel.delete(cmd) }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        binding.rvCommands.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEditCommandActivity::class.java))
        }

        binding.btnToggleOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                if (isServiceRunning()) {
                    GestureOverlayService.stop(this)
                } else {
                    GestureOverlayService.start(this)
                }
                updateOverlayStatus()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 已在主页，不跳转
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
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

    private fun observeCommands() {
        lifecycleScope.launch {
            viewModel.allCommands.collect { commands ->
                adapter.submitList(commands)
                binding.tvEmpty.visibility =
                    if (commands.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun requestPermissions() {
        // 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // 悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请授予悬浮窗权限以启用手势检测", Toast.LENGTH_LONG).show()
        } else {
            updateOverlayStatus()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun updateOverlayStatus() {
        val running = isServiceRunning()
        binding.btnToggleOverlay.text = if (running) "关闭悬浮球" else "开启悬浮球"
        binding.tvOverlayStatus.text = if (running) "● 手势检测运行中" else "○ 手势检测已停止"
        binding.tvOverlayStatus.setTextColor(
            if (running) 0xFF4CAF50.toInt() else 0xFF757575.toInt()
        )
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == GestureOverlayService::class.java.name }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayStatus()
        // 确保从设置页返回时底部导航高亮"命令"
        binding.bottomNav.menu.findItem(R.id.nav_home).isChecked = true
    }
}
