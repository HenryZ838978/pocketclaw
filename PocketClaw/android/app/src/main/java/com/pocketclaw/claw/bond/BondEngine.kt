package com.pocketclaw.claw.bond

import com.pocketclaw.claw.prompt.PromptAssembler
import com.pocketclaw.claw.prompt.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bond Engine: manages the relationship state between user and PocketClaw.
 * Handles memory extraction, growth progression, and personality evolution.
 */
class BondEngine(
    private val memoryDao: BondMemoryDao,
    val growthDao: BondGrowthDao,
) {

    companion object {
        // XP thresholds per growth stage
        private val STAGE_THRESHOLDS = intArrayOf(0, 50, 200, 500, 1500)
        // XP per interaction type
        private const val XP_CHAT = 2
        private const val XP_MEMORY_FORMED = 5
        private const val XP_SKILL_USED = 3
        private const val XP_STREAK_BONUS = 10
    }

    /**
     * Process LLM output for memory markers and record interaction.
     * Returns cleaned output with markers stripped.
     */
    suspend fun processResponse(llmOutput: String): String {
        val memories = PromptAssembler.extractMemories(llmOutput)
        var xpGained = XP_CHAT

        for (mem in memories) {
            val existing = memoryDao.findByKey(mem.key, mem.type)
            if (existing != null) {
                memoryDao.upsert(existing.copy(
                    value = mem.value,
                    confidence = (existing.confidence + 0.1f).coerceAtMost(1.0f),
                    updatedAt = System.currentTimeMillis(),
                ))
            } else {
                memoryDao.upsert(BondMemory(
                    type = mem.type,
                    key = mem.key,
                    value = mem.value,
                    confidence = mem.confidence,
                ))
            }
            xpGained += XP_MEMORY_FORMED
        }

        updateGrowth(xpGained)
        return PromptAssembler.cleanOutput(llmOutput)
    }

    /**
     * Get current memories as prompt-ready entries.
     */
    suspend fun getMemoriesForPrompt(): List<UserProfile.MemoryEntry> {
        return memoryDao.recent(20).map {
            UserProfile.MemoryEntry(
                type = it.type,
                key = it.key,
                value = it.value,
                confidence = it.confidence,
            )
        }
    }

    /**
     * Get current growth stage (0-4).
     */
    suspend fun getGrowthStage(): Int {
        return growthDao.getGrowth()?.stage ?: 0
    }

    /**
     * Record an interaction and progress growth.
     */
    private suspend fun updateGrowth(xpGained: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val current = growthDao.getGrowth() ?: BondGrowth()

        val isNewDay = current.lastInteractionDate != today
        val newStreak = if (isNewDay) {
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Date(System.currentTimeMillis() - 86_400_000))
            if (current.lastInteractionDate == yesterday) current.streakDays + 1 else 1
        } else {
            current.streakDays
        }
        val streakBonus = if (isNewDay && newStreak > 1) XP_STREAK_BONUS else 0

        val newXp = current.xp + xpGained + streakBonus
        val newStage = STAGE_THRESHOLDS.indexOfLast { newXp >= it }.coerceAtLeast(0)

        growthDao.upsert(current.copy(
            stage = newStage,
            totalInteractions = current.totalInteractions + 1,
            positiveInteractions = current.positiveInteractions + 1,
            streakDays = newStreak,
            lastInteractionDate = today,
            xp = newXp,
        ))
    }

    /**
     * Export bond state as JSON for portability.
     */
    suspend fun exportBondState(): String {
        val memories = memoryDao.getAllMemories()
        val growth = growthDao.getGrowth()

        val sb = StringBuilder()
        sb.append("{\"version\":1,\"exportedAt\":\"${Date()}\",")
        sb.append("\"growth\":{")
        if (growth != null) {
            sb.append("\"stage\":${growth.stage},\"xp\":${growth.xp},\"streak\":${growth.streakDays},\"totalInteractions\":${growth.totalInteractions}")
        }
        sb.append("},\"memories\":[")
        memories.forEachIndexed { i, m ->
            if (i > 0) sb.append(",")
            sb.append("{\"type\":\"${m.type}\",\"key\":\"${m.key}\",\"value\":\"${m.value.replace("\"", "\\\"")}\",\"confidence\":${m.confidence}}")
        }
        sb.append("]}")
        return sb.toString()
    }

    /**
     * Import bond state from JSON.
     */
    suspend fun importBondState(json: String) {
        try {
            val memoriesStart = json.indexOf("\"memories\":[") + 12
            val memoriesEnd = json.lastIndexOf("]")
            if (memoriesStart < 12 || memoriesEnd < memoriesStart) return

            val memoriesJson = json.substring(memoriesStart, memoriesEnd)
            val memoryPattern = Regex("""\{"type":"([^"]+)","key":"([^"]+)","value":"([^"]+)","confidence":([\d.]+)\}""")

            for (match in memoryPattern.findAll(memoriesJson)) {
                memoryDao.upsert(BondMemory(
                    type = match.groupValues[1],
                    key = match.groupValues[2],
                    value = match.groupValues[3],
                    confidence = match.groupValues[4].toFloatOrNull() ?: 0.5f,
                ))
            }
        } catch (_: Exception) {
            // Import is best-effort
        }
    }
}
