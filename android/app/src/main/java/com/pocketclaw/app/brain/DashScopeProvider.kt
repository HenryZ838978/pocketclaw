package com.pocketclaw.app.brain

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.pocketclaw.app.data.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DashScopeProvider : LlmProvider {

    companion object {
        private const val TAG = "DashScope"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json".toMediaType()

    override val name: String = "Cloud (DashScope API)"
    override val isReady: Boolean get() = Preferences.dashScopeApiKey.isNotBlank()

    override suspend fun generate(systemPrompt: String, userMessage: String): LlmResult =
        withContext(Dispatchers.IO) {
            val apiKey = Preferences.dashScopeApiKey
            val baseUrl = Preferences.dashScopeBaseUrl
            val model = Preferences.dashScopeModel

            if (apiKey.isBlank()) {
                return@withContext LlmResult("API key not configured. Go to Settings → Cloud API.")
            }

            val body = ChatRequest(
                model = model,
                messages = listOf(
                    Message("system", systemPrompt),
                    Message("user", userMessage),
                ),
            )

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(gson.toJson(body).toRequestBody(jsonMedia))
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "API error ${response.code}: $responseBody")
                    return@withContext LlmResult("API error: ${response.code}")
                }

                val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
                val tokens = chatResponse.usage?.totalTokens ?: 0

                LlmResult(content = content, tokensUsed = tokens)
            } catch (e: Exception) {
                Log.e(TAG, "Request failed: ${e.message}", e)
                LlmResult("Network error: ${e.message}")
            }
        }

    // OpenAI-compatible request/response types
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
    )

    private data class Message(
        val role: String,
        val content: String,
    )

    private data class ChatResponse(
        val choices: List<Choice>,
        val usage: Usage?,
    )

    private data class Choice(
        val message: Message,
    )

    private data class Usage(
        @SerializedName("total_tokens") val totalTokens: Int,
    )
}
