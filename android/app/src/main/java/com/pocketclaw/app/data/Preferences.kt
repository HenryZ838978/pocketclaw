package com.pocketclaw.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

object Preferences {

    private const val PREFS_NAME = "pocketclaw_prefs"

    private lateinit var prefs: SharedPreferences

    private val _themeMode = mutableStateOf("dark")

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "dark") ?: "dark"
    }

    var themeMode: String
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
            prefs.edit().putString("theme_mode", value).apply()
        }

    var llmMode: String
        get() = prefs.getString("llm_mode", "local") ?: "local"
        set(value) = prefs.edit().putString("llm_mode", value).apply()

    var dashScopeApiKey: String
        get() = prefs.getString("dashscope_api_key", "") ?: ""
        set(value) = prefs.edit().putString("dashscope_api_key", value).apply()

    var dashScopeBaseUrl: String
        get() = prefs.getString("dashscope_base_url", "https://coding.dashscope.aliyuncs.com/v1") ?: "https://coding.dashscope.aliyuncs.com/v1"
        set(value) = prefs.edit().putString("dashscope_base_url", value).apply()

    var dashScopeModel: String
        get() = prefs.getString("dashscope_model", "MiniMax-M2.5") ?: "MiniMax-M2.5"
        set(value) = prefs.edit().putString("dashscope_model", value).apply()

    var sttEnabled: Boolean
        get() = prefs.getBoolean("stt_enabled", true)
        set(value) = prefs.edit().putBoolean("stt_enabled", value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("tts_enabled", true)
        set(value) = prefs.edit().putBoolean("tts_enabled", value).apply()

    var ttsVoice: String
        get() = prefs.getString("tts_voice", "af_heart") ?: "af_heart"
        set(value) = prefs.edit().putString("tts_voice", value).apply()
}
