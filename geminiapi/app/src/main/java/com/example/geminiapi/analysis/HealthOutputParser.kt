package com.example.geminiapi.analysis

import ai.onnxruntime.OrtSession

object HealthOutputParser {

    fun parseDiabetes(output: OrtSession.Result): ModelResult {
        val classId = (output[0].value as LongArray)[0].toInt()
        val probs = (output[1].value as Array<FloatArray>)[0]
        val label = if (classId == 1) "At Risk" else "Healthy"
        val riskScore = probs[1] * 100f  // Probability of class 1 * 100
        return ModelResult(classId, label, riskScore)
    }

    fun parseHeart(output: OrtSession.Result): ModelResult {
        val classId = (output[0].value as LongArray)[0].toInt()
        val probs = (output[1].value as Array<FloatArray>)[0]
        // Guide: Class 1 = No Risk, Class 0 = Risk
        val label = if (classId == 1) "Healthy" else "At Risk"
        val riskScore = probs[0] * 100f  // Probability of class 0 (Elevated Risk) * 100
        return ModelResult(classId, label, riskScore)
    }

    fun parseHypertension(output: OrtSession.Result): ModelResult {
        val classId = (output[0].value as LongArray)[0].toInt()
        val probs = (output[1].value as Array<FloatArray>)[0]
        // Guide: Class 1 = Normal, Class 0 = High Risk
        val label = if (classId == 0) "At Risk" else "Healthy"
        val riskScore = probs[0] * 100f  // Probability of class 0 (High Risk) * 100
        return ModelResult(classId, label, riskScore)
    }

    fun parseObesity(output: OrtSession.Result): ModelResult {
        val classId = (output[0].value as LongArray)[0].toInt()
        val probs = (output[1].value as Array<FloatArray>)[0]
        val labels = arrayOf(
            "Insufficient Weight",
            "Normal Weight",
            "Obesity Type I",
            "Obesity Type II",
            "Obesity Type III",
            "Overweight Level I",
            "Overweight Level II"
        )
        val label = labels[classId]
        val confidence = probs[classId] * 100f
        return ModelResult(classId, label, confidence)
    }
}
