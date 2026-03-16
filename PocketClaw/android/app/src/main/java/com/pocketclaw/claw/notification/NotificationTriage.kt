package com.pocketclaw.claw.notification

/**
 * Notification Triage: proactive butler behavior.
 * Categorizes and prioritizes incoming notifications so the user
 * only sees what matters. Runs as a lightweight classifier.
 *
 * Integration: Hook into Android's NotificationListenerService.
 */
object NotificationTriage {

    enum class Priority { URGENT, NORMAL, LOW, SPAM }

    data class TriagedNotification(
        val packageName: String,
        val title: String,
        val content: String,
        val priority: Priority,
        val reason: String,
    )

    private val URGENT_KEYWORDS = listOf(
        "紧急", "urgent", "emergency", "alert", "验证码", "verification",
        "confirm", "确认", "安全", "security", "payment", "付款",
    )

    private val SPAM_PACKAGES = setOf(
        "com.taobao.taobao", "com.tencent.mm.plugin.appbrand",
    )

    private val SPAM_KEYWORDS = listOf(
        "优惠", "coupon", "discount", "限时", "limited time",
        "广告", "推广", "红包", "领取",
    )

    fun triage(
        packageName: String,
        title: String,
        content: String,
        userPreferences: Map<String, String> = emptyMap(),
    ): TriagedNotification {
        val combined = "$title $content".lowercase()

        if (URGENT_KEYWORDS.any { combined.contains(it) }) {
            return TriagedNotification(packageName, title, content, Priority.URGENT, "Contains urgent keywords")
        }

        if (packageName in SPAM_PACKAGES || SPAM_KEYWORDS.count { combined.contains(it) } >= 2) {
            return TriagedNotification(packageName, title, content, Priority.SPAM, "Likely promotional")
        }

        val isFromContact = userPreferences.any { (key, _) ->
            key.startsWith("contact_") && combined.contains(key.removePrefix("contact_").lowercase())
        }
        if (isFromContact) {
            return TriagedNotification(packageName, title, content, Priority.NORMAL, "From known contact")
        }

        return TriagedNotification(packageName, title, content, Priority.LOW, "Default classification")
    }

    fun formatSummary(notifications: List<TriagedNotification>): String {
        val urgent = notifications.filter { it.priority == Priority.URGENT }
        val normal = notifications.filter { it.priority == Priority.NORMAL }

        return buildString {
            if (urgent.isNotEmpty()) {
                append("紧急通知 (${urgent.size}):\n")
                urgent.take(3).forEach { append("  - ${it.title}: ${it.content.take(50)}\n") }
            }
            if (normal.isNotEmpty()) {
                append("普通通知 (${normal.size}):\n")
                normal.take(5).forEach { append("  - ${it.title}\n") }
            }
            val spamCount = notifications.count { it.priority == Priority.SPAM }
            if (spamCount > 0) {
                append("已过滤 $spamCount 条推广通知\n")
            }
        }
    }
}
