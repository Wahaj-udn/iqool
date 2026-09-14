package com.example.geminiapi

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    // TODO: Add your API key here. Get one at https://aistudio.google.com/
    private val apiKey = "YOUR_API_KEY_HERE"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.8-flash",
        apiKey = apiKey
    )

    private val chat = generativeModel.startChat()
    
    private val _chatHistory = MutableStateFlow<List<Content>>(emptyList())
    val chatHistory: StateFlow<List<Content>> = _chatHistory.asStateFlow()

    fun sendPrompt(bitmap: Bitmap?, prompt: String) {
        _uiState.value = UiState.Loading
        
        // Construct the user content
        val userContent = content {
            if (bitmap != null) {
                image(bitmap)
            }
            text(prompt)
        }

        // Optimistically add the user's message to the UI history immediately
        _chatHistory.value = _chatHistory.value + userContent

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Send the message to the model
                chat.sendMessage(userContent)
                
                // Replace optimistic history with the official history (includes the model's response)
                _chatHistory.value = chat.history.toList()
                _uiState.value = UiState.Success("")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error")
                // Remove the failed optimistic message to keep UI consistent
                _chatHistory.value = chat.history.toList()
            }
        }
    }
}
