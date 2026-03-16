package com.pocketclaw.app.brain

import com.pocketclaw.app.data.CustomSkill

data class SkillDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val keywords: List<String>,
    val isCustom: Boolean = false,
)

val BUILTIN_SKILLS = listOf(
    SkillDescriptor(
        "message_triage", "Message Triage",
        "Classify incoming messages by urgency. Summarize key points. Flag messages that need immediate attention.",
        listOf("message", "notification", "urgent", "important", "spam", "read", "unread"),
    ),
    SkillDescriptor(
        "schedule_manage", "Schedule Manager",
        "Create, modify, and check calendar events. Set reminders for meetings. Detect scheduling conflicts.",
        listOf("schedule", "calendar", "meeting", "reminder", "event", "tomorrow", "today", "time", "date"),
    ),
    SkillDescriptor(
        "quick_reply", "Quick Reply",
        "Draft contextually appropriate short replies to messages. Offer 2-3 reply suggestions.",
        listOf("reply", "respond", "answer", "message", "chat", "say", "tell"),
    ),
    SkillDescriptor(
        "web_search", "Web Search",
        "Search the web for real-time information. Return concise summaries.",
        listOf("search", "google", "find", "what is", "how to", "weather", "news"),
    ),
    SkillDescriptor(
        "expense_track", "Expense Tracker",
        "Parse receipt descriptions into expense entries. Categorize spending. Track totals.",
        listOf("expense", "cost", "money", "pay", "receipt", "spent", "price", "buy"),
    ),
    SkillDescriptor(
        "translate", "Translator",
        "Translate text between languages. Auto-detect source language.",
        listOf("translate", "translation", "language", "english", "chinese", "japanese"),
    ),
    SkillDescriptor(
        "daily_digest", "Daily Digest",
        "Generate end-of-day summary of all processed messages and events.",
        listOf("summary", "digest", "today", "recap", "overview", "report"),
    ),
    SkillDescriptor(
        "contact_lookup", "Contact Lookup",
        "Find contact information by name or relationship.",
        listOf("contact", "phone", "email", "who", "person", "name", "call"),
    ),
    SkillDescriptor(
        "note_capture", "Quick Note",
        "Capture voice or text notes and organize them. Tag notes by topic.",
        listOf("note", "write", "remember", "save", "memo", "jot"),
    ),
    SkillDescriptor(
        "alarm_timer", "Alarm & Timer",
        "Set alarms, timers, and countdown reminders.",
        listOf("alarm", "timer", "wake", "countdown", "minutes", "hours"),
    ),
    SkillDescriptor(
        "file_manage", "File Manager",
        "Organize photos, documents, and downloads. Clean up files.",
        listOf("file", "photo", "document", "download", "storage", "clean", "organize"),
    ),
    SkillDescriptor(
        "weather_check", "Weather",
        "Check current weather and forecast for any location.",
        listOf("weather", "rain", "sunny", "temperature", "forecast", "cold", "hot"),
    ),
)

fun CustomSkill.toDescriptor() = SkillDescriptor(
    id = skillId,
    name = name,
    description = description,
    keywords = keywords.split(",").map { it.trim() }.filter { it.isNotBlank() },
    isCustom = true,
)

object SkillRouter {

    private var customSkills: List<SkillDescriptor> = emptyList()

    fun updateCustomSkills(skills: List<CustomSkill>) {
        customSkills = skills.map { it.toDescriptor() }
    }

    private val allSkills: List<SkillDescriptor>
        get() = customSkills + BUILTIN_SKILLS

    fun route(userText: String, topK: Int = 3): List<SkillDescriptor> {
        val lowerText = userText.lowercase()
        val words = lowerText.split(Regex("[\\s,.:;!?]+")).filter { it.length > 1 }

        val scored = allSkills.map { skill ->
            val keywordHits = skill.keywords.count { keyword ->
                lowerText.contains(keyword)
            }
            val descHits = words.count { word ->
                skill.description.lowercase().contains(word)
            }
            val customBonus = if (skill.isCustom) 2 else 0
            skill to (keywordHits * 3 + descHits + customBonus)
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(topK)
        .map { it.first }

        return scored.ifEmpty { listOf(BUILTIN_SKILLS[0]) }
    }

    fun isAddSkillRequest(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("add") || lower.contains("create") || lower.contains("new") ||
                lower.contains("增加") || lower.contains("添加") || lower.contains("新建") ||
                lower.contains("加一个") || lower.contains("教你"))
                &&
               (lower.contains("skill") || lower.contains("技能") || lower.contains("能力") ||
                lower.contains("功能"))
    }

    data class ParsedSkill(
        val name: String,
        val description: String,
        val keywords: List<String>,
    )
}
