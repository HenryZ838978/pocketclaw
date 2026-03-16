package com.pocketclaw.claw.tools

import android.content.Context
import android.util.Log
import com.pocketclaw.claw.security.AuditLog
import com.pocketclaw.claw.security.PermissionGuard
import com.pocketclaw.claw.security.RateLimiter

/**
 * Executes tool calls with security checks (PermissionGuard, RateLimiter, AuditLog).
 */
class ToolExecutor(
    private val context: Context,
    private val permissionGuard: PermissionGuard,
    private val auditLog: AuditLog,
) {
    companion object {
        private const val TAG = "ToolExecutor"
    }

    private val rateLimiter = RateLimiter()

    /**
     * Execute a parsed tool call. Returns null if blocked by security.
     * The [onConfirmNeeded] callback is invoked for high-risk actions and must
     * suspend until the user confirms or denies.
     */
    suspend fun execute(
        call: ToolParser.ParsedCall,
        onConfirmNeeded: suspend (String) -> Boolean = { true },
    ): ToolResult {
        val tool = ToolRegistry.get(call.toolId)
        if (tool == null) {
            val result = ToolResult(false, "Unknown tool: ${call.toolId}", call.toolId, call.args)
            auditLog.record(call.toolId, call.args, "UNKNOWN_TOOL", false)
            return result
        }

        if (!rateLimiter.allow(call.toolId)) {
            val result = ToolResult(false, "Rate limit exceeded for ${call.toolId}", call.toolId, call.args)
            auditLog.record(call.toolId, call.args, "RATE_LIMITED", false)
            return result
        }

        val granted = permissionGuard.checkPermission(tool, call.args, onConfirmNeeded)
        if (!granted) {
            val result = ToolResult(false, "Permission denied for ${call.toolId}", call.toolId, call.args)
            auditLog.record(call.toolId, call.args, "DENIED", false)
            return result
        }

        return try {
            Log.d(TAG, "Executing tool: ${call.toolId}(${call.args.take(80)})")
            val result = tool.execute(call.args, context)
            auditLog.record(call.toolId, call.args, result.output.take(200), result.success)
            if (result.success) rateLimiter.recordSuccess() else rateLimiter.recordFailure()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution failed: ${e.message}", e)
            rateLimiter.recordFailure()
            val result = ToolResult(false, "Error: ${e.message}", call.toolId, call.args)
            auditLog.record(call.toolId, call.args, "EXCEPTION: ${e.message}", false)
            result
        }
    }

    fun resetTurn() {
        rateLimiter.resetTurn()
    }
}
