#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"
#include "common.h"

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model* g_model = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_pocketclaw_app_voice_LlamaEngine_nativeInit(
    JNIEnv* env, jobject thiz, jstring model_path, jint n_gpu_layers) {

    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading llama model: %s", path);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = n_gpu_layers;

    g_model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) {
        LOGE("Failed to load llama model");
        return JNI_FALSE;
    }

    LOGI("Llama model loaded successfully");
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_pocketclaw_app_voice_LlamaEngine_nativeGenerate(
    JNIEnv* env, jobject thiz, jstring prompt_str, jint max_tokens, jfloat temperature) {

    if (!g_model) {
        LOGE("Model not loaded");
        return env->NewStringUTF("");
    }

    const char* prompt = env->GetStringUTFChars(prompt_str, nullptr);
    LOGI("Generating with prompt length: %zu", strlen(prompt));

    const llama_vocab * vocab = llama_model_get_vocab(g_model);

    // Tokenize
    int n_prompt = strlen(prompt);
    int n_tokens_max = n_prompt + 256;
    std::vector<llama_token> tokens(n_tokens_max);
    int n_tokens = llama_tokenize(vocab, prompt, n_prompt, tokens.data(), n_tokens_max, true, true);
    env->ReleaseStringUTFChars(prompt_str, prompt);

    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        return env->NewStringUTF("");
    }
    tokens.resize(n_tokens);
    LOGI("Tokenized: %d tokens", n_tokens);

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = n_tokens + max_tokens + 64;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;

    llama_context* ctx = llama_init_from_model(g_model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        return env->NewStringUTF("");
    }

    // Create sampler
    llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));

    // Eval prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(ctx, batch) != 0) {
        LOGE("Failed to decode prompt");
        llama_sampler_free(smpl);
        llama_free(ctx);
        return env->NewStringUTF("");
    }

    // Generate tokens
    std::string result;
    llama_token eos = llama_vocab_eos(vocab);

    for (int i = 0; i < max_tokens; i++) {
        llama_token new_token = llama_sampler_sample(smpl, ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) {
            result.append(buf, n);
        }

        llama_batch single = llama_batch_get_one(&new_token, 1);
        if (llama_decode(ctx, single) != 0) {
            LOGE("Decode failed at token %d", i);
            break;
        }
    }

    llama_sampler_free(smpl);
    llama_free(ctx);

    LOGI("Generated %zu chars", result.size());
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_pocketclaw_app_voice_LlamaEngine_nativeRelease(
    JNIEnv* env, jobject thiz) {

    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
        LOGI("Llama model released");
    }
}

}
