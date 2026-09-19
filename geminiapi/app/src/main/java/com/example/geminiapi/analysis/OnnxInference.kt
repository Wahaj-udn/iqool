package com.example.geminiapi.analysis

import android.content.Context
import ai.onnxruntime.*
import android.util.Log
import java.nio.FloatBuffer

data class ModelResult(
    val classId: Int,
    val label: String,
    val confidence: Float
)

object OnnxInference {
    private val env = OrtEnvironment.getEnvironment()

    fun loadSession(context: Context, assetName: String): OrtSession? {
        return try {
            val modelBytes = context.assets.open(assetName).readBytes()
            env.createSession(modelBytes, OrtSession.SessionOptions())
        } catch (e: Exception) {
            Log.e("OnnxInference", "Error loading session for $assetName: ${e.message}")
            null
        }
    }

    fun runInference(session: OrtSession, inputFloats: FloatArray): Long {
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(1L, inputFloats.size.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputFloats), shape)
        
        return try {
            session.run(mapOf(inputName to tensor)).use { output ->
                val result = output[0].value as LongArray
                result[0]
            }
        } finally {
            tensor.close()
        }
    }

    fun runFullInference(session: OrtSession, inputFloats: FloatArray): OrtSession.Result {
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(1L, inputFloats.size.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputFloats), shape)
        
        return try {
            session.run(mapOf(inputName to tensor))
        } finally {
            tensor.close()
        }
    }
}
