package com.pocketclaw.claw.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Prevents tool call loops: max calls per conversation turn,
 * per-tool cooldown, and consecutive failure limit.
 */
class RateLimiter {

    companion object {
        const val MAX_CALLS_PER_TURN = 5
        const val COOLDOWN_MS = 10_000L
        const val MAX_CONSECUTIVE_FAILURES = 3
    }

    private val lastCall = ConcurrentHashMap<String, Long>()
    private var callsThisTurn = 0
    private var consecutiveFailures = 0

    fun allow(toolId: String): Boolean {
        if (callsThisTurn >= MAX_CALLS_PER_TURN) return false
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) return false

        val now = System.currentTimeMillis()
        val last = lastCall[toolId] ?: 0L
        if (now - last < COOLDOWN_MS) return false

        lastCall[toolId] = now
        callsThisTurn++
        return true
    }

    fun recordFailure() {
        consecutiveFailures++
    }

    fun recordSuccess() {
        consecutiveFailures = 0
    }

    fun resetTurn() {
        callsThisTurn = 0
        consecutiveFailures = 0
    }
}
