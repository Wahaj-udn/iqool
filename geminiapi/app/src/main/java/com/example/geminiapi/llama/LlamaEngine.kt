package com.example.geminiapi.llama

import android.util.Log

class LlamaEngine {
    private var isLibraryLoaded = false

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

    fun load(modelPath: String): Boolean {
        if (!ensureLibraryLoaded()) return false
        return loadModel(modelPath)
    }

    fun completion(prompt: String, onToken: (String) -> Unit): String {
        if (!isLibraryLoaded) return "Error: Native library not loaded"
        return doCompletion(prompt, onToken)
    }

    fun unload() {
        if (isLibraryLoaded) {
            unloadModel()
        }
    }

    private external fun loadModel(modelPath: String): Boolean
    private external fun unloadModel()
    private external fun doCompletion(prompt: String, callback: (String) -> Unit): String
}
