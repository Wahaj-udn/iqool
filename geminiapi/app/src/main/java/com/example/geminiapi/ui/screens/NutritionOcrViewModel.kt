package com.example.geminiapi.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.HealthDataManager
import com.example.geminiapi.HealthFeatureStore
import com.example.geminiapi.llama.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class NutritionOcrUiState(
    val extractedText: String = "",
    val structuredNutrition: Map<String, Double?> = emptyMap(),
    val assessmentResponse: String = "",
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isModelLoaded: Boolean = false,
    val error: String? = null
)

class NutritionOcrViewModel(application: Application) : AndroidViewModel(application) {
    private val healthDataManager = HealthDataManager(application)
    private val llamaEngine = LlamaEngine.shared
    
    private val _uiState = MutableStateFlow(NutritionOcrUiState())
    val uiState: StateFlow<NutritionOcrUiState> = _uiState.asStateFlow()

    init {
        checkModelLoaded()
    }

    private fun checkModelLoaded() {
        _uiState.value = _uiState.value.copy(isModelLoaded = llamaEngine.isLoaded())
    }

    fun onTextExtracted(text: String) {
        val structured = parseStructuredNutrition(text)
        _uiState.value = _uiState.value.copy(
            extractedText = text,
            structuredNutrition = structured,
            assessmentResponse = "",
            error = null
        )
    }

    fun assessFood() {
        if (_uiState.value.isGenerating) return
        
        val modelPath = "/storage/emulated/0/Download/Qwen3-4B-Q4_K_M.gguf"
        
        _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure model is loaded (reuses shared instance)
                if (!llamaEngine.isLoaded()) {
                    if (!File(modelPath).exists()) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(error = "Model file not found. Please download Qwen3.", isGenerating = false)
                        }
                        return@launch
                    }
                    llamaEngine.load(modelPath)
                }
                
                // 1. Gather User Health Context
                val healthSummary = healthDataManager.fetchLast15DaysSummary()
                
                // 2. Gather Assessment Results
                val assessmentResults = HealthFeatureStore.lastResults.value
                val assessmentText = buildString {
                    append("Diabetes risk: ${assessmentResults?.diabetes?.let { "${it.label} (${(it.confidence * 100).toInt()}%)" } ?: "Unavailable"}\n")
                    append("Heart disease risk: ${assessmentResults?.heart?.let { "${it.label} (${(it.confidence * 100).toInt()}%)" } ?: "Unavailable"}\n")
                    append("Hypertension risk: ${assessmentResults?.hypertension?.let { "${it.label} (${(it.confidence * 100).toInt()}%)" } ?: "Unavailable"}\n")
                    append("Obesity risk: ${assessmentResults?.obesity?.let { "${it.label} (${(it.confidence * 100).toInt()}%)" } ?: "Unavailable"}\n")
                }

                // 3. Nutrition Label
                val nutritionText = _uiState.value.structuredNutrition.filterValues { it != null }
                    .map { "${it.key}: ${it.value}" }
                    .joinToString("\n")

                // 4. Build Prompt
                val prompt = buildPrompt(nutritionText, healthSummary, assessmentText)
                
                // 5. Generate with Qwen3 (Quick mode - thinkingEnabled = false as it is a concise response)
                llamaEngine.completion(prompt, thinkingEnabled = false) { token ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            assessmentResponse = _uiState.value.assessmentResponse + token
                        )
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isGenerating = false, isModelLoaded = true)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(error = "Generation failed: ${e.localizedMessage}", isGenerating = false)
                }
            }
        }
    }

    private fun buildPrompt(nutrition: String, health: String, assessment: String): String {
        return """
            SYSTEM:
            You are a concise personal food-assessment assistant.
            Your task is to assess how suitable the given food appears for this specific user based on the nutrition label and the user's recent health/activity data.
            Use only the information provided. Do not invent missing values. Do not diagnose diseases.
            Do not make claims that require information not provided.
            Respond in 2–4 short sentences. Keep the response short and concise. Do not repeat the label.

            USER:
            NUTRITION LABEL:
            $nutrition

            15-DAY HEALTH SUMMARY:
            $health

            HEALTH ASSESSMENT:
            $assessment

            TASK:
            Briefly tell the user how this food would be for them to eat. Mention the most important reasons based on the data.
        """.trimIndent()
    }

    private fun parseStructuredNutrition(text: String): Map<String, Double?> {
        val result = mutableMapOf<String, Double?>(
            "calories_kcal" to null,
            "total_fat_g" to null,
            "saturated_fat_g" to null,
            "trans_fat_g" to null,
            "cholesterol_mg" to null,
            "sodium_mg" to null,
            "carbohydrates_g" to null,
            "fiber_g" to null,
            "sugars_g" to null,
            "added_sugars_g" to null,
            "protein_g" to null
        )

        val lines = text.lines()
        val numericRegex = Regex("""\d+(?:\.\d+)?""")

        for (line in lines) {
            val lower = line.lowercase()
            val matchValue = numericRegex.find(line)?.value?.toDoubleOrNull() ?: continue

            when {
                "saturated" in lower -> result["saturated_fat_g"] = matchValue
                "trans" in lower -> result["trans_fat_g"] = matchValue
                "added" in lower -> result["added_sugars_g"] = matchValue
                "fat" in lower -> result["total_fat_g"] = matchValue
                "cholesterol" in lower -> result["cholesterol_mg"] = matchValue
                "sodium" in lower -> result["sodium_mg"] = matchValue
                "carbohydrate" in lower || "carb" in lower -> result["carbohydrates_g"] = matchValue
                "fiber" in lower -> result["fiber_g"] = matchValue
                "sugar" in lower -> result["sugars_g"] = matchValue
                "protein" in lower -> result["protein_g"] = matchValue
                "calorie" in lower || "energy" in lower || "kcal" in lower -> result["calories_kcal"] = matchValue
            }
        }
        return result
    }

    fun clear() {
        _uiState.value = NutritionOcrUiState(isModelLoaded = llamaEngine.isLoaded())
    }
}
