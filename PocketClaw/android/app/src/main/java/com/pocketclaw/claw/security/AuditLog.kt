package com.pocketclaw.claw.security

import android.util.Log
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-memory audit log for tool executions.
 * Keeps the last MAX_ENTRIES records for debugging and user inspection.
 */
class AuditLog {

    companion object {
        private const val TAG = "AuditLog"
        private const val MAX_ENTRIES = 200
    }

    data class Entry(
        val timestamp: Long,
        val toolId: String,
        val args: String,
        val result: String,
        val success: Boolean,
    )

    private val entries = ConcurrentLinkedDeque<Entry>()

    fun record(toolId: String, args: String, result: String, success: Boolean) {
        val entry = Entry(
            timestamp = System.currentTimeMillis(),
            toolId = toolId,
            args = args.take(200),
            result = result.take(200),
            success = success,
        )
        entries.addFirst(entry)
        while (entries.size > MAX_ENTRIES) entries.removeLast()
        Log.d(TAG, "[${if (success) "OK" else "FAIL"}] $toolId(${args.take(40)}) -> ${result.take(60)}")
    }

    fun recent(count: Int = 20): List<Entry> = entries.take(count)

    fun clear() = entries.clear()
}
