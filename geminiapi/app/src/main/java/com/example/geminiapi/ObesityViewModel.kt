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
import java.util.Locale

data class ObesityUiState(
    val age: String = "30",
    val height: String = "1.70",
    val weight: String = "70",
    val fcvc: Float = 2f, 
    val ncp: Float = 3f, 
    val ch2o: Float = 2f, 
    val faf: Float = 1f, 
    val tue: Float = 1f, 
    val isMale: Boolean = true,
    val familyHistory: Boolean = false,
    val favc: Boolean = false, 
    val caec: String = "Sometimes", 
    val smoke: Boolean = false,
    val scc: Boolean = false, 
    val calc: String = "Sometimes", 
    val transport: String = "Public_Transportation", 
    val resultLabel: String? = null,
    val isCalculating: Boolean = false,
    val error: String? = null
)

class ObesityViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ObesityUiState())
    val uiState: StateFlow<ObesityUiState> = _uiState.asStateFlow()

    private val healthDataManager = HealthDataManager(application)
    private var session = OnnxInference.loadSession(application, "obesity_model.onnx")

    private val labels = arrayOf(
        "Insufficient Weight", "Normal Weight",
        "Obesity Type I", "Obesity Type II", "Obesity Type III",
        "Overweight Level I", "Overweight Level II"
    )

    fun syncWithHealthConnect() {
        viewModelScope.launch {
            val (h, w) = healthDataManager.getLatestHeightAndWeight()
            _uiState.value = _uiState.value.copy(
                height = h?.let { String.format(Locale.US, "%.2f", it) } ?: _uiState.value.height,
                weight = w?.let { String.format(Locale.US, "%.1f", it) } ?: _uiState.value.weight
            )
        }
    }

    fun updateAge(v: String) { _uiState.value = _uiState.value.copy(age = v) }
    fun updateHeight(v: String) { _uiState.value = _uiState.value.copy(height = v) }
    fun updateWeight(v: String) { _uiState.value = _uiState.value.copy(weight = v) }
    fun updateFcvc(v: Float) { _uiState.value = _uiState.value.copy(fcvc = v) }
    fun updateNcp(v: Float) { _uiState.value = _uiState.value.copy(ncp = v) }
    fun updateCh2o(v: Float) { _uiState.value = _uiState.value.copy(ch2o = v) }
    fun updateFaf(v: Float) { _uiState.value = _uiState.value.copy(faf = v) }
    fun updateTue(v: Float) { _uiState.value = _uiState.value.copy(tue = v) }
    fun updateGender(isMale: Boolean) { _uiState.value = _uiState.value.copy(isMale = isMale) }
    fun updateFamilyHistory(v: Boolean) { _uiState.value = _uiState.value.copy(familyHistory = v) }
    fun updateFavc(v: Boolean) { _uiState.value = _uiState.value.copy(favc = v) }
    fun updateCaec(v: String) { _uiState.value = _uiState.value.copy(caec = v) }
    fun updateSmoke(v: Boolean) { _uiState.value = _uiState.value.copy(smoke = v) }
    fun updateScc(v: Boolean) { _uiState.value = _uiState.value.copy(scc = v) }
    fun updateCalc(v: String) { _uiState.value = _uiState.value.copy(calc = v) }
    fun updateTransport(v: String) { _uiState.value = _uiState.value.copy(transport = v) }

    fun predict() {
        val s = _uiState.value
        val currentSession = session ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculating = true)
            try {
                val input = FloatArray(23) { 0f }
                input[0] = s.age.toFloatOrNull() ?: 30f
                input[1] = s.height.toFloatOrNull() ?: 1.70f
                input[2] = s.weight.toFloatOrNull() ?: 70f
                input[3] = s.fcvc
                input[4] = s.ncp
                input[5] = s.ch2o
                input[6] = s.faf
                input[7] = s.tue

                if (s.isMale) input[8] = 1f
                if (s.familyHistory) input[9] = 1f
                if (s.favc) input[10] = 1f

                when (s.caec) {
                    "Frequently" -> input[11] = 1f
                    "Sometimes" -> input[12] = 1f
                    "no" -> input[13] = 1f
                }

                if (s.smoke) input[14] = 1f
                if (s.scc) input[15] = 1f

                when (s.calc) {
                    "Frequently" -> input[16] = 1f
                    "Sometimes" -> input[17] = 1f
                    "no" -> input[18] = 1f
                }

                when (s.transport) {
                    "Bike" -> input[19] = 1f
                    "Motorbike" -> input[20] = 1f
                    "Public_Transportation" -> input[21] = 1f
                    "Walking" -> input[22] = 1f
                }

                val prediction = withContext(Dispatchers.Default) {
                    OnnxInference.runInference(currentSession, input)
                }

                _uiState.value = _uiState.value.copy(
                    resultLabel = labels[prediction.toInt()],
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
