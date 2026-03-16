package com.pocketclaw.app.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhisperEngine(private val context: Context) {

    companion object {
        private const val TAG = "WhisperSTT"
        private const val SAMPLE_RATE = 16000
        private const val MODEL_FILENAME = "ggml-small-q5_1.bin"

        init {
            System.loadLibrary("pocketclaw_whisper")
        }
    }

    private var isLoaded = false
    private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording = false

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
            Log.w(TAG, "Model not downloaded yet")
            return@withContext false
        }
        isLoaded = nativeInit(modelFile.absolutePath)
        Log.i(TAG, "Model loaded: $isLoaded")
        isLoaded
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun recordAndTranscribe(maxDurationMs: Long = 10_000): String = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            Log.e(TAG, "Model not loaded")
            return@withContext ""
        }
        if (!hasRecordPermission()) {
            Log.e(TAG, "No RECORD_AUDIO permission")
            return@withContext ""
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(SAMPLE_RATE * 4)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            bufferSize
        )

        audioRecord = recorder
        val allSamples = mutableListOf<Float>()
        val buffer = FloatArray(SAMPLE_RATE)

        recorder.startRecording()
        isRecording = true
        Log.i(TAG, "Recording started")

        val startTime = System.currentTimeMillis()
        while (isRecording && (System.currentTimeMillis() - startTime) < maxDurationMs) {
            val read = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            if (read > 0) {
                for (i in 0 until read) allSamples.add(buffer[i])
            }
        }

        recorder.stop()
        recorder.release()
        audioRecord = null
        isRecording = false
        Log.i(TAG, "Recording stopped, ${allSamples.size} samples")

        if (allSamples.isEmpty()) return@withContext ""
        nativeTranscribe(allSamples.toFloatArray())
    }

    fun stopRecording() {
        isRecording = false
    }

    fun release() {
        stopRecording()
        nativeRelease()
        isLoaded = false
    }

    private external fun nativeInit(modelPath: String): Boolean
    private external fun nativeTranscribe(audioData: FloatArray): String
    private external fun nativeRelease()
}
