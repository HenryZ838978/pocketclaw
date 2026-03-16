package com.pocketclaw.claw.tools

import android.content.Context
import com.pocketclaw.app.messaging.MessageBridge

class TelegramSendTool : Tool {
    override val id = "tg_send"
    override val name = "发送Telegram消息"
    override val description = "通过Telegram Bot发送消息"
    override val riskLevel = RiskLevel.L2_DESTRUCTIVE
    override val paramHint = "消息内容"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val channel = MessageBridge.get("telegram")
        if (channel == null || !channel.isConnected) {
            return ToolResult(false, "Telegram not connected. Set up in Settings.", id, args)
        }
        val ok = channel.sendMessage(args)
        return ToolResult(ok, if (ok) "Sent via Telegram" else "Send failed", id, args)
    }
}
