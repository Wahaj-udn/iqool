package com.example.geminiapi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.analysis.HypertensionPredictor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HypertensionUiState(
    val age: String = "30",
    val bmi: String = "24.2",
    val cholesterol: String = "200",
    val systolic: String = "120",
    val diastolic: String = "80",
    val alcohol: String = "0",
    val stress: String = "5",
    val salt: String = "5",
    val sleep: String = "7",
    val heartRate: String = "70",
    val ldl: String = "100",
    val hdl: String = "50",
    val triglycerides: String = "150",
    val glucose: String = "100",
    val country: String = "India",
    val smoking: String = "Never",
    val activity: String = "Moderate",
    val hasFamilyHistory: Boolean = false,
    val hasDiabetes: Boolean = false,
    val isMale: Boolean = true,
    val education: String = "Secondary",
    val employment: String = "Employed",
    val result: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HypertensionViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HypertensionUiState())
    val uiState: StateFlow<HypertensionUiState> = _uiState.asStateFlow()

    private val predictor = HypertensionPredictor(application)

    fun updateField(update: (HypertensionUiState) -> HypertensionUiState) {
        _uiState.value = update(_uiState.value)
    }

    fun predict() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val input = FloatArray(44) { 0f }
                // Continuous 0-13
                input[0] = s.age.toFloatOrNull() ?: 30f
                input[1] = s.bmi.toFloatOrNull() ?: 24.2f
                input[2] = s.cholesterol.toFloatOrNull() ?: 200f
                input[3] = s.systolic.toFloatOrNull() ?: 120f
                input[4] = s.diastolic.toFloatOrNull() ?: 80f
                input[5] = s.alcohol.toFloatOrNull() ?: 0f
                input[6] = s.stress.toFloatOrNull() ?: 5f
                input[7] = s.salt.toFloatOrNull() ?: 5f
                input[8] = s.sleep.toFloatOrNull() ?: 7f
                input[9] = s.heartRate.toFloatOrNull() ?: 70f
                input[10] = s.ldl.toFloatOrNull() ?: 100f
                input[11] = s.hdl.toFloatOrNull() ?: 50f
                input[12] = s.triglycerides.toFloatOrNull() ?: 150f
                input[13] = s.glucose.toFloatOrNull() ?: 100f

                // One-hot Country 14-32
                val countries = listOf("Australia", "Brazil", "Canada", "China", "France", "Germany", "India", "Indonesia", "Italy", "Japan", "Mexico", "Russia", "Saudi Arabia", "South Africa", "South Korea", "Spain", "Turkey", "UK", "USA")
                val countryIdx = countries.indexOf(s.country)
                if (countryIdx != -1) input[14 + countryIdx] = 1f

                // Smoking 33-34
                if (s.smoking == "Former") input[33] = 1f
                else if (s.smoking == "Never") input[34] = 1f

                // Activity 35-36
                if (s.activity == "Low") input[35] = 1f
                else if (s.activity == "Moderate") input[36] = 1f

                if (s.hasFamilyHistory) input[37] = 1f
                if (s.hasDiabetes) input[38] = 1f
                if (s.isMale) input[39] = 1f

                // Education 40-41
                if (s.education == "Secondary") input[40] = 1f
                else if (s.education == "Tertiary") input[41] = 1f

                // Employment 42-43
                if (s.employment == "Retired") input[42] = 1f
                else if (s.employment == "Unemployed") input[43] = 1f

                val pred = withContext(Dispatchers.Default) {
                    predictor.predict(input)
                }

                _uiState.value = _uiState.value.copy(
                    result = if (pred == 0L) "High Risk of Hypertension" else "Normal / Low Risk",
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        predictor.close()
    }
}
