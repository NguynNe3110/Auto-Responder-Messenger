package com.autoreply.messenger

import android.app.Notification
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.RemoteInput

class AutoReplyService : NotificationListenerService() {

    companion object {
        private const val TAG = "AutoReply"
        // Gói ứng dụng Messenger
        private val MESSENGER_PACKAGES = setOf(
            "com.facebook.orca",      // Messenger
            "com.facebook.mlite",     // Messenger Lite
            "com.facebook.katana"     // Facebook (tin nhắn tích hợp)
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private val lastReplyTime = HashMap<String, Long>()
    private lateinit var settings: SettingsManager
    private lateinit var database: ReplyDatabase

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        database = ReplyDatabase(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Chỉ xử lý thông báo từ Messenger
        if (sbn.packageName !in MESSENGER_PACKAGES) return

        // Kiểm tra xem tính năng đã bật chưa
        if (!settings.isEnabled()) return

        // Lấy thông tin từ notification
        val extras = sbn.notification.extras ?: return

        // Lấy tên người gửi
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
        if (sender.isBlank()) return

        // Không phản hồi tin nhắn của chính mình hoặc thông báo hệ thống
        if (sender.equals("Bạn", ignoreCase = true)) return
        if (sender.equals("Messenger", ignoreCase = true)) return

        // Lấy nội dung tin nhắn
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Kiểm tra tin nhắn nhóm
        if (!settings.isGroupReplyEnabled()) {
            // Nếu notification có tiêu đề nhóm, bỏ qua
            val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            if (isGroup) return
        }

        // Kiểm tra lọc liên hệ
        if (!isContactAllowed(sender)) return

        // Kiểm tra rate limit
        val rateLimit = settings.getRateLimitMs()
        val lastTime = lastReplyTime[sender] ?: 0
        if (System.currentTimeMillis() - lastTime < rateLimit) return

        // Ghi nhận thời gian phản hồi
        lastReplyTime[sender] = System.currentTimeMillis()

        // Lấy tin nhắn phản hồi
        val replyMsg = settings.getReplyMessage()
        val delay = settings.getDelayMs()

        // Delay rồi gửi phản hồi
        handler.postDelayed({
            val success = sendReply(sbn, replyMsg)
            if (success) {
                // Lưu lịch sử
                database.insertHistory(sender, message, replyMsg)
                Log.d(TAG, "Đã phản hồi $sender: $replyMsg")
            } else {
                Log.w(TAG, "Không thể gửi phản hồi cho $sender")
            }
        }, delay)
    }

    // Kiểm tra xem liên hệ có được phép gửi tin không
    private fun isContactAllowed(sender: String): Boolean {
        val mode = settings.getFilterMode()
        val contacts = settings.getContactList()

        return when (mode) {
            "whitelist" -> contacts.contains(sender)
            "blacklist" -> !contacts.contains(sender)
            else -> true // "all"
        }
    }

    // Gửi phản hồi qua action Reply của notification
    private fun sendReply(sbn: StatusBarNotification, replyText: String): Boolean {
        val notification = sbn.notification
        val actions = notification.actions ?: return false

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            if (remoteInputs.isEmpty()) continue

            try {
                val intent = Intent()
                val bundle = android.os.Bundle()
                for (input in remoteInputs) {
                    bundle.putCharSequence(input.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                action.actionIntent.send(this, 0, intent)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi gửi phản hồi: ${e.message}")
            }
        }
        return false
    }
}