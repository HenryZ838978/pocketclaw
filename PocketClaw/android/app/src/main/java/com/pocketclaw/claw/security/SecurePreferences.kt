package com.pocketclaw.claw.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted SharedPreferences wrapper for sensitive data (API keys, etc.).
 * Falls back to regular SharedPreferences if encryption fails (old devices).
 */
object SecurePreferences {

    private const val FILE_NAME = "pocketclaw_secure_prefs"
    private var instance: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        return instance ?: synchronized(this) {
            instance ?: create(context).also { instance = it }
        }
    }

    private fun create(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            context.getSharedPreferences(FILE_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun putApiKey(context: Context, provider: String, key: String) {
        get(context).edit().putString("api_key_$provider", key).apply()
    }

    fun getApiKey(context: Context, provider: String): String? {
        return get(context).getString("api_key_$provider", null)
    }

    fun removeApiKey(context: Context, provider: String) {
        get(context).edit().remove("api_key_$provider").apply()
    }
}
