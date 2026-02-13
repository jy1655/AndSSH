package com.opencode.sshterminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.opencode.sshterminal.R

class SshForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channelId = ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification(channelId))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun ensureChannel(): String {
        val channelId = "ssh_session_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    private fun buildNotification(channelId: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_body))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
