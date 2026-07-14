package com.autoreply.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class KeepAliveService : Service() {

    companion object {
        const val CHANNEL_ID = "autoreply_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_ENABLE = "com.autoreply.messenger.ENABLE"
        const val ACTION_DISABLE = "com.autoreply.messenger.DISABLE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE -> {
                startForegroundNotification()
            }
            ACTION_DISABLE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(NotificationManager::class.java)

        // Tạo notification channel (Android 8+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dịch vụ tự động phản hồi",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Thông báo khi dịch vụ đang chạy"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoReply đang hoạt động")
            .setContentText("Đang theo dõi tin nhắn Messenger")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}