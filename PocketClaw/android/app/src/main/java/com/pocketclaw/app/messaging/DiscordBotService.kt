package com.pocketclaw.app.messaging

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Discord Bot integration using REST API polling (simplified, no WebSocket gateway).
 * For a full implementation, use the Discord Gateway WebSocket.
 */
class DiscordBotService : MessageChannel {

    companion object {
        private const val TAG = "DiscordBot"
        private const val BASE = "https://discord.com/api/v10"
    }

    override val platformId = "discord"
    override val displayName = "Discord"
    override var isConnected = false
        private set

    private var token: String = ""
    private var channelId: String = ""
    private var incomingHandler: (suspend (IncomingMessage) -> String?)? = null

    override suspend fun connect(config: Map<String, String>): Boolean {
        token = config["token"] ?: return false
        channelId = config["channel_id"] ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val conn = URL("$BASE/users/@me").openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bot $token")
                val ok = conn.responseCode == 200
                conn.disconnect()
                isConnected = ok
                if (ok) Log.d(TAG, "Connected to Discord")
                ok
            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${e.message}")
                false
            }
        }
    }

    override suspend fun disconnect() {
        isConnected = false
    }

    override suspend fun sendMessage(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE/channels/$channelId/messages").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bot $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().put("content", text).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val ok = conn.responseCode in 200..299
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
