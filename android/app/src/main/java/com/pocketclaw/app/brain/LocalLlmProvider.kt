package com.pocketclaw.app.brain

import com.pocketclaw.app.voice.LlamaEngine

class LocalLlmProvider(private val engine: LlamaEngine) : LlmProvider {

    override val name: String = "Local (Qwen3-1.7B)"
    override val isReady: Boolean get() = engine.isReady

    override suspend fun generate(systemPrompt: String, userMessage: String): LlmResult {
        val prompt = buildChatPrompt(systemPrompt, userMessage)
        val response = engine.generate(prompt, maxTokens = 512, temperature = 0.7f)
        val cleaned = cleanResponse(response)
        return LlmResult(content = cleaned, tokensUsed = 0)
    }

    private fun buildChatPrompt(system: String, user: String): String {
        // Qwen3 chat template
        return buildString {
            append("<|im_start|>system\n")
            append(system)
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(user)
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    private fun cleanResponse(raw: String): String {
        // Strip any trailing special tokens
        return raw
            .replace("<|im_end|>", "")
            .replace("<|im_start|>", "")
            .replace("<|endoftext|>", "")
            .trim()
    }
}
