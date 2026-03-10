package com.crabagent.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.crabagent.app.CrabApplication
import com.crabagent.app.R

/**
 * Foreground service that keeps CrabAgent alive.
 *
 * Android kills background processes aggressively.
 * A foreground notification ("🦀 CrabAgent is watching") keeps us running.
 * The Pixel 8 Pro's stock Android is the most lenient, but we still need this.
 */
class CrabForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CrabApplication.CHANNEL_AGENT)
            .setSmallIcon(R.drawable.ic_crab)
            .setContentTitle("🦀 CrabAgent")
            .setContentText("Your butler is watching")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
