package com.example.geminiapi.llama

import android.util.Log

class LlamaEngine {
    companion object {
        private var isLibraryLoaded = false
        private var isModelLoaded = false

        @Synchronized
        private fun ensureLibraryLoaded(): Boolean {
            if (isLibraryLoaded) return true
            return try {
                System.loadLibrary("llama-android")
                isLibraryLoaded = true
                Log.d("LlamaEngine", "Native library loaded successfully")
                true
            } catch (e: Throwable) {
                Log.e("LlamaEngine", "Failed to load native library: ${e.message}")
                false
            }
        }

        val shared by lazy { LlamaEngine() }
    }

    fun isLoaded(): Boolean = isModelLoaded

    @Synchronized
    fun load(modelPath: String): Boolean {
        if (isModelLoaded) return true
        if (!ensureLibraryLoaded()) return false
        val success = loadModel(modelPath)
        if (success) {
            isModelLoaded = true
        }
        return success
    }

    fun completion(prompt: String, thinkingEnabled: Boolean, onToken: (String) -> Unit): String {
        if (!ensureLibraryLoaded()) return "Error: Native library not loaded"
        return doCompletion(prompt, thinkingEnabled, onToken)
    }

    @Synchronized
    fun unload() {
        if (isLibraryLoaded && isModelLoaded) {
            unloadModel()
            isModelLoaded = false
        }
    }

    private external fun loadModel(modelPath: String): Boolean
    private external fun unloadModel()
    private external fun doCompletion(prompt: String, thinkingEnabled: Boolean, callback: (String) -> Unit): String
}
