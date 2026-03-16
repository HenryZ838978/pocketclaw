package com.pocketclaw.claw.tools

import android.content.Context
import com.pocketclaw.app.PocketClawApplication
import com.pocketclaw.app.data.ScheduledTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleCreateTool : Tool {
    override val id = "schedule_create"
    override val name = "创建提醒"
    override val description = "创建一个定时提醒（格式：HH:MM:提醒内容）"
    override val riskLevel = RiskLevel.L1_WRITE
    override val paramHint = "HH:MM:内容"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val parts = args.split(":", limit = 3)
        if (parts.size < 3) return ToolResult(false, "Format: HH:MM:content", id, args)

        val hour = parts[0].trim().toIntOrNull()
        val minute = parts[1].trim().toIntOrNull()
        val content = parts[2].trim()

        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return ToolResult(false, "Invalid time. Use HH:MM (24h format)", id, args)
        }

        val app = context.applicationContext as PocketClawApplication
        val task = ScheduledTask(
            name = content.take(30),
            action = content,
            hour = hour,
            minute = minute,
        )
        withContext(Dispatchers.IO) {
            app.database.scheduledTaskDao().upsert(task)
        }

        return ToolResult(true, "Reminder set for %02d:%02d - $content".format(hour, minute), id, args)
    }
}

class ScheduleListTool : Tool {
    override val id = "schedule_list"
    override val name = "查看提醒"
    override val description = "列出所有定时提醒"
    override val riskLevel = RiskLevel.L0_READ
    override val paramHint = ""

    override suspend fun execute(args: String, context: Context): ToolResult {
        val app = context.applicationContext as PocketClawApplication
        val tasks = withContext(Dispatchers.IO) {
            app.database.scheduledTaskDao().enabledTasks()
        }
        if (tasks.isEmpty()) {
            return ToolResult(true, "No scheduled reminders", id, args)
        }
        val list = tasks.joinToString("\n") { t ->
            "%02d:%02d - %s%s".format(t.hour, t.minute, t.name, if (t.repeating) " (daily)" else "")
        }
        return ToolResult(true, list, id, args)
    }

    override fun summarize(raw: String, budget: Int): String {
        val lines = raw.lines()
        if (raw.length <= budget) return raw
        return lines.take(5).joinToString("\n") + "\n...(共${lines.size}条)"
    }
}
