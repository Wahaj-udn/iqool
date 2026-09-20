package com.example.geminiapi

import com.example.geminiapi.analysis.ModelResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

object HealthFeatureStore {
    private val _profile = MutableStateFlow(UnifiedHealthProfile())
    val profile: StateFlow<UnifiedHealthProfile> = _profile.asStateFlow()

    private val _lastResults = MutableStateFlow<AssessmentResults?>(null)
    val lastResults: StateFlow<AssessmentResults?> = _lastResults.asStateFlow()

    fun init(p: UnifiedHealthProfile) {
        _profile.value = p
    }

    fun update(update: (UnifiedHealthProfile) -> UnifiedHealthProfile) {
        val newProfile = update(_profile.value)
        val heightM = newProfile.height / 100f
        val calculatedBmi = if (heightM > 0) newProfile.weight / heightM.pow(2) else 24.2f
        _profile.value = newProfile.copy(bmi = calculatedBmi)
    }

    fun setResults(results: AssessmentResults) {
        _lastResults.value = results
    }
}

data class AssessmentResults(
    val diabetes: ModelResult? = null,
    val heart: ModelResult? = null,
    val hypertension: ModelResult? = null,
    val obesity: ModelResult? = null,
    val isCalculating: Boolean = false
)

data class UnifiedHealthProfile(
    val age: Float = 30f,
    val gender: String = "male",
    val height: Float = 170f,
    val weight: Float = 70f,
    val bmi: Float = 24.2f,
    val systolic: Float = 120f,
    val diastolic: Float = 80f,
    val restingHr: Float = 70f,
    val maxHr: Float = 180f,
    val glucose: Float = 90f,
    val cholesterol: Float = 180f,
    val ldl: Float = 100f,
    val hdl: Float = 50f,
    val triglycerides: Float = 150f,
    val insulin: Float = 10f,
    val skinThickness: Float = 20f,
    val smoking: String = "never",
    val alcohol: String = "none",
    val activity: String = "moderate",
    val sleep: Float = 7f,
    val stress: String = "moderate",
    val salt: String = "moderate",
    val vegetables: Float = 3f,
    val meals: Float = 3f,
    val water: Float = 2f,
    val highCalorie: String = "no",
    val snacking: String = "sometimes",
    val calorieMonitoring: String = "no",
    val familyDiabetes: String = "no",
    val familyHypertension: String = "no",
    val familyOverweight: String = "no",
    val personalDiabetes: String = "no",
    val pregnancies: Float = 0f
)
