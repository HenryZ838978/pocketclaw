package com.pocketclaw.claw.prompt

/**
 * Skills prompt section with few-shot examples.
 * Small models need concrete examples to understand expected behavior.
 */
object SkillsPrompt {

    data class SkillDef(
        val id: String,
        val name: String,
        val description: String,
        val exampleQ: String,
        val exampleA: String,
        val isCustom: Boolean = false,
    )

    val BUILTIN = listOf(
        SkillDef(
            "chat", "日常聊天",
            "和用户闲聊，回答问题，提供建议",
            "今天好累啊", "辛苦了！要不要早点休息？明天又是新的一天。",
        ),
        SkillDef(
            "translate", "翻译",
            "在不同语言之间翻译文本",
            "帮我翻译：今天天气真好",
            "The weather is really nice today.",
        ),
        SkillDef(
            "note", "记笔记",
            "帮用户记住重要的事情",
            "帮我记一下，周五下午3点开会",
            "好的，已记住：周五下午3点开会。\n[M:fact:meeting:周五下午3点]",
        ),
        SkillDef(
            "summarize", "总结",
            "总结长文本的关键信息",
            "总结一下这段话：（长文本）",
            "核心要点：1. ... 2. ... 3. ...",
        ),
    )

    fun build(activeSkills: List<SkillDef>, maxExamples: Int = 2): String {
        if (activeSkills.isEmpty()) return ""

        return buildString {
            append("\n你的能力：\n")
            for (skill in activeSkills.take(6)) {
                val tag = if (skill.isCustom) "★" else ""
                append("- $tag${skill.name}：${skill.description}\n")
            }

            val examples = activeSkills.take(maxExamples)
            if (examples.isNotEmpty()) {
                append("\n回复示范：\n")
                for (skill in examples) {
                    append("用户：${skill.exampleQ}\n")
                    append("你：${skill.exampleA}\n\n")
                }
            }
        }
    }
}
