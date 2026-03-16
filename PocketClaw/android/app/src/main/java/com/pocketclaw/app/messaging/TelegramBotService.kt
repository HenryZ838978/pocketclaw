package com.pocketclaw.app.messaging

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Telegram Bot API integration using long-polling (getUpdates).
 * No webhook needed — runs entirely from the phone.
 */
class TelegramBotService : MessageChannel {

    companion object {
        private const val TAG = "TelegramBot"
        private const val BASE = "https://api.telegram.org/bot"
    }

    override val platformId = "telegram"
    override val displayName = "Telegram"
    override var isConnected = false
        private set

    private var token: String = ""
    private var pollingJob: Job? = null
    private var lastUpdateId: Long = 0
    private var incomingHandler: (suspend (IncomingMessage) -> String?)? = null

    override suspend fun connect(config: Map<String, String>): Boolean {
        token = config["token"] ?: return false
        if (token.isBlank()) return false

        return withContext(Dispatchers.IO) {
            try {
                val resp = httpGet("$BASE$token/getMe")
                val ok = resp?.optBoolean("ok") == true
                if (ok) {
                    isConnected = true
                    startPolling()
                    val botName = resp?.optJSONObject("result")?.optString("username")
                    Log.d(TAG, "Connected as @$botName")
                }
                ok
            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${e.message}")
                false
            }
        }
    }

    override suspend fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        isConnected = false
    }

    override suspend fun sendMessage(text: String): Boolean {
        return false
    }

    /**
     * Send a message to a specific chat (reply to incoming messages).
     */
    suspend fun sendTo(chatId: Long, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE$token/sendMessage?chat_id=$chatId&text=${java.net.URLEncoder.encode(text, "UTF-8")}"
            val resp = httpGet(url)
            resp?.optBoolean("ok") == true
        } catch (e: Exception) {
            Log.e(TAG, "Send failed: ${e.message}")
            false
        }
    }

    override fun setIncomingHandler(handler: suspend (IncomingMessage) -> String?) {
        incomingHandler = handler
    }

    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive && isConnected) {
                try {
                    val url = "$BASE$token/getUpdates?offset=${lastUpdateId + 1}&timeout=30"
                    val resp = httpGet(url)
                    val results = resp?.optJSONArray("result") ?: continue

                    for (i in 0 until results.length()) {
                        val update = results.getJSONObject(i)
                        lastUpdateId = update.getLong("update_id")
                        val message = update.optJSONObject("message") ?: continue
                        val text = message.optString("text", "")
                        val chatId = message.optJSONObject("chat")?.optLong("id") ?: continue
                        val from = message.optJSONObject("from")
                        val senderName = from?.optString("first_name", "User") ?: "User"

                        if (text.isNotBlank()) {
                            val incoming = IncomingMessage(
                                platform = "telegram",
                                senderId = chatId.toString(),
                                senderName = senderName,
                                text = text,
                            )
                            val reply = incomingHandler?.invoke(incoming)
                            if (!reply.isNullOrBlank()) {
                                sendTo(chatId, reply)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Polling error: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    private fun httpGet(urlStr: String): JSONObject? {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 35_000
        conn.readTimeout = 35_000
        return try {
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                JSONObject(body)
            } else null
        } finally {
            conn.disconnect()
        }
    }
}
