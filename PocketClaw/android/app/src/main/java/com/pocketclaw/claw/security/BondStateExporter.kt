package com.pocketclaw.claw.security

import android.content.Context
import android.net.Uri
import com.pocketclaw.claw.bond.BondEngine
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Export/Import Bond State as encrypted JSON for portability.
 * Uses AES-256-CBC with PBKDF2 key derivation from a user-provided passphrase.
 */
object BondStateExporter {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 10000
    private const val MAGIC = "PCLAW1" // file format identifier

    suspend fun exportToUri(
        context: Context,
        bondEngine: BondEngine,
        uri: Uri,
        passphrase: String,
    ): Boolean {
        return try {
            val json = bondEngine.exportBondState()
            val encrypted = encrypt(json, passphrase)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write("$MAGIC\n".toByteArray())
                os.write(encrypted.toByteArray())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun importFromUri(
        context: Context,
        bondEngine: BondEngine,
        uri: Uri,
        passphrase: String,
    ): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return false

            if (!content.startsWith(MAGIC)) return false
            val encrypted = content.substringAfter("$MAGIC\n")
            val json = decrypt(encrypted, passphrase)
            bondEngine.importBondState(json)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun encrypt(plaintext: String, passphrase: String): String {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val iv = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }

        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKey = SecretKeyFactory.getInstance(KEY_ALGORITHM).generateSecret(keySpec)
        val aesKey = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = salt + iv + encrypted
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String, passphrase: String): String {
        val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)

        val salt = combined.sliceArray(0 until 16)
        val iv = combined.sliceArray(16 until 32)
        val encrypted = combined.sliceArray(32 until combined.size)

        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKey = SecretKeyFactory.getInstance(KEY_ALGORITHM).generateSecret(keySpec)
        val aesKey = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
