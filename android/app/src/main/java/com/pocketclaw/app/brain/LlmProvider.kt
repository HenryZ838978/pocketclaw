package com.pocketclaw.app.brain

interface LlmProvider {
    val name: String
    val isReady: Boolean
    suspend fun generate(systemPrompt: String, userMessage: String): LlmResult
}

data class LlmResult(
    val content: String,
    val tokensUsed: Int = 0,
)
