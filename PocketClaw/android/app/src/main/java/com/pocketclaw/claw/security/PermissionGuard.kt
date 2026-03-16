package com.pocketclaw.claw.security

import com.pocketclaw.claw.tools.RiskLevel
import com.pocketclaw.claw.tools.Tool
import java.util.concurrent.ConcurrentHashMap

/**
 * Controls whether tools are allowed to execute based on their risk level.
 * L0 (read-only): auto-approve after first grant.
 * L1 (write): auto-approve after first grant, show summary toast.
 * L2 (destructive): always require user confirmation.
 * L3 (system): always require confirmation + auto-revoke after 30s.
 */
class PermissionGuard {

    private val grantedTools = ConcurrentHashMap<String, Long>()

    private val SENSITIVE_PACKAGES = setOf(
        "com.google.android.apps.authenticator2",
        "com.android.settings",
        "com.google.android.gms",
    )

    private val BLOCKED_PATHS = listOf("/data/", "/system/", "/proc/", "/dev/")

    suspend fun checkPermission(
        tool: Tool,
        args: String,
        onConfirmNeeded: suspend (String) -> Boolean,
    ): Boolean {
        if (isPathBlocked(args)) return false

        return when (tool.riskLevel) {
            RiskLevel.L0_READ -> {
                grantedTools[tool.id] = System.currentTimeMillis()
                true
            }
            RiskLevel.L1_WRITE -> {
                if (grantedTools.containsKey(tool.id)) {
                    true
                } else {
                    val msg = "PocketClaw wants to use ${tool.name}: $args"
                    val granted = onConfirmNeeded(msg)
                    if (granted) grantedTools[tool.id] = System.currentTimeMillis()
                    granted
                }
            }
            RiskLevel.L2_DESTRUCTIVE -> {
                val msg = "PocketClaw wants to ${tool.name}: $args\nThis action may modify or delete data."
                onConfirmNeeded(msg)
            }
            RiskLevel.L3_SYSTEM -> {
                val msg = "PocketClaw wants system access: ${tool.name}($args)\nThis action controls your device."
                onConfirmNeeded(msg)
            }
        }
    }

    fun isSensitivePackage(packageName: String): Boolean {
        return packageName in SENSITIVE_PACKAGES
    }

    fun revokeAll() {
        grantedTools.clear()
    }

    private fun isPathBlocked(args: String): Boolean {
        return BLOCKED_PATHS.any { args.startsWith(it) }
    }
}
