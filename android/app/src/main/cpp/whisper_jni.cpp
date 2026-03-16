#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "whisper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static whisper_context* g_ctx = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_pocketclaw_app_voice_WhisperEngine_nativeInit(
    JNIEnv* env, jobject thiz, jstring model_path) {

    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading whisper model: %s", path);

    whisper_context_params cparams = whisper_context_default_params();
    g_ctx = whisper_init_from_file_with_params(path, cparams);

    env->ReleaseStringUTFChars(model_path, path);

    if (!g_ctx) {
        LOGE("Failed to load whisper model");
        return JNI_FALSE;
    }

    LOGI("Whisper model loaded successfully");
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_pocketclaw_app_voice_WhisperEngine_nativeTranscribe(
    JNIEnv* env, jobject thiz, jfloatArray audio_data) {

    if (!g_ctx) {
        LOGE("Whisper context not initialized");
        return env->NewStringUTF("");
    }

    jsize n_samples = env->GetArrayLength(audio_data);
    jfloat* samples = env->GetFloatArrayElements(audio_data, nullptr);

    LOGI("Transcribing %d samples", n_samples);

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress   = false;
    wparams.print_special    = false;
    wparams.print_realtime   = false;
    wparams.print_timestamps = false;
    wparams.single_segment   = true;
    wparams.no_timestamps    = true;
    wparams.language         = "auto";
    wparams.n_threads        = 4;

    int result = whisper_full(g_ctx, wparams, samples, n_samples);
    env->ReleaseFloatArrayElements(audio_data, samples, 0);

    if (result != 0) {
        LOGE("Whisper transcription failed: %d", result);
        return env->NewStringUTF("");
    }

    std::string text;
    int n_segments = whisper_full_n_segments(g_ctx);
    for (int i = 0; i < n_segments; i++) {
        text += whisper_full_get_segment_text(g_ctx, i);
    }

    LOGI("Transcribed: %s", text.c_str());
    return env->NewStringUTF(text.c_str());
}

JNIEXPORT void JNICALL
Java_com_pocketclaw_app_voice_WhisperEngine_nativeRelease(
    JNIEnv* env, jobject thiz) {

    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
        LOGI("Whisper context released");
    }
}

}
