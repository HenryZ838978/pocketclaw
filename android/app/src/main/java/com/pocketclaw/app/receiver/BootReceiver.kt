package com.pocketclaw.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pocketclaw.app.service.ClawForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ClawForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
