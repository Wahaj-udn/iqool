#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"
#include "common.h"

#define TAG "LLAMA_ANDROID"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model * model = nullptr;
static llama_context * ctx = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_geminiapi_llama_LlamaEngine_loadModel(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);

    llama_backend_init();

    auto mparams = llama_model_default_params();
    model = llama_load_model_from_file(path, mparams);

    if (!model) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    auto cparams = llama_context_default_params();
    cparams.n_ctx = 2048; // Set context size
    cparams.n_threads = 4; // Adjust based on device cores

    ctx = llama_new_context_with_model(model, cparams);
    if (!ctx) {
        LOGE("Failed to create context");
        llama_free_model(model);
        model = nullptr;
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }

    LOGD("Model loaded successfully from %s", path);
    env->ReleaseStringUTFChars(model_path, path);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_geminiapi_llama_LlamaEngine_unloadModel(JNIEnv *env, jobject thiz) {
    if (ctx) {
        llama_free(ctx);
        ctx = nullptr;
    }
    if (model) {
        llama_free_model(model);
        model = nullptr;
    }
    llama_backend_free();
    LOGD("Model unloaded");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_geminiapi_llama_LlamaEngine_doCompletion(JNIEnv *env, jobject thiz, jstring prompt) {
    if (!ctx) return env->NewStringUTF("Error: Model not loaded");

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);

    // Very basic completion logic for verification
    // In a real app, this would use a proper loop with streaming callbacks
    std::string response = "Inference placeholder for: ";
    response += prompt_str;

    env->ReleaseStringUTFChars(prompt, prompt_str);
    return env->NewStringUTF(response.c_str());
}
