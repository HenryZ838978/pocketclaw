package com.pocketclaw.claw.security

import android.os.Environment
import java.io.File

/**
 * Constrains file operations to safe directories.
 * Prevents the LLM from accessing system files, other app data, etc.
 */
object PathSandbox {

    private val ALLOWED_ROOTS = listOf(
        Environment.getExternalStorageDirectory().absolutePath + "/PocketClaw",
        Environment.getExternalStorageDirectory().absolutePath + "/Download",
        Environment.getExternalStorageDirectory().absolutePath + "/Documents",
    )

    private val BLOCKED_PREFIXES = listOf(
        "/data/",
        "/system/",
        "/proc/",
        "/dev/",
        "/sys/",
        "/vendor/",
    )

    fun isAllowed(path: String): Boolean {
        val canonical = try { File(path).canonicalPath } catch (_: Exception) { return false }
        if (BLOCKED_PREFIXES.any { canonical.startsWith(it) }) return false
        return ALLOWED_ROOTS.any { canonical.startsWith(it) }
    }

    fun ensureBaseDir() {
        val dir = File(Environment.getExternalStorageDirectory(), "PocketClaw")
        if (!dir.exists()) dir.mkdirs()
    }

    fun defaultDir(): String {
        return Environment.getExternalStorageDirectory().absolutePath + "/PocketClaw"
    }
}
