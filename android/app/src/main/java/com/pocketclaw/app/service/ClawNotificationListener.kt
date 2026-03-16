package com.pocketclaw.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pocketclaw.app.PocketClawApplication
import com.pocketclaw.app.brain.BrainEngine
import com.pocketclaw.app.brain.DashScopeProvider
import com.pocketclaw.app.brain.LocalLlmProvider
import com.pocketclaw.app.data.Preferences
import com.pocketclaw.app.voice.LlamaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClawNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var brain: BrainEngine? = null

    private val watchedApps = setOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.alibaba.android.rimet",
        "org.telegram.messenger",
        "com.whatsapp",
        "com.slack",
        "com.google.android.gm",
        "com.microsoft.office.outlook",
    )

    private val ignoredApps = setOf(
        "com.android.systemui",
        "com.android.vending",
        "com.google.android.gms",
        "android",
    )

    override fun onCreate() {
        super.onCreate()
        val llamaEngine = LlamaEngine(applicationContext)
        val provider = if (Preferences.llmMode == "api") {
            DashScopeProvider()
        } else {
            LocalLlmProvider(llamaEngine)
        }
        brain = BrainEngine(provider)

        scope.launch {
            if (llamaEngine.isModelDownloaded) {
                llamaEngine.loadModel()
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        if (pkg in ignoredApps) return
        if (pkg == applicationContext.packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        if (pkg !in watchedApps) {
            if (text.length < 5) {
                Log.d(TAG, "Skipped: $pkg | $title | ${text.take(50)}")
                return
            }
            Log.i(TAG, "Non-watched app, processing: $pkg | $title")
        }

        Log.i(TAG, "Captured: $pkg | $title | ${text.take(80)}")

        scope.launch {
            try {
                val memoryDao = (applicationContext as PocketClawApplication).database.memoryDao()
                val memories = memoryDao.getAll()
                val response = brain?.processNotification(pkg, title, text, memories) ?: return@launch
                ActionExecutor.execute(applicationContext, response)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    companion object {
        private const val TAG = "ClawNotification"
    }
}
