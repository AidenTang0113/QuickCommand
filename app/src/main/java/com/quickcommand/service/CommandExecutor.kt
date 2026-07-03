package com.quickcommand.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.quickcommand.model.ActionType
import com.quickcommand.model.Command
import java.util.*

/**
 * 命令执行器 — 根据命令的动作类型执行对应的操作
 */
object CommandExecutor {

    fun execute(context: Context, command: Command) {
        try {
            when (command.actionType) {
                ActionType.OPEN_APP -> openApp(context, command.actionParam)
                ActionType.SET_REMINDER -> setReminder(context, command)
                ActionType.TOGGLE_WIFI -> toggleWifi(context)
                ActionType.TOGGLE_BLUETOOTH -> toggleBluetooth(context)
                ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight(context)
                ActionType.OPEN_WEBSITE -> openWebsite(context, command.actionParam)
                ActionType.TAKE_SCREENSHOT -> takeScreenshot(context)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "执行失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openApp(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) {
            Toast.makeText(context, "未指定应用", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "无法启动应用: $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setReminder(context: Context, command: Command) {
        val param = command.actionParam ?: "3000" // 默认 3 秒
        val delayMs = param.toLongOrNull() ?: 3000L

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("command_name", command.name)
            putExtra("command_id", command.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, command.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + delayMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        val seconds = delayMs / 1000
        Toast.makeText(context, "提醒将在 ${seconds}秒 后触发", Toast.LENGTH_SHORT).show()
    }

    private fun toggleWifi(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不能直接开关 WiFi，跳转到设置
            val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(panelIntent)
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
            val status = if (wifiManager.isWifiEnabled) "已开启" else "已关闭"
            Toast.makeText(context, "WiFi $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleBluetooth(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter != null) {
            if (adapter.isEnabled) {
                adapter.disable()
                Toast.makeText(context, "蓝牙已关闭", Toast.LENGTH_SHORT).show()
            } else {
                adapter.enable()
                Toast.makeText(context, "蓝牙已开启", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "设备不支持蓝牙", Toast.LENGTH_SHORT).show()
        }
    }

    private var flashlightOn = false

    private fun toggleFlashlight(context: Context) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            flashlightOn = !flashlightOn
            cameraManager.setTorchMode(cameraId, flashlightOn)
            val status = if (flashlightOn) "已开启" else "已关闭"
            Toast.makeText(context, "手电筒 $status", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "无法控制手电筒: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebsite(context: Context, url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "未指定网址", Toast.LENGTH_SHORT).show()
            return
        }
        val finalUrl = if (url.startsWith("http")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun takeScreenshot(context: Context) {
        // Android 没有直接截屏的公开 API
        // 使用 MediaProjection 需要用户授权，这里提供快捷入口
        Toast.makeText(context, "请使用系统截屏快捷键 (电源+音量减)", Toast.LENGTH_LONG).show()
    }
}
