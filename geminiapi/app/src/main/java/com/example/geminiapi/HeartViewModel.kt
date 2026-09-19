package com.example.geminiapi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.analysis.OnnxInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HeartUiState(
    val age: String = "30",
    val isMale: Boolean = true,
    val chestPainType: Float = 0f, 
    val restingBP: String = "120",
    val cholesterol: String = "200",
    val fastingBS: Boolean = false,
    val restECG: Float = 0f, 
    val maxHR: String = "150",
    val exerciseAngina: Boolean = false,
    val oldPeak: String = "0.0",
    val slope: Float = 1f, 
    val ca: Float = 0f, 
    val thal: Float = 2f, 
    val resultMessage: String? = null,
    val isCalculating: Boolean = false,
    val error: String? = null
)

class HeartViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HeartUiState())
    val uiState: StateFlow<HeartUiState> = _uiState.asStateFlow()

    private var session = OnnxInference.loadSession(application, "heart_model.onnx")

    fun updateAge(v: String) { _uiState.value = _uiState.value.copy(age = v) }
    fun updateGender(isMale: Boolean) { _uiState.value = _uiState.value.copy(isMale = isMale) }
    fun updateChestPain(v: Float) { _uiState.value = _uiState.value.copy(chestPainType = v) }
    fun updateRestingBP(v: String) { _uiState.value = _uiState.value.copy(restingBP = v) }
    fun updateCholesterol(v: String) { _uiState.value = _uiState.value.copy(cholesterol = v) }
    fun updateFastingBS(v: Boolean) { _uiState.value = _uiState.value.copy(fastingBS = v) }
    fun updateRestECG(v: Float) { _uiState.value = _uiState.value.copy(restECG = v) }
    fun updateMaxHR(v: String) { _uiState.value = _uiState.value.copy(maxHR = v) }
    fun updateExerciseAngina(v: Boolean) { _uiState.value = _uiState.value.copy(exerciseAngina = v) }
    fun updateOldPeak(v: String) { _uiState.value = _uiState.value.copy(oldPeak = v) }
    fun updateSlope(v: Float) { _uiState.value = _uiState.value.copy(slope = v) }
    fun updateCA(v: Float) { _uiState.value = _uiState.value.copy(ca = v) }
    fun updateThal(v: Float) { _uiState.value = _uiState.value.copy(thal = v) }

    fun predict() {
        val s = _uiState.value
        val currentSession = session ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculating = true)
            try {
                val input = FloatArray(13) { 0f }
                input[0] = s.age.toFloatOrNull() ?: 30f
                input[1] = if (s.isMale) 1.0f else 0.0f
                input[2] = s.chestPainType
                input[3] = s.restingBP.toFloatOrNull() ?: 120f
                input[4] = s.cholesterol.toFloatOrNull() ?: 200f
                input[5] = if (s.fastingBS) 1.0f else 0.0f
                input[6] = s.restECG
                input[7] = s.maxHR.toFloatOrNull() ?: 150f
                input[8] = if (s.exerciseAngina) 1.0f else 0.0f
                input[9] = s.oldPeak.toFloatOrNull() ?: 0.0f
                input[10] = s.slope
                input[11] = s.ca
                input[12] = s.thal

                val prediction = withContext(Dispatchers.Default) {
                    OnnxInference.runInference(currentSession, input)
                }

                _uiState.value = _uiState.value.copy(
                    resultMessage = if (prediction == 1L) "At Risk of Heart Disease" else "No Heart Disease Risk",
                    isCalculating = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isCalculating = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        session?.close()
    }
}
