package com.pocketclaw.claw.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WebSearchTool : Tool {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "搜索互联网（通过DuckDuckGo）"
    override val riskLevel = RiskLevel.L0_READ
    override val paramHint = "query"

    override suspend fun execute(args: String, context: Context): ToolResult {
        val query = args.trim()
        if (query.isBlank()) return ToolResult(false, "Empty query", id, args)

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://lite.duckduckgo.com/lite/?q=$encoded")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "PocketClaw/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val html = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val results = parseResults(html)
                if (results.isBlank()) {
                    ToolResult(true, "No results found for: $query", id, args)
                } else {
                    ToolResult(true, results.take(800), id, args)
                }
            } catch (e: Exception) {
                ToolResult(false, "Search failed: ${e.message}", id, args)
            }
        }
    }

    private fun parseResults(html: String): String {
        val sb = StringBuilder()
        val snippetPattern = Regex("<td class=\"result-snippet\">(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
        val linkPattern = Regex("<a.*?class=\"result-link\".*?>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)

        val snippets = snippetPattern.findAll(html).map { it.groupValues[1].stripHtml() }.toList()
        val links = linkPattern.findAll(html).map { it.groupValues[1].stripHtml() }.toList()

        val count = minOf(snippets.size, links.size, 3)
        for (i in 0 until count) {
            sb.append("${i + 1}. ${links[i]}\n   ${snippets[i]}\n\n")
        }
        return sb.toString().trim()
    }

    private fun String.stripHtml(): String {
        return replace(Regex("<[^>]+>"), "").replace("&amp;", "&").replace("&lt;", "<")
            .replace("&gt;", ">").replace("&quot;", "\"").replace("&#x27;", "'").trim()
    }
}
