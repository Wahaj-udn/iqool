package com.example.geminiapi.analysis

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.nio.FloatBuffer

class DiabetesPredictor(private val context: Context) {
    private var environment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    init {
        try {
            // Using the new 8-feature model from the guide
            val modelBytes = context.assets.open("diabetes_model.onnx").use { it.readBytes() }
            session = environment.createSession(modelBytes)
            Log.d("DiabetesPredictor", "New 8-feature model loaded")
        } catch (e: Exception) {
            Log.e("DiabetesPredictor", "Failed to load model: ${e.message}")
        }
    }

    fun predict(inputArr: FloatArray): Pair<Float, Int> {
        val session = session ?: return Pair(0f, 0)
        
        val tensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputArr),
            longArrayOf(1, 8)
        )

        tensor.use {
            val inputs = mapOf(session.inputNames.first() to it)
            session.run(inputs).use { result ->
                // RandomForest usually returns Long for index 0
                val predictionArray = result[0].value as LongArray
                val prediction = predictionArray[0].toInt()
                
                // If probabilities are requested, they are at index 1
                var probability = 0f
                try {
                    val probs = result[1].value as Array<FloatArray>
                    probability = probs[0][1]
                } catch (e: Exception) {
                    probability = if (prediction == 1) 0.8f else 0.1f
                }
                
                return Pair(probability, prediction)
            }
        }
    }

    fun close() {
        session?.close()
        environment.close()
    }
}
