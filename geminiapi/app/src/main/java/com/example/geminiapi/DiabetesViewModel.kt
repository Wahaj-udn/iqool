package com.example.geminiapi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.analysis.DiabetesPredictor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiabetesUiState(
    val pregnancies: String = "0",
    val glucose: String = "100",
    val bloodPressure: String = "80",
    val skinThickness: String = "20",
    val insulin: String = "80",
    val bmi: String = "24.2",
    val pedigree: String = "0.2",
    val age: String = "30",
    val resultMessage: String? = null,
    val probability: Float? = null,
    val isError: Boolean = false
)

class DiabetesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DiabetesUiState())
    val uiState: StateFlow<DiabetesUiState> = _uiState.asStateFlow()

    private val predictor = DiabetesPredictor(application)

    fun updatePregnancies(v: String) { _uiState.value = _uiState.value.copy(pregnancies = v) }
    fun updateGlucose(v: String) { _uiState.value = _uiState.value.copy(glucose = v) }
    fun updateBP(v: String) { _uiState.value = _uiState.value.copy(bloodPressure = v) }
    fun updateSkin(v: String) { _uiState.value = _uiState.value.copy(skinThickness = v) }
    fun updateInsulin(v: String) { _uiState.value = _uiState.value.copy(insulin = v) }
    fun updateBmi(v: String) { _uiState.value = _uiState.value.copy(bmi = v) }
    fun updatePedigree(v: String) { _uiState.value = _uiState.value.copy(pedigree = v) }
    fun updateAge(v: String) { _uiState.value = _uiState.value.copy(age = v) }

    fun predict() {
        val s = _uiState.value
        try {
            val inputArr = floatArrayOf(
                s.pregnancies.toFloatOrNull() ?: 0f,
                s.glucose.toFloatOrNull() ?: 100f,
                s.bloodPressure.toFloatOrNull() ?: 80f,
                s.skinThickness.toFloatOrNull() ?: 20f,
                s.insulin.toFloatOrNull() ?: 80f,
                s.bmi.toFloatOrNull() ?: 24.2f,
                s.pedigree.toFloatOrNull() ?: 0.2f,
                s.age.toFloatOrNull() ?: 30f
            )

            val (prob, pred) = predictor.predict(inputArr)
            
            _uiState.value = _uiState.value.copy(
                resultMessage = if (pred == 1) "At Risk of Diabetes" else "No Risk Detected",
                probability = prob * 100f,
                isError = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                resultMessage = "Error: Check your inputs",
                isError = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        predictor.close()
    }
}
