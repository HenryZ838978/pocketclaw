package com.pocketclaw.app.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlamaEngine(private val context: Context) {

    companion object {
        private const val TAG = "LlamaLLM"
        private const val MODEL_FILENAME = "qwen3-1.7b-q4_k_m.gguf"

        init {
            System.loadLibrary("pocketclaw_llama")
        }
    }

    private var isLoaded = false

    val modelFile: File get() {
        val internal = File(context.filesDir, "models/$MODEL_FILENAME")
        if (internal.exists()) return internal
        val adbPushed = File("/data/local/tmp/pocketclaw_models/$MODEL_FILENAME")
        if (adbPushed.exists()) return adbPushed
        return internal
    }
    val isModelDownloaded: Boolean get() = modelFile.exists()
    val isReady: Boolean get() = isLoaded

    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true
        if (!isModelDownloaded) {
            Log.w(TAG, "Model not downloaded: ${modelFile.absolutePath}")
            return@withContext false
        }
        Log.i(TAG, "Loading model: ${modelFile.absolutePath} (${modelFile.length() / 1_048_576}MB)")
        isLoaded = nativeInit(modelFile.absolutePath, 0)
        Log.i(TAG, "Model loaded: $isLoaded")
        isLoaded
    }

    suspend fun generate(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f,
    ): String = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            Log.e(TAG, "Model not loaded")
            return@withContext ""
        }
        nativeGenerate(prompt, maxTokens, temperature)
    }

    fun release() {
        nativeRelease()
        isLoaded = false
    }

    private external fun nativeInit(modelPath: String, nGpuLayers: Int): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int, temperature: Float): String
    private external fun nativeRelease()
}
