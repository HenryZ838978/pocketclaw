package com.pocketclaw.claw.tools

/**
 * Parses [T:tool_id:args] markers from LLM output.
 * Designed to work with small models that can reliably produce simple text markers.
 */
object ToolParser {

    data class ParsedCall(
        val toolId: String,
        val args: String,
        val rawMarker: String,
    )

    private val TOOL_PATTERN = Regex("""\[T:([a-z_]+):(.+?)]""")

    fun parse(llmOutput: String): List<ParsedCall> {
        return TOOL_PATTERN.findAll(llmOutput).map { match ->
            ParsedCall(
                toolId = match.groupValues[1],
                args = match.groupValues[2],
                rawMarker = match.value,
            )
        }.toList()
    }

    fun hasToolCall(llmOutput: String): Boolean {
        return TOOL_PATTERN.containsMatchIn(llmOutput)
    }

    fun stripToolMarkers(llmOutput: String): String {
        return llmOutput
            .lines()
            .filter { !TOOL_PATTERN.containsMatchIn(it) }
            .joinToString("\n")
            .trim()
    }
}
