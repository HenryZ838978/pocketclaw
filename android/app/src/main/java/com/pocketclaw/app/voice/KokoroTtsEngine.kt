package com.pocketclaw.app.voice

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

class KokoroTtsEngine(private val context: Context) {

    companion object {
        private const val TAG = "KokoroTTS"
        private const val MODEL_FILENAME = "kokoro-v0.19-int8.onnx"
        private const val VOICES_FILENAME = "voices.bin"
        private const val SAMPLE_RATE = 24000

        private val PHONEME_MAP = buildPhonemeMap()

        private fun buildPhonemeMap(): Map<Char, Long> {
            val chars = " !\"#\$%&'()*+,-./0123456789:;<=>?@" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
                "abcdefghijklmnopqrstuvwxyzæçðøħŋœǎǐǒǔ" +
                "ɐɑɒɔɕɗɘəɚɛɜɝɞɟɡɣɥɦɨɪɫɬɭɯɰɱɲɳɵɶɸɹɺɻɽɾʀʁʂʃʄʈʉʊʋʌʍʎʏʐʑʒʔʕʘʙʛʜʝʟʡʢ" +
                "ˈˌːˑ˞βθχᵻ"
            return chars.mapIndexed { index, c -> c to (index + 1).toLong() }.toMap()
        }
    }

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var voiceData: FloatArray? = null
    private var audioTrack: AudioTrack? = null

    val modelFile: File get() {
        val internal = File(context.filesDir, "models/$MODEL_FILENAME")
        if (internal.exists()) return internal
        val adbPushed = File("/data/local/tmp/pocketclaw_models/$MODEL_FILENAME")
        if (adbPushed.exists()) return adbPushed
        return internal
    }
    val voicesFile: File get() {
        val internal = File(context.filesDir, "models/$VOICES_FILENAME")
        if (internal.exists()) return internal
        val adbPushed = File("/data/local/tmp/pocketclaw_models/$VOICES_FILENAME")
        if (adbPushed.exists()) return adbPushed
        return internal
    }
    val isModelDownloaded: Boolean get() = modelFile.exists() && voicesFile.exists()
    val isReady: Boolean get() = session != null

    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (session != null) return@withContext true
        if (!isModelDownloaded) {
            Log.w(TAG, "Models not downloaded yet")
            return@withContext false
        }

        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
            }
            session = ortEnv!!.createSession(modelFile.absolutePath, opts)
            loadVoiceData()
            Log.i(TAG, "Kokoro TTS model loaded")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TTS model: ${e.message}", e)
            false
        }
    }

    private fun loadVoiceData() {
        val bytes = voicesFile.readBytes()
        val fb = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        voiceData = FloatArray(fb.remaining()).also { fb.get(it) }
        Log.i(TAG, "Voice data loaded: ${voiceData!!.size} floats")
    }

    suspend fun speak(text: String, voiceId: String = "af_heart"): FloatArray = withContext(Dispatchers.IO) {
        val sess = session ?: run {
            Log.e(TAG, "Session not loaded")
            return@withContext floatArrayOf()
        }

        val tokens = textToTokens(text)
        if (tokens.isEmpty()) return@withContext floatArrayOf()

        try {
            val env = ortEnv!!
            val inputIds = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(tokens),
                longArrayOf(1, tokens.size.toLong())
            )

            val styleVec = getVoiceStyle(voiceId, tokens.size)
            val styleTensor = OnnxTensor.createTensor(env, styleVec)

            val speedTensor = OnnxTensor.createTensor(
                env,
                java.nio.FloatBuffer.wrap(floatArrayOf(1.0f)),
                longArrayOf(1)
            )

            val inputs = mapOf(
                "input_ids" to inputIds,
                "style" to styleTensor,
                "speed" to speedTensor
            )

            val results = sess.run(inputs)
            val output = results[0].value as Array<*>
            val audio = (output[0] as FloatArray)

            inputIds.close()
            styleTensor.close()
            speedTensor.close()
            results.close()

            audio
        } catch (e: Exception) {
            Log.e(TAG, "TTS inference failed: ${e.message}", e)
            floatArrayOf()
        }
    }

    suspend fun speakAndPlay(text: String, voiceId: String = "af_heart") {
        val audio = speak(text, voiceId)
        if (audio.isEmpty()) return
        playAudio(audio)
    }

    private fun playAudio(samples: FloatArray) {
        stopPlayback()

        val bufferSize = samples.size * 4
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        track.play()
        audioTrack = track
    }

    fun stopPlayback() {
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        audioTrack = null
    }

    private fun textToTokens(text: String): LongArray {
        val tokens = mutableListOf<Long>()
        for (ch in text) {
            val id = PHONEME_MAP[ch]
            if (id != null) tokens.add(id)
        }
        return tokens.toLongArray()
    }

    private fun getVoiceStyle(voiceId: String, tokenLen: Int): Array<FloatArray> {
        val styleSize = 256
        val style = FloatArray(styleSize) { 0.0f }

        voiceData?.let { data ->
            if (data.size >= styleSize) {
                System.arraycopy(data, 0, style, 0, styleSize)
            }
        }

        return arrayOf(style)
    }

    fun release() {
        stopPlayback()
        session?.close()
        session = null
        ortEnv?.close()
        ortEnv = null
        voiceData = null
    }
}
