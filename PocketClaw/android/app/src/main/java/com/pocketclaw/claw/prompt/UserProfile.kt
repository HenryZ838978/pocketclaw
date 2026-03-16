package com.pocketclaw.claw.prompt

/**
 * Compressed user profile for context injection.
 * Uses ultra-compact token format per EMERGENT_BOND.md Section VII:
 * [U:food=红烧肉|city=北京|lang=zh] < 100 tokens total
 */
object UserProfile {

    data class MemoryEntry(
        val type: String,
        val key: String,
        val value: String,
        val confidence: Float = 0.5f,
    )

    fun build(memories: List<MemoryEntry>, maxTokenBudget: Int = 200): String {
        if (memories.isEmpty()) return ""

        val grouped = memories.groupBy { it.type }
        val sb = StringBuilder()
        sb.append("用户档案：")

        for ((type, entries) in grouped) {
            val label = when (type) {
                "pref", "preference" -> "偏好"
                "habit" -> "习惯"
                "rel", "relationship" -> "关系"
                "fact" -> "事实"
                else -> type
            }
            val items = entries
                .sortedByDescending { it.confidence }
                .take(8)
                .joinToString("、") { "${it.key}=${it.value}" }
            sb.append("\n[$label] $items")
        }

        val result = sb.toString()
        val approxTokens = result.length / 2
        if (approxTokens > maxTokenBudget) {
            return result.take(maxTokenBudget * 2)
        }
        return result
    }
}
