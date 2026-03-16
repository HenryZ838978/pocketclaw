package com.pocketclaw.app.messaging

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Slack Bot integration using Incoming Webhook URL.
 */
class SlackBotService : MessageChannel {

    companion object {
        private const val TAG = "SlackBot"
    }

    override val platformId = "slack"
    override val displayName = "Slack"
    override var isConnected = false
        private set

    private var webhookUrl: String = ""
    private var incomingHandler: (suspend (IncomingMessage) -> String?)? = null

    override suspend fun connect(config: Map<String, String>): Boolean {
        webhookUrl = config["webhook_url"] ?: return false
        isConnected = webhookUrl.isNotBlank()
        return isConnected
    }

    override suspend fun disconnect() {
        isConnected = false
    }

    override suspend fun sendMessage(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(webhookUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().put("text", text).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val ok = conn.responseCode == 200
            conn.disconnect()
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Send failed: ${e.message}")
            false
        }
    }

    override fun setIncomingHandler(handler: suspend (IncomingMessage) -> String?) {
        incomingHandler = handler
    }
}
