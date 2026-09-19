package com.example.geminiapi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.analysis.*
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HealthAssessmentViewModel(application: Application) : AndroidViewModel(application) {
    private val _results = MutableStateFlow(AssessmentResults())
    val results: StateFlow<AssessmentResults> = _results.asStateFlow()

    private var diabetesSession: OrtSession? = OnnxInference.loadSession(application, "diabetes_model.onnx")
    private var heartSession: OrtSession? = OnnxInference.loadSession(application, "heart_model.onnx")
    private var hypertensionSession: OrtSession? = null 
    private var obesitySession: OrtSession? = OnnxInference.loadSession(application, "obesity_model.onnx")

    fun runAllPredictions() {
        val p = HealthFeatureStore.profile.value
        viewModelScope.launch {
            _results.value = _results.value.copy(isCalculating = true)
            
            if (hypertensionSession == null) {
                withContext(Dispatchers.IO) {
                    hypertensionSession = OnnxInference.loadSession(getApplication(), "hypertension_model.onnx")
                }
            }

            val diabetesRes = runDiabetes(p)
            val heartRes = runHeart(p)
            val hypertensionRes = runHypertension(p)
            val obesityRes = runObesity(p)

            val finalResults = AssessmentResults(
                diabetes = diabetesRes,
                heart = heartRes,
                hypertension = hypertensionRes,
                obesity = obesityRes,
                isCalculating = false
            )
            
            _results.value = finalResults
            // Save to store for Gemini context
            HealthFeatureStore.setResults(finalResults)
        }
    }

    private suspend fun runDiabetes(p: UnifiedHealthProfile): ModelResult? = withContext(Dispatchers.Default) {
        val session = diabetesSession ?: return@withContext null
        val input = floatArrayOf(
            p.pregnancies, p.glucose, p.diastolic, p.skinThickness,
            p.insulin, p.bmi, if (p.familyDiabetes == "yes") 0.5f else 0.2f, p.age
        )
        OnnxInference.runFullInference(session, input).use { 
            HealthOutputParser.parseDiabetes(it)
        }
    }

    private suspend fun runHeart(p: UnifiedHealthProfile): ModelResult? = withContext(Dispatchers.Default) {
        val session = heartSession ?: return@withContext null
        val input = floatArrayOf(
            p.age, if (p.gender == "male") 1f else 0f, 3.0f,
            p.systolic, p.cholesterol, if (p.glucose > 120f || p.personalDiabetes == "yes") 1f else 0f,
            0f, p.maxHr, 0f, 0f, 2.0f, 0f, 1.0f
        )
        OnnxInference.runFullInference(session, input).use {
            HealthOutputParser.parseHeart(it)
        }
    }

    private suspend fun runHypertension(p: UnifiedHealthProfile): ModelResult? = withContext(Dispatchers.Default) {
        val session = hypertensionSession ?: return@withContext null
        val input = FloatArray(44) { 0f }
        input[0] = p.age; input[1] = p.bmi; input[2] = p.cholesterol
        input[3] = p.systolic; input[4] = p.diastolic
        
        input[5] = when(p.alcohol) {
            "none" -> 0f
            "low" -> 2f
            "moderate" -> 5f
            "high" -> 10f
            else -> 0f
        }
        
        input[6] = when(p.stress) {
            "low" -> 2f
            "moderate" -> 5f
            "high" -> 8f
            "veryHigh" -> 10f
            else -> 5f
        }
        
        input[7] = when(p.salt) {
            "low" -> 2f
            "moderate" -> 5f
            "high" -> 10f
            else -> 5f
        }
        
        input[8] = p.sleep; input[9] = p.restingHr; input[10] = p.ldl; input[11] = p.hdl; input[12] = p.triglycerides; input[13] = p.glucose
        input[20] = 1f 
        if (p.smoking == "former") input[33] = 1f else if (p.smoking == "never") input[34] = 1f
        if (p.activity == "low") input[35] = 1f else if (p.activity == "moderate") input[36] = 1f
        if (p.familyHypertension == "yes") input[37] = 1f
        if (p.personalDiabetes == "yes") input[38] = 1f
        if (p.gender == "male") input[39] = 1f

        OnnxInference.runFullInference(session, input).use {
            HealthOutputParser.parseHypertension(it)
        }
    }

    private suspend fun runObesity(p: UnifiedHealthProfile): ModelResult? = withContext(Dispatchers.Default) {
        val session = obesitySession ?: return@withContext null
        val input = FloatArray(23) { 0f }
        input[0] = p.age; input[1] = p.height / 100f; input[2] = p.weight
        input[3] = (p.vegetables / 10f * 2f) + 1f 
        input[4] = p.meals.coerceIn(1f, 4f)
        input[5] = (p.water / 8f * 2f) + 1f 
        input[6] = when(p.activity) {
            "low" -> 0.5f
            "moderate" -> 1.5f
            "high" -> 2.5f
            "veryHigh" -> 3.0f
            else -> 1.0f
        }
        input[7] = 1f 
        if (p.gender == "male") input[8] = 1f
        if (p.familyOverweight == "yes") input[9] = 1f
        if (p.highCalorie == "yes") input[10] = 1f
        when (p.snacking) {
            "frequently" -> input[11] = 1f
            "sometimes" -> input[12] = 1f
            "never" -> input[13] = 1f
        }
        if (p.smoking != "never") input[14] = 1f
        if (p.calorieMonitoring == "yes") input[15] = 1f
        when (p.alcohol) {
            "high" -> input[16] = 1f
            "moderate" -> input[17] = 1f
            "low" -> input[17] = 1f 
            "none" -> input[18] = 1f
        }
        input[21] = 1f 
        OnnxInference.runFullInference(session, input).use {
            HealthOutputParser.parseObesity(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        diabetesSession?.close()
        heartSession?.close()
        hypertensionSession?.close()
        obesitySession?.close()
    }
}
