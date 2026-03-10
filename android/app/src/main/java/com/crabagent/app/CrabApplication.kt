package com.crabagent.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class CrabApplication : Application() {

    companion object {
        const val CHANNEL_AGENT = "crab_agent"
        const val CHANNEL_TRIAGE = "crab_triage"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val agentChannel = NotificationChannel(
            CHANNEL_AGENT,
            "CrabAgent Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps CrabAgent running in background"
        }

        val triageChannel = NotificationChannel(
            CHANNEL_TRIAGE,
            "Message Triage",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important messages flagged by your crab butler"
        }

        manager.createNotificationChannel(agentChannel)
        manager.createNotificationChannel(triageChannel)
    }
}
