package com.autoreply.messenger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settings = SettingsManager(context)
            if (settings.isEnabled()) {
                // Khởi động lại foreground service
                val serviceIntent = Intent(context, KeepAliveService::class.java).apply {
                    action = KeepAliveService.ACTION_ENABLE
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}