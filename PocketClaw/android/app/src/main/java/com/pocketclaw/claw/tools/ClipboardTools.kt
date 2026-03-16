package com.pocketclaw.claw.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardReadTool : Tool {
    override val id = "clipboard_read"
    override val name = "Read Clipboard"
    override val description = "读取剪贴板内容"
    override val riskLevel = RiskLevel.L0_READ
    override val paramHint = "(无参数)"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        val text = clip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
        return ToolResult(true, if (text.isBlank()) "(clipboard empty)" else text.take(500), id, args)
    }
}

class ClipboardWriteTool : Tool {
    override val id = "clipboard_write"
    override val name = "Write Clipboard"
    override val description = "写入文本到剪贴板"
    override val riskLevel = RiskLevel.L1_WRITE
    override val paramHint = "text"

    override suspend fun execute(args: String, context: Context): ToolResult {
        if (args.isBlank()) return ToolResult(false, "Empty content", id, args)
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("PocketClaw", args))
        return ToolResult(true, "Copied ${args.length} chars to clipboard", id, args)
    }
}
