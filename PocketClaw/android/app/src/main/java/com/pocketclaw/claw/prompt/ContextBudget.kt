package com.pocketclaw.claw.prompt

/**
 * Dynamic token budget allocation that adapts to local vs cloud mode.
 * "Context Claw" — precisely grab what's useful, discard noise.
 */
data class ContextBudget(
    val systemChars: Int,
    val profileChars: Int,
    val skillsChars: Int,
    val toolResultChars: Int,
    val historyChars: Int,
    val historyTurns: Int,
    val maxToolResults: Int,
) {
    companion object {
        val LOCAL = ContextBudget(
            systemChars = 800,
            profileChars = 400,
            skillsChars = 600,
            toolResultChars = 1000,
            historyChars = 2000,
            historyTurns = 10,
            maxToolResults = 2,
        )

        val CLOUD = ContextBudget(
            systemChars = 1600,
            profileChars = 800,
            skillsChars = 1200,
            toolResultChars = 4000,
            historyChars = 6000,
            historyTurns = 30,
            maxToolResults = 5,
        )

        fun forMode(mode: String): ContextBudget = when (mode) {
            "api" -> CLOUD
            else -> LOCAL
        }
    }
}

/**
 * A tool result that has been summarized and is ready for prompt injection.
 */
data class ToolContext(
    val toolId: String,
    val args: String,
    val summary: String,
)
