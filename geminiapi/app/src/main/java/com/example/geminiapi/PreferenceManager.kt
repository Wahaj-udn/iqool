package com.example.geminiapi

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("health_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        
        // Unified Profile keys
        private const val K_NAME = "name"
        private const val K_AGE = "age"
        private const val K_GENDER = "gender"
        private const val K_HEIGHT = "height"
        private const val K_WEIGHT = "weight"
        private const val K_SYSTOLIC = "systolic"
        private const val K_DIASTOLIC = "diastolic"
        private const val K_GLUCOSE = "glucose"
        private const val K_SMOKING = "smoking"
        private const val K_ACTIVITY = "activity"
        private const val K_RESTING_HR = "restingHr"
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    fun saveFullProfile(p: UnifiedHealthProfile) {
        prefs.edit().apply {
            putString(K_NAME, p.name)
            putFloat(K_AGE, p.age)
            putString(K_GENDER, p.gender)
            putFloat(K_HEIGHT, p.height)
            putFloat(K_WEIGHT, p.weight)
            putFloat(K_SYSTOLIC, p.systolic)
            putFloat(K_DIASTOLIC, p.diastolic)
            putFloat(K_GLUCOSE, p.glucose)
            putString(K_SMOKING, p.smoking)
            putString(K_ACTIVITY, p.activity)
            putFloat(K_RESTING_HR, p.restingHr)
        }.apply()
    }

    fun getProfile(): UnifiedHealthProfile {
        return UnifiedHealthProfile(
            name = prefs.getString(K_NAME, "Health User") ?: "Health User",
            age = prefs.getFloat(K_AGE, 30f),
            height = prefs.getFloat(K_HEIGHT, 170f),
            weight = prefs.getFloat(K_WEIGHT, 70f),
            systolic = prefs.getFloat(K_SYSTOLIC, 120f),
            diastolic = prefs.getFloat(K_DIASTOLIC, 80f),
            glucose = prefs.getFloat(K_GLUCOSE, 100f),
            smoking = prefs.getString(K_SMOKING, "never") ?: "never",
            activity = prefs.getString(K_ACTIVITY, "moderate") ?: "moderate",
            restingHr = prefs.getFloat(K_RESTING_HR, 70f)
        )
    }
}
