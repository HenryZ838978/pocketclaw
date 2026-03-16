package com.pocketclaw.claw.tools

import android.content.Context
import android.content.Intent

class AppLaunchTool : Tool {
    override val id = "app_launch"
    override val name = "Launch App"
    override val description = "启动手机上的应用"
    override val riskLevel = RiskLevel.L1_WRITE
    override val paramHint = "package_name"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val pkg = args.trim()
        if (pkg.isBlank()) return ToolResult(false, "No package name", id, args)

        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return ToolResult(false, "App not found: $pkg", id, args)

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult(true, "Launched $pkg", id, args)
        } catch (e: Exception) {
            ToolResult(false, "Launch failed: ${e.message}", id, args)
        }
    }
}
