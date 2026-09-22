package com.example.geminiapi.llama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LlamaUiState(
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isThinkingEnabled: Boolean = false,
    val response: String = "",
    val error: String? = null
)

class LocalLlamaViewModel : ViewModel() {
    private val engine = LlamaEngine.shared
    
    private val _uiState = MutableStateFlow(LlamaUiState())
    val uiState: StateFlow<LlamaUiState> = _uiState.asStateFlow()

    fun loadQwenModel() {
        val modelPath = "/storage/emulated/0/Download/Qwen3-4B-Q4_K_M.gguf"
        val file = File(modelPath)
        
        if (!file.exists()) {
            _uiState.value = _uiState.value.copy(error = "Model file not found at: $modelPath")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch(Dispatchers.IO) {
            val success = engine.load(modelPath)
            withContext(Dispatchers.Main) {
                if (success) {
                    _uiState.value = _uiState.value.copy(isLoaded = true, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Failed to initialize llama.cpp engine", isLoading = false)
                }
            }
        }
    }

    fun setThinkingEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isThinkingEnabled = enabled)
    }

    fun sendPrompt(prompt: String) {
        if (!_uiState.value.isLoaded || _uiState.value.isGenerating) return
        
        val thinkingEnabled = _uiState.value.isThinkingEnabled
        _uiState.value = _uiState.value.copy(isGenerating = true, response = "", error = null)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                engine.completion(prompt, thinkingEnabled) { token ->
                    // Update UI for every token received (Streaming)
                    _uiState.value = _uiState.value.copy(
                        response = _uiState.value.response + token
                    )
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(error = e.message, isGenerating = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do not automatically unload to allow cross-screen persistence in Phase 2
    }
}
