package com.crabagent.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.crabagent.app.service.CrabForegroundService

/**
 * Restarts CrabAgent after device reboot.
 * The butler should always be on duty.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, CrabForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
