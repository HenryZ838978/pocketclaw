package com.pocketclaw.claw.tools

import android.content.Context
import android.os.Environment
import java.io.File

private val SANDBOX_ROOTS = listOf(
    Environment.getExternalStorageDirectory().absolutePath + "/PocketClaw",
    Environment.getExternalStorageDirectory().absolutePath + "/Download",
    Environment.getExternalStorageDirectory().absolutePath + "/Documents",
)

private fun isPathAllowed(path: String): Boolean {
    val canonical = File(path).canonicalPath
    return SANDBOX_ROOTS.any { canonical.startsWith(it) }
}

private fun ensurePocketClawDir() {
    val dir = File(Environment.getExternalStorageDirectory(), "PocketClaw")
    if (!dir.exists()) dir.mkdirs()
}

class FileReadTool : Tool {
    override val id = "file_read"
    override val name = "读取文件"
    override val description = "读取指定路径的文件内容"
    override val riskLevel = RiskLevel.L0_READ
    override val paramHint = "文件路径"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val path = args.trim()
        if (!isPathAllowed(path)) {
            return ToolResult(false, "Access denied: path outside sandbox", id, args)
        }
        val file = File(path)
        if (!file.exists()) {
            return ToolResult(false, "File not found: $path", id, args)
        }
        if (!file.isFile) {
            return ToolResult(false, "Not a file: $path", id, args)
        }
        val content = file.readText(Charsets.UTF_8)
        return ToolResult(true, content, id, args)
    }

    override fun summarize(raw: String, budget: Int): String {
        val lines = raw.lines()
        if (raw.length <= budget) return raw
        val taken = mutableListOf<String>()
        var chars = 0
        for (line in lines) {
            if (chars + line.length > budget - 30) break
            taken.add(line)
            chars += line.length + 1
        }
        return taken.joinToString("\n") + "\n...(共${lines.size}行)"
    }
}

class FileWriteTool : Tool {
    override val id = "file_write"
    override val name = "写入文件"
    override val description = "将内容写入指定路径的文件"
    override val riskLevel = RiskLevel.L1_WRITE
    override val paramHint = "路径:内容"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val sep = args.indexOf(':')
        if (sep < 0) return ToolResult(false, "Format: path:content", id, args)
        val path = args.substring(0, sep).trim()
        val content = args.substring(sep + 1)

        if (!isPathAllowed(path)) {
            return ToolResult(false, "Access denied: path outside sandbox", id, args)
        }
        ensurePocketClawDir()
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
        return ToolResult(true, "Written ${content.length} chars to $path", id, args)
    }
}

class FileListTool : Tool {
    override val id = "file_list"
    override val name = "列出目录"
    override val description = "列出指定目录下的文件和子目录"
    override val riskLevel = RiskLevel.L0_READ
    override val paramHint = "目录路径"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val path = args.trim()
        if (!isPathAllowed(path)) {
            return ToolResult(false, "Access denied: path outside sandbox", id, args)
        }
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            return ToolResult(false, "Not a directory: $path", id, args)
        }
        val entries = dir.listFiles()?.sortedBy { it.name }?.map { f ->
            val suffix = if (f.isDirectory) "/" else " (${f.length()} bytes)"
            "${f.name}$suffix"
        } ?: emptyList()
        return ToolResult(true, entries.joinToString("\n"), id, args)
    }

    override fun summarize(raw: String, budget: Int): String {
        val lines = raw.lines()
        if (raw.length <= budget) return raw
        return lines.take(20).joinToString("\n") + "\n...(共${lines.size}项)"
    }
}

class FileDeleteTool : Tool {
    override val id = "file_delete"
    override val name = "删除文件"
    override val description = "删除指定路径的文件"
    override val riskLevel = RiskLevel.L2_DESTRUCTIVE
    override val paramHint = "文件路径"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val path = args.trim()
        if (!isPathAllowed(path)) {
            return ToolResult(false, "Access denied: path outside sandbox", id, args)
        }
        val file = File(path)
        if (!file.exists()) {
            return ToolResult(false, "File not found: $path", id, args)
        }
        val deleted = file.delete()
        return ToolResult(deleted, if (deleted) "Deleted: $path" else "Failed to delete: $path", id, args)
    }
}
