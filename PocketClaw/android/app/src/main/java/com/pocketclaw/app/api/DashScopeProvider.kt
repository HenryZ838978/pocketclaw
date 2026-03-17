package com.pocketclaw.app.api

import android.util.Log
import com.pocketclaw.app.data.Preferences
import com.pocketclaw.claw.prompt.PromptAssembler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * DashScope (Alibaba Cloud) API provider for cloud-based Qwen inference.
 * Uses the OpenAI-compatible endpoint for streaming chat completions.
 */
object DashScopeProvider {

    private const val TAG = "DashScope"
    private const val STANDARD_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    private const val CODING_URL = "https://coding.dashscope.aliyuncs.com/v1/chat/completions"
    private const val DEFAULT_MODEL = "MiniMax-M2.5"

    private fun baseUrl(apiKey: String): String =
        if (apiKey.startsWith("sk-sp-")) CODING_URL else STANDARD_URL

    val isReady: Boolean
        get() = Preferences.dashScopeApiKey.isNotBlank()

    fun generateStream(assembled: PromptAssembler.AssembledPrompt): Flow<String> = flow {
        val apiKey = Preferences.dashScopeApiKey
        if (apiKey.isBlank()) {
            emit("Error: API key not set. Go to Settings > AI Brain > API Key.")
            return@flow
        }

        val messages = JSONArray()

        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", assembled.systemPrompt)
        })

        for (turn in assembled.chatHistory) {
            messages.put(JSONObject().apply {
                put("role", turn.role)
                put("content", turn.content)
            })
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", assembled.userMessage)
        })

        val body = JSONObject().apply {
            put("model", DEFAULT_MODEL)
            put("messages", messages)
            put("stream", true)
            put("temperature", 0.7)
            put("max_tokens", 2048)
        }

        Log.d(TAG, "Request: model=$DEFAULT_MODEL, messages=${messages.length()}, system=${assembled.systemPrompt.take(80)}...")

        val url = baseUrl(apiKey)
        Log.d(TAG, "Using endpoint: $url (key prefix: ${apiKey.take(6)}...)")

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("User-Agent", "openclaw/2026.3.1")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }

        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                Log.e(TAG, "API error: $error")
                emit("API error ($responseCode): ${extractErrorMessage(error)}")
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data: ")) continue
                val data = l.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                try {
                    val chunk = JSONObject(data)
                    val choices = chunk.optJSONArray("choices") ?: continue
                    if (choices.length() == 0) continue
                    val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                    val content = delta.optString("content", "")
                    if (content.isNotEmpty()) {
                        emit(content)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse SSE chunk: $data")
                }
            }

            reader.close()
            Log.d(TAG, "Stream completed")

        } catch (e: Exception) {
            Log.e(TAG, "Stream failed: ${e.message}", e)
            emit("\n\nConnection error: ${e.message}")
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractErrorMessage(raw: String): String {
        return try {
            val obj = JSONObject(raw)
            obj.optJSONObject("error")?.optString("message") ?: raw.take(200)
        } catch (_: Exception) {
            raw.take(200)
        }
    }
}
