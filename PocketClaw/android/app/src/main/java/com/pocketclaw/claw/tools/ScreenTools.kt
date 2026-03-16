package com.pocketclaw.claw.tools

import android.content.Context
import com.pocketclaw.app.service.ScreenControlService

class ScreenReadTool : Tool {
    override val id = "screen_read"
    override val name = "读取屏幕"
    override val description = "获取当前屏幕的可交互元素列表"
    override val riskLevel = RiskLevel.L0_READ
    override val paramHint = "current"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val service = ScreenControlService.instance
            ?: return ToolResult(false, "Accessibility service not enabled. Go to Settings > Accessibility > PocketClaw.", id, args)
        val tree = service.readScreen()
        return ToolResult(true, tree, id, args)
    }

    override fun summarize(raw: String, budget: Int): String {
        val lines = raw.lines().filter { it.isNotBlank() }
        if (raw.length <= budget) return raw
        val kept = mutableListOf<String>()
        var chars = 0
        for (line in lines) {
            if (line.contains("clickable") || line.contains("editable") || kept.size < 3) {
                if (chars + line.length > budget - 30) break
                kept.add(line.trim())
                chars += line.length
            }
        }
        return kept.joinToString("\n") + "\n...(${lines.size} nodes total)"
    }
}

class ScreenTapTool : Tool {
    override val id = "screen_tap"
    override val name = "点击屏幕"
    override val description = "在指定坐标点击屏幕"
    override val riskLevel = RiskLevel.L3_SYSTEM
    override val paramHint = "x,y"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val service = ScreenControlService.instance
            ?: return ToolResult(false, "Accessibility service not enabled", id, args)

        val parts = args.split(",").map { it.trim() }
        if (parts.size != 2) return ToolResult(false, "Format: x,y", id, args)
        val x = parts[0].toFloatOrNull() ?: return ToolResult(false, "Invalid x coordinate", id, args)
        val y = parts[1].toFloatOrNull() ?: return ToolResult(false, "Invalid y coordinate", id, args)

        val ok = service.tap(x, y)
        return ToolResult(ok, if (ok) "Tapped ($x, $y)" else "Tap failed", id, args)
    }
}

class ScreenSwipeTool : Tool {
    override val id = "screen_swipe"
    override val name = "滑动屏幕"
    override val description = "从一个坐标滑动到另一个坐标"
    override val riskLevel = RiskLevel.L3_SYSTEM
    override val paramHint = "x1,y1,x2,y2"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val service = ScreenControlService.instance
            ?: return ToolResult(false, "Accessibility service not enabled", id, args)

        val parts = args.split(",").map { it.trim() }
        if (parts.size != 4) return ToolResult(false, "Format: x1,y1,x2,y2", id, args)
        val coords = parts.map { it.toFloatOrNull() }
        if (coords.any { it == null }) return ToolResult(false, "Invalid coordinates", id, args)

        val ok = service.swipe(coords[0]!!, coords[1]!!, coords[2]!!, coords[3]!!)
        return ToolResult(ok, if (ok) "Swiped" else "Swipe failed", id, args)
    }
}

class ScreenInputTool : Tool {
    override val id = "screen_input"
    override val name = "输入文字"
    override val description = "在当前焦点输入框输入文字"
    override val riskLevel = RiskLevel.L1_WRITE
    override val paramHint = "文字内容"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val service = ScreenControlService.instance
            ?: return ToolResult(false, "Accessibility service not enabled", id, args)

        val ok = service.inputText(args)
        return ToolResult(ok, if (ok) "Input: ${args.take(30)}" else "Input failed (no focused field?)", id, args)
    }
}

class ScreenBackTool : Tool {
    override val id = "screen_back"
    override val name = "返回"
    override val description = "按下返回键"
    override val riskLevel = RiskLevel.L3_SYSTEM
    override val paramHint = ""

    override suspend fun execute(args: String, context: Context): ToolResult {
        val service = ScreenControlService.instance
            ?: return ToolResult(false, "Accessibility service not enabled", id, args)
        val ok = service.pressBack()
        return ToolResult(ok, if (ok) "Back pressed" else "Back failed", id, args)
    }
}
