package com.crabagent.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.crabagent.app.cloud.CloudClient
import com.crabagent.app.cloud.DeviceEvent
import com.crabagent.app.cloud.EventSource
import com.crabagent.app.cloud.EventType
import com.crabagent.app.cloud.DeviceContext
import com.crabagent.app.cloud.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * The eyes and ears of CrabAgent.
 *
 * Reads every notification that arrives on the device,
 * packages it as a DeviceEvent, and sends it to the Cloud Brain.
 */
class CrabNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cloudClient = CloudClient()

    // Apps we actually care about triaging
    private val watchedApps = setOf(
        "com.tencent.mm",           // WeChat
        "com.tencent.mobileqq",     // QQ
        "com.alibaba.android.rimet", // DingTalk
        "org.telegram.messenger",   // Telegram
        "com.whatsapp",             // WhatsApp
        "com.slack",                // Slack
        "com.google.android.gm",   // Gmail
        "com.microsoft.office.outlook", // Outlook
    )

    // System noise we always skip
    private val ignoredApps = setOf(
        "com.android.systemui",
        "com.android.vending",
        "com.google.android.gms",
        "android",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        if (pkg in ignoredApps) return
        if (pkg == applicationContext.packageName) return // don't listen to ourselves

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        // For MVP: only process watched apps. Log everything else.
        if (pkg !in watchedApps) {
            Log.d(TAG, "Skipped: $pkg | $title | ${text.take(50)}")
            return
        }

        Log.i(TAG, "Captured: $pkg | $title | ${text.take(80)}")

        val event = DeviceEvent(
            type = EventType.NOTIFICATION,
            timestamp = Instant.now().toString(),
            source = EventSource(
                app = pkg,
                title = title,
                text = text,
                sender = null // TODO: parse sender from notification template
            ),
            device = DeviceContext(
                battery = getBatteryLevel(),
                network = getNetworkType()
            )
        )

        scope.launch {
            try {
                val response = cloudClient.sendEvent(event)
                ActionExecutor.execute(applicationContext, response)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process event: ${e.message}")
                // TODO: queue for retry when network recovers
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Could track dismissed notifications for context
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getNetworkType(): NetworkType {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return NetworkType.OFFLINE
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkType.OFFLINE
        return when {
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.OFFLINE
        }
    }

    companion object {
        private const val TAG = "CrabNotification"
    }
}
