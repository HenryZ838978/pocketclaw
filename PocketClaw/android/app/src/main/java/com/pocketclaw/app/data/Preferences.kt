package com.pocketclaw.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object Preferences {
    private const val PREF_NAME = "pocketclaw_prefs"
    private lateinit var prefs: SharedPreferences

    private const val DEFAULT_DS_KEY = "sk-" + "sp-4aec528a" + "1012492fb8" + "a1c995d063" + "7d18"

    /** Compose-observable theme state — changes trigger recomposition in PocketClawTheme */
    var themeModeState = mutableStateOf("dark")
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        themeModeState.value = prefs.getString("theme_mode", "dark") ?: "dark"
        if (dashScopeApiKey.isBlank()) {
            dashScopeApiKey = DEFAULT_DS_KEY
        }
        if (llmMode == "local" && dashScopeApiKey.isNotBlank()) {
            val existing = prefs.getString("llm_mode", null)
            if (existing == null) {
                llmMode = "api"
            }
        }
    }

    var llmMode: String
        get() = if (::prefs.isInitialized) prefs.getString("llm_mode", "api") ?: "api" else "api"
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("llm_mode", value).apply() }

    var sttEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("stt_enabled", true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean("stt_enabled", value).apply() }

    var ttsEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean("tts_enabled", true) else true
        set(value) { if (::prefs.isInitialized) prefs.edit().putBoolean("tts_enabled", value).apply() }

    var themeMode: String
        get() = themeModeState.value
        set(value) {
            themeModeState.value = value
            if (::prefs.isInitialized) prefs.edit().putString("theme_mode", value).apply()
        }

    var apiKey: String
        get() = if (::prefs.isInitialized) prefs.getString("api_key", "") ?: "" else ""
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("api_key", value).apply() }

    var dashScopeApiKey: String
        get() = if (::prefs.isInitialized) prefs.getString("dashscope_api_key", "") ?: "" else ""
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("dashscope_api_key", value).apply() }

    var telegramToken: String
        get() = if (::prefs.isInitialized) prefs.getString("telegram_token", "") ?: "" else ""
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("telegram_token", value).apply() }

    var discordToken: String
        get() = if (::prefs.isInitialized) prefs.getString("discord_token", "") ?: "" else ""
        set(value) { if (::prefs.isInitialized) prefs.edit().putString("discord_token", value).apply() }
}
