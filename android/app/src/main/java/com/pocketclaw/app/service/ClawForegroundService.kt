package com.pocketclaw.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pocketclaw.app.PocketClawApplication
import com.pocketclaw.app.R

class ClawForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, PocketClawApplication.CHANNEL_AGENT)
            .setSmallIcon(R.drawable.ic_crab)
            .setContentTitle("🦀 PocketClaw")
            .setContentText("Your butler is watching")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
