package com.quickcommand.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.quickcommand.MainActivity
import com.quickcommand.R

/**
 * 提醒广播接收器 — 在设定的延迟后弹出通知
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val commandName = intent.getStringExtra("command_name") ?: "快捷命令"
        val commandId = intent.getLongExtra("command_id", 0)

        val channelId = "reminders"
        val manager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "提醒通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "快捷命令提醒"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("command_id", commandId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, commandId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ 提醒")
            .setContentText(commandName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(commandId.toInt() + 10000, notification)
    }
}
