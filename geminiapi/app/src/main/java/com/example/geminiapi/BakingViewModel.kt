package com.example.geminiapi

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    // TODO: Add your API key here. Get one at https://aistudio.google.com/
    private val apiKey = "AQ.Ab8RN6Ld_6i_JZDi1AAYW5B6dRUv6aKA5jJhKVU3DGg4_edZVw"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.6-flash",
        apiKey = apiKey,
        systemInstruction = content {
            text("You are a helpful health and fitness assistant. Use the provided health data context to give personalized advice and answer questions accurately.")
        }
    )

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
                    val healthContext = healthDataManager.fetchLast15DaysSummary()
                    "$healthContext\n\nUser Question: $prompt"
                } else {
                    prompt
                }

                // Construct the content to send
                val userContent = content {
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    text(finalPrompt)
                }

                // For the UI, we only want to show the original prompt if it was the first message
                // to avoid cluttering with the background health data.
                val uiUserContent = if (isFirstMessage) {
                    content { 
                        if (bitmap != null) image(bitmap)
                        text(prompt) 
                    }
                } else {
                    userContent
                }

                // Optimistically update UI history with the clean prompt
                _chatHistory.value = _chatHistory.value + uiUserContent

                // Send the message to the model (with health context if first)
                chat.sendMessage(userContent)
                
                // Refresh history from the chat session
                // We need to map the first message back to the clean version in our local state
                // OR we can just keep managing our own _chatHistory if we want absolute control.
                // Given the requirement, let's keep it simple: replace the long prompt in history with the short one.
                val officialHistory = chat.history.toMutableList()
                if (isFirstMessage && officialHistory.isNotEmpty()) {
                    officialHistory[0] = uiUserContent
                }
                
                _chatHistory.value = officialHistory.toList()
                _uiState.value = UiState.Success("")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error")
                _chatHistory.value = _chatHistory.value // trigger update or cleanup
            }
        }
    }
}
