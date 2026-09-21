package com.example.geminiapi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapi.analysis.ModelResult
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthAssessmentScreen(
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    vm: HealthAssessmentViewModel = viewModel()
) {
    val profile by HealthFeatureStore.profile.collectAsState()
    val results by vm.results.collectAsState()
    val scrollState = rememberScrollState()

    // Local string state for precise numeric inputs to avoid "fidgety" sliders
    var ageStr by remember { mutableStateOf(profile.age.toInt().toString()) }
    var heightStr by remember { mutableStateOf(profile.height.toInt().toString()) }
    var weightStr by remember { mutableStateOf(profile.weight.toInt().toString()) }
    var systolicStr by remember { mutableStateOf(profile.systolic.toInt().toString()) }
    var diastolicStr by remember { mutableStateOf(profile.diastolic.toInt().toString()) }
    var restingHrStr by remember { mutableStateOf(profile.restingHr.toInt().toString()) }
    var maxHrStr by remember { mutableStateOf(profile.maxHr.toInt().toString()) }
    var glucoseStr by remember { mutableStateOf(profile.glucose.toInt().toString()) }
    var cholesterolStr by remember { mutableStateOf(profile.cholesterol.toInt().toString()) }
    var ldlStr by remember { mutableStateOf(profile.ldl.toInt().toString()) }
    var hdlStr by remember { mutableStateOf(profile.hdl.toInt().toString()) }
    var triglyceridesStr by remember { mutableStateOf(profile.triglycerides.toInt().toString()) }
    var insulinStr by remember { mutableStateOf(profile.insulin.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Assessment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Multimodal Health Analysis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // 1. BASIC INFORMATION
            AssessmentSection("BASIC INFORMATION") {
                AssessmentTextField("Age", ageStr, onValueChange = { 
                    ageStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(age = v) } }
                }, suffix = "years")
                
                AssessmentChoice("Gender", profile.gender, listOf("male", "female"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(gender = it) }
                })

                AssessmentTextField("Height", heightStr, onValueChange = { 
                    heightStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(height = v) } }
                }, suffix = "cm")

                AssessmentTextField("Weight", weightStr, onValueChange = { 
                    weightStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(weight = v) } }
                }, suffix = "kg")
                
                Text("Calculated BMI: ${String.format(Locale.US, "%.1f", profile.bmi)}", color = Lime, fontWeight = FontWeight.Bold)
            }

            // 2. VITAL SIGNS
            AssessmentSection("VITAL SIGNS") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AssessmentTextField("Systolic BP", systolicStr, onValueChange = { 
                            systolicStr = it
                            it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(systolic = v) } }
                        })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AssessmentTextField("Diastolic BP", diastolicStr, onValueChange = { 
                            diastolicStr = it
                            it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(diastolic = v) } }
                        })
                    }
                }
                AssessmentTextField("Resting HR", restingHrStr, onValueChange = { 
                    restingHrStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(restingHr = v) } }
                }, suffix = "bpm")
                AssessmentTextField("Max HR", maxHrStr, onValueChange = { 
                    maxHrStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(maxHr = v) } }
                }, suffix = "bpm")
            }

            // 3. BLOOD TESTS
            AssessmentSection("BLOOD TESTS") {
                AssessmentTextField("Glucose", glucoseStr, onValueChange = { 
                    glucoseStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(glucose = v) } }
                }, suffix = "mg/dL")
                AssessmentTextField("Total Cholesterol", cholesterolStr, onValueChange = { 
                    cholesterolStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(cholesterol = v) } }
                }, suffix = "mg/dL")
                AssessmentTextField("LDL", ldlStr, onValueChange = { 
                    ldlStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(ldl = v) } }
                }, suffix = "mg/dL")
                AssessmentTextField("HDL", hdlStr, onValueChange = { 
                    hdlStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(hdl = v) } }
                }, suffix = "mg/dL")
                AssessmentTextField("Triglycerides", triglyceridesStr, onValueChange = { 
                    triglyceridesStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(triglycerides = v) } }
                }, suffix = "mg/dL")
                AssessmentTextField("Insulin", insulinStr, onValueChange = { 
                    insulinStr = it
                    it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(insulin = v) } }
                }, suffix = "mu U/ml")
            }

            // 4. LIFESTYLE FACTORS
            AssessmentSection("LIFESTYLE FACTORS") {
                AssessmentChoice("Smoking Status", profile.smoking, listOf("never", "former", "current"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(smoking = it) }
                })
                AssessmentChoice("Alcohol Consumption", profile.alcohol, listOf("none", "low", "moderate", "high"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(alcohol = it) }
                })
                AssessmentDropdown("Physical Activity", profile.activity, listOf("low", "moderate", "high", "veryHigh"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(activity = it) }
                })
                AssessmentSlider("Daily Sleep", profile.sleep, 1f..16f) { HealthFeatureStore.update { p -> p.copy(sleep = it) } }
                AssessmentChoice("Stress Level", profile.stress, listOf("low", "moderate", "high", "veryHigh"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(stress = it) }
                })
                AssessmentChoice("Salt Intake", profile.salt, listOf("low", "moderate", "high"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(salt = it) }
                })
            }

            // 5. DIET & NUTRITION
            AssessmentSection("DIET & NUTRITION") {
                AssessmentSlider("Vegetable Servings", profile.vegetables, 0f..10f) { HealthFeatureStore.update { p -> p.copy(vegetables = it) } }
                AssessmentSlider("Main Meals/Day", profile.meals, 1f..6f) { HealthFeatureStore.update { p -> p.copy(meals = it) } }
                AssessmentSlider("Water Intake (L)", profile.water, 0f..8f, 0.5f) { HealthFeatureStore.update { p -> p.copy(water = it) } }
                AssessmentChoice("High-Calorie Food", profile.highCalorie, listOf("yes", "no"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(highCalorie = it) }
                })
                AssessmentDropdown("Snacking", profile.snacking, listOf("never", "sometimes", "frequently", "always"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(snacking = it) }
                })
                AssessmentChoice("Calorie Monitoring", profile.calorieMonitoring, listOf("yes", "no"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(calorieMonitoring = it) }
                })
            }

            // 6. FAMILY & PERSONAL HISTORY
            AssessmentSection("FAMILY & HISTORY") {
                AssessmentChoice("Family History Diabetes", profile.familyDiabetes, listOf("yes", "no"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(familyDiabetes = it) }
                })
                AssessmentChoice("Family History Hypertension", profile.familyHypertension, listOf("yes", "no"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(familyHypertension = it) }
                })
                AssessmentChoice("Family History Overweight", profile.familyOverweight, listOf("yes", "no"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(familyOverweight = it) }
                })
                AssessmentChoice("Personal Diabetes Status", profile.personalDiabetes, listOf("yes", "no"), onSelect = {
                    HealthFeatureStore.update { p -> p.copy(personalDiabetes = it) }
                })
                if (profile.gender == "female") {
                    AssessmentTextField("Number of Pregnancies", profile.pregnancies.toInt().toString(), onValueChange = { 
                        it.toFloatOrNull()?.let { v -> HealthFeatureStore.update { p -> p.copy(pregnancies = v) } }
                    })
                }
            }

            Button(
                onClick = { vm.runAllPredictions() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = !results.isCalculating,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Charcoal)
            ) {
                if (results.isCalculating) CircularProgressIndicator(color = Charcoal)
                else Text("GENERATE HEALTH REPORT", fontWeight = FontWeight.ExtraBold)
            }

            if (results.diabetes != null) {
                FinalReportCard(results)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AppButton(
                    text = "DISCUSS WITH AI COACH",
                    onClick = onNavigateToChat,
                    modifier = Modifier.fillMaxWidth(),
                    style = AppButtonStyle.Dark,
                    icon = AppIcons.Sparkle
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FinalReportCard(r: AssessmentResults) {
    HeroCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📋 CLINICAL ASSESSMENT SUMMARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Lime)
            ReportRow("Diabetes Risk", r.diabetes)
            ReportRow("Heart Health Risk", r.heart)
            ReportRow("Hypertension Risk", r.hypertension)
            ReportRow("Obesity Category", r.obesity)
        }
    }
}

@Composable
fun ReportRow(label: String, res: ModelResult?) {
    if (res == null) return
    val value = res.label
    val isRisk = value == "At Risk" || value.contains("Obesity") || value.contains("Overweight")
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("Certainty: ${String.format(Locale.US, "%.1f", res.confidence)}%", style = MaterialTheme.typography.labelSmall, color = mutedContent())
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isRisk) AccentRed else Color(0xFFBBF246)
        )
    }
}
