package com.pocketclaw.claw.skills

import com.pocketclaw.claw.prompt.SkillsPrompt

/**
 * Routes user intent to the most relevant skills for prompt context.
 * Uses keyword matching (good enough for on-device; no embedding needed).
 */
object SkillRouter {

    fun routeSkills(
        userMessage: String,
        customSkills: List<CustomSkill>,
        topK: Int = 4,
    ): List<SkillsPrompt.SkillDef> {
        val lower = userMessage.lowercase()
        val allSkills = buildFullList(customSkills)

        val scored = allSkills.map { skill ->
            val keywordHits = skill.keywords.count { kw -> lower.contains(kw) }
            val nameHit = if (lower.contains(skill.name.lowercase())) 2 else 0
            Pair(skill, keywordHits + nameHit)
        }

        val matched = scored.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first.toSkillDef() }

        return matched.ifEmpty {
            SkillsPrompt.BUILTIN.take(topK)
        }
    }

    fun isSkillCreationRequest(text: String): Boolean {
        val lower = text.lowercase()
        val patterns = listOf(
            "增加skill", "添加skill", "新skill", "创建skill",
            "add skill", "new skill", "create skill",
            "增加技能", "添加技能", "新技能", "创建技能",
            "学一个", "学一下", "教你一个",
        )
        return patterns.any { lower.contains(it) }
    }

    private fun buildFullList(custom: List<CustomSkill>): List<SkillEntry> {
        val builtins = SkillsPrompt.BUILTIN.map { skill ->
            SkillEntry(
                id = skill.id,
                name = skill.name,
                description = skill.description,
                keywords = skill.exampleQ.split("\\s+".toRegex()).filter { it.length > 1 }.take(5),
                isCustom = false,
            )
        }

        val customs = custom.filter { it.enabled }.map { skill ->
            SkillEntry(
                id = "custom_${skill.id}",
                name = skill.name,
                description = skill.description,
                keywords = skill.keywords.split(",").map { it.trim() }.filter { it.isNotBlank() },
                isCustom = true,
            )
        }

        return builtins + customs
    }

    private data class SkillEntry(
        val id: String,
        val name: String,
        val description: String,
        val keywords: List<String>,
        val isCustom: Boolean,
    ) {
        fun toSkillDef() = SkillsPrompt.SkillDef(
            id = id,
            name = name,
            description = description,
            exampleQ = "",
            exampleA = "",
            isCustom = isCustom,
        )
    }
}
