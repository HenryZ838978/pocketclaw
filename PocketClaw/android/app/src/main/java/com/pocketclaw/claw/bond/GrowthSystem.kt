package com.pocketclaw.claw.bond

/**
 * Growth/Nurture System: the butler "grows up" based on interaction patterns.
 * Personality traits emerge from usage patterns rather than being predefined.
 *
 * Stage progression:
 *   0 = Egg (fresh install, no interactions)
 *   1 = Hatchling (XP >= 50, ~25 conversations)
 *   2 = Juvenile (XP >= 200, ~100 conversations)
 *   3 = Adult (XP >= 500, ~250 conversations)
 *   4 = Elder (XP >= 1500, ~750 conversations)
 *
 * Trait discovery: after enough interactions of a type, new personality
 * traits unlock that affect the system prompt and response style.
 */
object GrowthSystem {

    data class Trait(
        val id: String,
        val name: String,
        val nameZh: String,
        val description: String,
        val requiredStage: Int,
        val condition: (BondGrowth) -> Boolean,
    )

    val ALL_TRAITS = listOf(
        Trait(
            "curious", "Curious", "好奇心",
            "Asks follow-up questions occasionally",
            1, { it.totalInteractions > 30 }
        ),
        Trait(
            "empathetic", "Empathetic", "共情",
            "Notices and responds to emotional cues",
            1, { it.positiveInteractions > 20 }
        ),
        Trait(
            "proactive", "Proactive", "主动",
            "Sometimes suggests things without being asked",
            2, { it.streakDays > 7 }
        ),
        Trait(
            "witty", "Witty", "机智",
            "Adds occasional humor to conversations",
            2, { it.totalInteractions > 150 }
        ),
        Trait(
            "wise", "Wise", "智慧",
            "Offers deeper insights and nuanced advice",
            3, { it.totalInteractions > 300 }
        ),
        Trait(
            "loyal", "Loyal", "忠诚",
            "References shared history and past conversations",
            3, { it.streakDays > 30 }
        ),
        Trait(
            "mentor", "Mentor", "导师",
            "Proactively teaches and guides the user",
            4, { it.totalInteractions > 500 }
        ),
    )

    fun discoverTraits(growth: BondGrowth): List<Trait> {
        return ALL_TRAITS.filter { trait ->
            growth.stage >= trait.requiredStage && trait.condition(growth)
        }
    }

    fun getPersonalityModifier(traits: List<Trait>): String {
        if (traits.isEmpty()) return ""

        return buildString {
            append("\n你的性格特征：")
            for (trait in traits) {
                append("\n- ${trait.nameZh}：${trait.description}")
            }
        }
    }

    fun getStageDescription(stage: Int): StageInfo {
        return when (stage) {
            0 -> StageInfo("Egg", "蛋", "刚出生，什么都是新的", "🥚")
            1 -> StageInfo("Hatchling", "幼崽", "好奇地探索世界", "🦞")
            2 -> StageInfo("Juvenile", "少年", "开始形成自己的个性", "🦐")
            3 -> StageInfo("Adult", "成年", "可靠的伙伴", "🦀")
            4 -> StageInfo("Elder", "长老", "深厚的理解与默契", "👑")
            else -> StageInfo("Unknown", "未知", "", "❓")
        }
    }

    data class StageInfo(
        val name: String,
        val nameZh: String,
        val description: String,
        val emoji: String,
    )
}
