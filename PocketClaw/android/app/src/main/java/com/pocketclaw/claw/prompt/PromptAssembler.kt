package com.pocketclaw.claw.prompt

import com.pocketclaw.claw.bond.GrowthSystem
import com.pocketclaw.claw.bond.BondGrowth

/**
 * Central prompt assembler with dynamic token budgeting.
 * Combines SOUL + USER + SKILLS + TOOLS + CONTEXT into a single system prompt.
 * Budget adapts to local (small context) vs cloud (large context) mode.
 */
object PromptAssembler {

    data class ChatTurn(
        val role: String,
        val content: String,
    )

    data class AssembledPrompt(
        val systemPrompt: String,
        val chatHistory: List<ChatTurn>,
        val userMessage: String,
    )

    fun assemble(
        memories: List<UserProfile.MemoryEntry>,
        skills: List<SkillsPrompt.SkillDef>,
        recentHistory: List<ChatTurn>,
        userMessage: String,
        growthStage: Int = 0,
        toolResults: List<ToolContext> = emptyList(),
        budget: ContextBudget = ContextBudget.LOCAL,
    ): AssembledPrompt {
        val systemPrompt = buildString {
            val soul = SOUL.build()
            append(soul.take(budget.systemChars))

            val profile = UserProfile.build(memories, maxTokenBudget = budget.profileChars / 2)
            if (profile.isNotBlank()) {
                append("\n\n")
                append(profile.take(budget.profileChars))
            }

            val allSkills = skills.ifEmpty { SkillsPrompt.BUILTIN }
            val skillsSection = SkillsPrompt.build(allSkills)
            if (skillsSection.isNotBlank()) {
                append("\n")
                append(skillsSection.take(budget.skillsChars))
            }

            if (toolResults.isNotEmpty()) {
                append("\n\n最近工具结果：\n")
                val limited = toolResults.takeLast(budget.maxToolResults)
                var usedChars = 0
                for (tr in limited) {
                    val line = "[工具结果] ${tr.toolId}(${tr.args.take(40)}): ${tr.summary}"
                    if (usedChars + line.length > budget.toolResultChars) break
                    append(line)
                    append("\n")
                    usedChars += line.length
                }
            }

            if (growthStage > 0) {
                val stageInfo = GrowthSystem.getStageDescription(growthStage)
                append("\n你当前的成长阶段：${stageInfo.emoji} ${stageInfo.nameZh}（阶段$growthStage）。${stageInfo.description}")

                val growth = BondGrowth(stage = growthStage, totalInteractions = 0, positiveInteractions = 0, streakDays = 0)
                val traits = GrowthSystem.discoverTraits(growth)
                val traitModifier = GrowthSystem.getPersonalityModifier(traits)
                if (traitModifier.isNotBlank()) {
                    append(traitModifier)
                }
            }
        }

        val trimmedHistory = trimHistory(recentHistory, budget)

        return AssembledPrompt(
            systemPrompt = systemPrompt,
            chatHistory = trimmedHistory,
            userMessage = userMessage,
        )
    }

    fun toQwen3Format(assembled: AssembledPrompt): String = buildString {
        append("<|im_start|>system\n")
        append(assembled.systemPrompt)
        append("<|im_end|>\n")

        for (turn in assembled.chatHistory) {
            append("<|im_start|>${turn.role}\n")
            append(turn.content)
            append("<|im_end|>\n")
        }

        append("<|im_start|>user\n")
        append(assembled.userMessage)
        append("<|im_end|>\n")
        append("<|im_start|>assistant\n")
    }

    fun toGenericFormat(assembled: AssembledPrompt): String = buildString {
        append("system: ")
        append(assembled.systemPrompt)
        append("\n\n")

        for (turn in assembled.chatHistory) {
            append("${turn.role}: ${turn.content}\n")
        }

        append("user: ${assembled.userMessage}\n")
        append("assistant: ")
    }

    private fun trimHistory(history: List<ChatTurn>, budget: ContextBudget): List<ChatTurn> {
        if (history.isEmpty()) return emptyList()

        val recent = history.takeLast(budget.historyTurns * 2)
        val result = mutableListOf<ChatTurn>()
        var totalChars = 0

        for (turn in recent.reversed()) {
            if (totalChars + turn.content.length > budget.historyChars) break
            result.add(0, turn)
            totalChars += turn.content.length
        }

        return result
    }

    fun extractMemories(llmOutput: String): List<UserProfile.MemoryEntry> {
        val memories = mutableListOf<UserProfile.MemoryEntry>()
        for (line in llmOutput.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[M:") && trimmed.endsWith("]")) {
                val inner = trimmed.substring(3, trimmed.length - 1)
                val parts = inner.split(":", limit = 3)
                if (parts.size >= 3) {
                    memories.add(UserProfile.MemoryEntry(
                        type = parts[0],
                        key = parts[1],
                        value = parts[2],
                    ))
                }
            }
        }
        return memories
    }

    fun cleanOutput(llmOutput: String): String {
        return llmOutput
            .lines()
            .filter { line ->
                val t = line.trim()
                !t.startsWith("[M:") &&
                !t.startsWith("[T:") &&
                !t.startsWith("<") &&
                !t.startsWith("</") &&
                !t.contains("tool_call") &&
                !t.contains("function_call")
            }
            .joinToString("\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("```[a-z]*\\n?"), "")
            .trim()
    }
}
