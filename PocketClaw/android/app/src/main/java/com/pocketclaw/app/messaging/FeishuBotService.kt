package com.pocketclaw.app.messaging

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Feishu/Lark Bot integration using Webhook URL.
 * Sends messages via incoming webhook — simplest integration path.
 */
class FeishuBotService : MessageChannel {

    companion object {
        private const val TAG = "FeishuBot"
    }

    override val platformId = "feishu"
    override val displayName = "Feishu"
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
            val body = JSONObject().apply {
                put("msg_type", "text")
                put("content", JSONObject().put("text", text))
            }.toString()
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
