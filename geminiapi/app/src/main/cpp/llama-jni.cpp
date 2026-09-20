#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <chrono>
#include "llama.h"
#include "common.h"

#define TAG "LLAMA_ANDROID"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model * model = nullptr;
static llama_context * ctx = nullptr;
static llama_sampler * smpl = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_geminiapi_llama_LlamaEngine_loadModel(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);

    llama_backend_init();

    auto mparams = llama_model_default_params();
    model = llama_model_load_from_file(path, mparams);

    if (!model) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 2048;
    cparams.n_threads = 4;

    ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_model_free(model);
        model = nullptr;
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    LOGD("Model loaded successfully. Context: 2048, Threads: 4");
    env->ReleaseStringUTFChars(model_path, path);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_geminiapi_llama_LlamaEngine_unloadModel(JNIEnv *env, jobject thiz) {
    if (smpl) { llama_sampler_free(smpl); smpl = nullptr; }
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    llama_backend_free();
    LOGD("Resources unloaded");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_geminiapi_llama_LlamaEngine_doCompletion(JNIEnv *env, jobject thiz, jstring prompt) {
    if (!ctx || !model) return env->NewStringUTF("Error: Engine not ready");

    // Clear previous memory context
    llama_memory_t mem = llama_get_memory(ctx);
    llama_memory_clear(mem, true);

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
    const struct llama_vocab * vocab = llama_model_get_vocab(model);

    // 1. Tokenization
    std::vector<llama_token> tokens;
    tokens.push_back(llama_vocab_bos(vocab));
    int n_tokens = -llama_tokenize(vocab, prompt_str, (int32_t)strlen(prompt_str), nullptr, 0, false, true);
    tokens.resize(n_tokens + 1);
    llama_tokenize(vocab, prompt_str, (int32_t)strlen(prompt_str), tokens.data() + 1, (int32_t)tokens.size() - 1, false, true);

    LOGD("Prompt: %d tokens", (int)tokens.size());
    auto t_start = std::chrono::high_resolution_clock::now();

    // 2. Process prompt
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); ++i) {
        batch.token[i] = tokens[i];
        batch.pos[i] = (llama_pos)i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == tokens.size() - 1);
    }
    batch.n_tokens = (int32_t)tokens.size();

    if (llama_decode(ctx, batch) != 0) {
        llama_batch_free(batch);
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return env->NewStringUTF("Error: Initial decode failed");
    }

    // 3. Generation loop
    std::string result_text;
    int max_tokens = 60;
    int count = 0;

    for (int i = 0; i < max_tokens; ++i) {
        llama_token next = llama_sampler_sample(smpl, ctx, -1);
        if (llama_vocab_is_eog(vocab, next)) break;

        char buf[256];
        int n = llama_token_to_piece(vocab, next, buf, sizeof(buf), 0, false);
        if (n < 0) break;
        result_text.append(buf, n);
        count++;

        batch.token[0] = next;
        batch.pos[0] = (llama_pos)(tokens.size() + i);
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        batch.n_tokens = 1;

        if (llama_decode(ctx, batch) != 0) break;
    }

    auto t_end = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double> duration = t_end - t_start;
    LOGD("Inference done: %d tokens in %.2fs (%.2f t/s)", count, duration.count(), count / duration.count());

    llama_batch_free(batch);
    env->ReleaseStringUTFChars(prompt, prompt_str);

    if (result_text.empty()) return env->NewStringUTF("AI returned empty result.");
    return env->NewStringUTF(result_text.c_str());
}
