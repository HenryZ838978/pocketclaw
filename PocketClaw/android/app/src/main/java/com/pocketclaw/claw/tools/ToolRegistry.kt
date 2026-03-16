package com.pocketclaw.claw.tools

import android.content.Context

enum class RiskLevel { L0_READ, L1_WRITE, L2_DESTRUCTIVE, L3_SYSTEM }

interface Tool {
    val id: String
    val name: String
    val description: String
    val riskLevel: RiskLevel
    val paramHint: String

    suspend fun execute(args: String, context: Context): ToolResult

    fun summarize(raw: String, budget: Int): String {
        return if (raw.length <= budget) raw
        else raw.take(budget - 20) + "...(${raw.length} chars total)"
    }
}

data class ToolResult(
    val success: Boolean,
    val output: String,
    val toolId: String,
    val args: String,
)

object ToolRegistry {

    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.id] = tool
    }

    fun get(id: String): Tool? = tools[id]

    fun all(): List<Tool> = tools.values.toList()

    fun buildToolListPrompt(): String {
        if (tools.isEmpty()) return ""
        return buildString {
            append("## 可用工具\n")
            for (tool in tools.values) {
                append("- ${tool.id}(${tool.paramHint}): ${tool.description}\n")
            }
            append("\n调用格式：[T:工具ID:参数]（独占一行，放在回复末尾）\n")
            append("规则：每条回复最多1个工具；先文字再工具；不需要就不写。\n")
        }
    }
}
