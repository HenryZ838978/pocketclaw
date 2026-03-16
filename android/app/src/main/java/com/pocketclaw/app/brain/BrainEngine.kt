package com.pocketclaw.app.brain

import android.util.Log
import com.pocketclaw.app.cloud.Action
import com.pocketclaw.app.cloud.CloudResponse
import com.pocketclaw.app.cloud.ExtractedMemory
import com.pocketclaw.app.cloud.ResponseMeta
import com.pocketclaw.app.data.Memory

class BrainEngine(
    private var llmProvider: LlmProvider,
) {
    companion object {
        private const val TAG = "Brain"
    }

    fun switchProvider(provider: LlmProvider) {
        llmProvider = provider
        Log.i(TAG, "Switched LLM provider: ${provider.name}")
    }

    val currentProviderName: String get() = llmProvider.name
    val isReady: Boolean get() = llmProvider.isReady

    data class SkillCreationResult(
        val name: String,
        val description: String,
        val keywords: List<String>,
    )

    suspend fun processUserMessage(
        text: String,
        memories: List<Memory> = emptyList(),
    ): CloudResponse {
        val start = System.currentTimeMillis()

        val skills = SkillRouter.route(text)
        val skillId = skills.firstOrNull()?.id ?: "message_triage"

        val systemPrompt = assembleContext(skills, memories)
        val result = llmProvider.generate(systemPrompt, text)
        val latencyMs = (System.currentTimeMillis() - start).toInt()

        Log.i(TAG, "Processed: skill=$skillId tokens=${result.tokensUsed} latency=${latencyMs}ms")

        val actions = parseActions(result.content)
        val extractedMemories = extractMemories(result.content)

        return CloudResponse(
            actions = actions,
            meta = ResponseMeta(
                skillUsed = skillId,
                tokensConsumed = result.tokensUsed,
                tokensSaved = 0,
                latencyMs = latencyMs,
            ),
            memories = extractedMemories.ifEmpty { null },
        )
    }

    suspend fun generateSkillDefinition(userRequest: String): SkillCreationResult? {
        val prompt = buildString {
            append("The user wants to add a new skill/capability. ")
            append("Based on their request, generate a skill definition.\n\n")
            append("Output EXACTLY in this format (one line each):\n")
            append("[SKILL_NAME] Short skill name\n")
            append("[SKILL_DESC] One sentence describing what this skill does\n")
            append("[SKILL_KEYWORDS] comma,separated,keywords,for,routing\n\n")
            append("Only output these 3 lines, nothing else.")
        }
        val result = llmProvider.generate(prompt, userRequest)
        return parseSkillDefinition(result.content)
    }

    private fun parseSkillDefinition(output: String): SkillCreationResult? {
        var name: String? = null
        var desc: String? = null
        var keywords: List<String>? = null

        for (line in output.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[SKILL_NAME]") ->
                    name = trimmed.removePrefix("[SKILL_NAME]").trim()
                trimmed.startsWith("[SKILL_DESC]") ->
                    desc = trimmed.removePrefix("[SKILL_DESC]").trim()
                trimmed.startsWith("[SKILL_KEYWORDS]") ->
                    keywords = trimmed.removePrefix("[SKILL_KEYWORDS]").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
        }

        return if (name != null && desc != null && keywords != null) {
            SkillCreationResult(name, desc, keywords)
        } else {
            Log.w(TAG, "Failed to parse skill definition from: $output")
            null
        }
    }

    suspend fun processNotification(
        pkg: String,
        title: String?,
        text: String,
        memories: List<Memory> = emptyList(),
    ): CloudResponse {
        val userText = "[$pkg] ${title ?: ""}: $text"
        return processUserMessage(userText, memories)
    }

    private fun assembleContext(
        skills: List<SkillDescriptor>,
        memories: List<Memory>,
    ): String = buildString {
        append("You are PocketClaw, a pocket butler running on the user's phone. ")
        append("Be concise, helpful, and friendly. Respond in the same language as the user.\n\n")

        append("CRITICAL RULES:\n")
        append("- NEVER output XML, tool_call, function_call, or any structured format\n")
        append("- NEVER output <tool_call>, <invoke>, <function>, or similar tags\n")
        append("- Always reply in plain natural language directly to the user\n")
        append("- If you cannot perform an action (like checking real-time weather), say so honestly and suggest alternatives\n\n")

        append("Your knowledge areas:\n")
        for (skill in skills) {
            val tag = if (skill.isCustom) " [custom]" else ""
            append("- ${skill.name}$tag: ${skill.description}\n")
        }
        append("\n")

        if (memories.isNotEmpty()) {
            append("What you know about this user:\n")
            for (mem in memories.takeLast(20)) {
                append("- [${mem.type}] ${mem.key}: ${mem.value}\n")
            }
            append("\n")
        }

        append("Instructions:\n")
        append("1. Analyze the user's message and respond helpfully in plain text\n")
        append("2. If you learn something new about the user, output a memory line: [MEMORY:type:key:value]\n")
        append("   Types: preference, habit, relationship, fact\n")
        append("3. Keep responses under 200 words\n")
    }

    private fun parseActions(llmOutput: String): List<Action> {
        val cleanOutput = llmOutput
            .lines()
            .filter { line ->
                val t = line.trim()
                !t.startsWith("[MEMORY:") &&
                !t.startsWith("<") &&
                !t.startsWith("</")
            }
            .joinToString("\n")
            .replace(Regex("<[^>]+>"), "")
            .trim()

        if (cleanOutput.isBlank()) {
            return listOf(Action(type = "silent"))
        }

        return listOf(
            Action(
                type = "notify",
                priority = "normal",
                title = "PocketClaw",
                body = cleanOutput,
            )
        )
    }

    private fun extractMemories(llmOutput: String): List<ExtractedMemory> {
        val memories = mutableListOf<ExtractedMemory>()

        for (line in llmOutput.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[MEMORY:") && trimmed.endsWith("]")) {
                val inner = trimmed.substring(8, trimmed.length - 1)
                val parts = inner.split(":", limit = 4)
                if (parts.size >= 3) {
                    memories.add(
                        ExtractedMemory(
                            type = parts[0],
                            key = parts[1],
                            value = parts[2],
                            confidence = parts.getOrNull(3)?.toFloatOrNull() ?: 0.6f,
                        )
                    )
                }
            }
        }
        return memories
    }
}
