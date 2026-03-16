package com.pocketclaw.app.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ModelInfo(
    val name: String,
    val filename: String,
    val url: String,
    val sizeBytes: Long,
    val description: String,
)

data class DownloadProgress(
    val model: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val error: String? = null,
) {
    val fraction: Float get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    val megabytesDownloaded: Float get() = bytesDownloaded / 1_048_576f
    val megabytesTotal: Float get() = totalBytes / 1_048_576f
}

data class OverallDownloadState(
    val isDownloading: Boolean = false,
    val currentModel: String = "",
    val modelsComplete: Int = 0,
    val modelsTotal: Int = 0,
    val currentProgress: DownloadProgress? = null,
    val allComplete: Boolean = false,
    val error: String? = null,
)

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDL"

        val WHISPER_MODEL = ModelInfo(
            name = "Whisper Small (Q5)",
            filename = "ggml-small-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
            sizeBytes = 190_000_000,
            description = "Speech recognition — on-device, private",
        )

        val KOKORO_MODEL = ModelInfo(
            name = "Kokoro TTS",
            filename = "kokoro-v0.19-int8.onnx",
            url = "https://huggingface.co/onnx-community/Kokoro-82M-ONNX/resolve/main/onnx/model_uint8.onnx",
            sizeBytes = 177_000_000,
            description = "Voice synthesis — on-device, natural speech",
        )

        val KOKORO_VOICES = ModelInfo(
            name = "Kokoro Voices",
            filename = "voices.bin",
            url = "https://huggingface.co/onnx-community/Kokoro-82M-ONNX/resolve/main/voices/af.bin",
            sizeBytes = 524_000,
            description = "Voice style data for TTS",
        )

        val QWEN_MODEL = ModelInfo(
            name = "Qwen3-1.7B (Brain)",
            filename = "qwen3-1.7b-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen3-1.7B-GGUF/resolve/main/qwen3-1.7b-q4_k_m.gguf",
            sizeBytes = 1_100_000_000,
            description = "On-device AI brain — private, no server needed",
        )

        val ALL_MODELS = listOf(QWEN_MODEL, WHISPER_MODEL, KOKORO_MODEL, KOKORO_VOICES)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val modelsDir = File(context.filesDir, "models")
    private val adbModelsDir = File("/data/local/tmp/pocketclaw_models")

    private val _state = MutableStateFlow(OverallDownloadState())
    val state: StateFlow<OverallDownloadState> = _state.asStateFlow()

    fun isModelDownloaded(model: ModelInfo): Boolean {
        return File(modelsDir, model.filename).exists()
            || File(adbModelsDir, model.filename).exists()
    }

    fun allModelsDownloaded(): Boolean {
        return ALL_MODELS.all { isModelDownloaded(it) }
    }

    fun brainReady(): Boolean {
        return isModelDownloaded(QWEN_MODEL)
    }

    fun pendingModels(): List<ModelInfo> {
        return ALL_MODELS.filter { !isModelDownloaded(it) }
    }

    suspend fun downloadAllPending() = withContext(Dispatchers.IO) {
        val pending = pendingModels()
        if (pending.isEmpty()) {
            _state.value = OverallDownloadState(allComplete = true)
            return@withContext
        }

        modelsDir.mkdirs()

        _state.value = OverallDownloadState(
            isDownloading = true,
            modelsTotal = pending.size,
        )

        for ((index, model) in pending.withIndex()) {
            _state.value = _state.value.copy(
                currentModel = model.name,
                modelsComplete = index,
            )

            val success = downloadModel(model)
            if (!success) {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    error = "Failed to download ${model.name}",
                )
                return@withContext
            }
        }

        _state.value = OverallDownloadState(
            isDownloading = false,
            modelsComplete = pending.size,
            modelsTotal = pending.size,
            allComplete = true,
        )
        Log.i(TAG, "All models downloaded")
    }

    private suspend fun downloadModel(model: ModelInfo): Boolean = withContext(Dispatchers.IO) {
        val targetFile = File(modelsDir, model.filename)
        val tempFile = File(modelsDir, "${model.filename}.tmp")

        try {
            Log.i(TAG, "Downloading ${model.name}: ${model.url}")

            val request = Request.Builder()
                .url(model.url)
                .header("User-Agent", "PocketClaw/0.2")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength().let { if (it > 0) it else model.sizeBytes }

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalRead += read

                        _state.value = _state.value.copy(
                            currentProgress = DownloadProgress(
                                model = model.name,
                                bytesDownloaded = totalRead,
                                totalBytes = contentLength,
                            ),
                        )
                    }
                }
            }

            tempFile.renameTo(targetFile)
            Log.i(TAG, "Downloaded ${model.name}: ${targetFile.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${model.name}: ${e.message}", e)
            tempFile.delete()
            false
        }
    }
}
