package com.example.geminiapi.analysis

import android.content.Context
import ai.onnxruntime.*
import android.util.Log
import java.nio.FloatBuffer

class HypertensionPredictor(private val context: Context) {
    private var environment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    fun loadModel() {
        if (session != null) return
        try {
            // Lazy load large model (221 MB)
            val modelBytes = context.assets.open("hypertension_model.onnx").use { it.readBytes() }
            session = environment.createSession(modelBytes)
            Log.d("HypertensionPredictor", "Large 221MB model loaded lazily")
        } catch (e: Exception) {
            Log.e("HypertensionPredictor", "Failed to load model: ${e.message}")
        }
    }

    fun predict(inputArr: FloatArray): Long {
        loadModel() // Ensure loaded
        val session = session ?: return -1L

        val tensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputArr),
            longArrayOf(1, 44)
        )

        tensor.use {
            val inputs = mapOf(session.inputNames.first() to it)
            session.run(inputs).use { result ->
                val predictionArray = result[0].value as LongArray
                return predictionArray[0]
            }
        }
    }

    fun close() {
        session?.close()
        environment.close()
    }
}
