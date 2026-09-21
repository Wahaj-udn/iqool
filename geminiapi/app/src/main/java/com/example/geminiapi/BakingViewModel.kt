package com.example.geminiapi

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.analysis.ModelResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class BakingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val apiKey = "AQ.Ab8RN6LOFR_SxdV7lcC392Ko7LQtYHh5PGa5dtjbj9RF8MV8VA"

    private val generativeModel = try {
        GenerativeModel(
            modelName = "gemini-3.6-flash",
            apiKey = apiKey,
            systemInstruction = content {
                text("You are a helpful health and fitness AI coach. Use the provided user profile, vitals, and risk assessment results to give personalized advice. Always emphasize that you are an AI and not a doctor.")
            }
        )
    } catch (e: Exception) {
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
    }

    private val chat = generativeModel.startChat()
    
    private val _chatHistory = MutableStateFlow<List<Content>>(emptyList())
    val chatHistory: StateFlow<List<Content>> = _chatHistory.asStateFlow()

    private val healthDataManager = HealthDataManager(application)

    fun sendPrompt(bitmap: Bitmap?, prompt: String) {
        _uiState.value = UiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isFirstMessage = chat.history.isEmpty()
                
                val finalPrompt = if (isFirstMessage) {
                    val healthConnectSummary = healthDataManager.fetchLast15DaysSummary()
                    val profile = HealthFeatureStore.profile.value
                    val results = HealthFeatureStore.lastResults.value
                    
                    val contextBuilder = StringBuilder()
                    contextBuilder.append("--- USER HEALTH CONTEXT ---\n")
                    contextBuilder.append("PROFILE:\n")
                    contextBuilder.append("Age: ${profile.age}, Gender: ${profile.gender}, Height: ${profile.height}cm, Weight: ${profile.weight}kg, BMI: ${String.format(Locale.US, "%.1f", profile.bmi)}\n")
                    contextBuilder.append("\nVITALS & LABS:\n")
                    contextBuilder.append("BP: ${profile.systolic}/${profile.diastolic}, Glucose: ${profile.glucose}, Cholesterol: ${profile.cholesterol}, HR: ${profile.restingHr}\n")
                    contextBuilder.append("LDL: ${profile.ldl}, HDL: ${profile.hdl}, Triglycerides: ${profile.triglycerides}, Insulin: ${profile.insulin}\n")
                    contextBuilder.append("\nLIFESTYLE:\n")
                    contextBuilder.append("Smoking: ${profile.smoking}, Activity: ${profile.activity}, Alcohol: ${profile.alcohol}, Sleep: ${profile.sleep}h, Stress: ${profile.stress}\n")
                    
                    if (results != null) {
                        contextBuilder.append("\n--- ONNX AI RISK ASSESSMENT RESULTS ---\n")
                        contextBuilder.append("Diabetes: ${results.diabetes?.label} (Certainty: ${String.format(Locale.US, "%.1f", results.diabetes?.confidence)}%)\n")
                        contextBuilder.append("Heart Health: ${results.heart?.label} (Certainty: ${String.format(Locale.US, "%.1f", results.heart?.confidence)}%)\n")
                        contextBuilder.append("Hypertension: ${results.hypertension?.label} (Certainty: ${String.format(Locale.US, "%.1f", results.hypertension?.confidence)}%)\n")
                        contextBuilder.append("Obesity: ${results.obesity?.label} (Certainty: ${String.format(Locale.US, "%.1f", results.obesity?.confidence)}%)\n")
                    }
                    
                    contextBuilder.append("\n--- HEALTH CONNECT DATA (PAST 15 DAYS) ---\n")
                    contextBuilder.append(healthConnectSummary)
                    contextBuilder.append("\n--- END OF CONTEXT ---\n")
                    
                    contextBuilder.append("\nUser Question: $prompt")
                    contextBuilder.toString()
                } else {
                    prompt
                }

                val userContent = content {
                    if (bitmap != null) image(bitmap)
                    text(finalPrompt)
                }

                val uiUserContent = if (isFirstMessage) {
                    content { 
                        if (bitmap != null) image(bitmap)
                        text(prompt) 
                    }
                } else {
                    userContent
                }

                _chatHistory.value = _chatHistory.value + uiUserContent
                chat.sendMessage(userContent)
                
                val officialHistory = chat.history.toMutableList()
                if (isFirstMessage && officialHistory.isNotEmpty()) {
                    officialHistory[0] = uiUserContent
                }
                
                _chatHistory.value = officialHistory.toList()
                _uiState.value = UiState.Success("")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
