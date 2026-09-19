package com.example.geminiapi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapi.analysis.ModelResult
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
                AssessmentSlider("Age", profile.age, 1f..120f) { HealthFeatureStore.update { p -> p.copy(age = it) } }
                AssessmentDropdown("Gender", profile.gender, listOf("male", "female")) { HealthFeatureStore.update { p -> p.copy(gender = it) } }
                AssessmentSlider("Height (cm)", profile.height, 50f..250f) { HealthFeatureStore.update { p -> p.copy(height = it) } }
                AssessmentSlider("Weight (kg)", profile.weight, 20f..300f) { HealthFeatureStore.update { p -> p.copy(weight = it) } }
                Text("Calculated BMI: ${String.format(Locale.US, "%.1f", profile.bmi)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            // 2. VITAL SIGNS
            AssessmentSection("VITAL SIGNS") {
                AssessmentSlider("Systolic BP", profile.systolic, 60f..250f) { HealthFeatureStore.update { p -> p.copy(systolic = it) } }
                AssessmentSlider("Diastolic BP", profile.diastolic, 40f..150f) { HealthFeatureStore.update { p -> p.copy(diastolic = it) } }
                AssessmentSlider("Resting HR", profile.restingHr, 40f..200f) { HealthFeatureStore.update { p -> p.copy(restingHr = it) } }
                AssessmentSlider("Max HR", profile.maxHr, 60f..250f) { HealthFeatureStore.update { p -> p.copy(maxHr = it) } }
            }

            // 3. BLOOD TESTS
            AssessmentSection("BLOOD TESTS") {
                AssessmentSlider("Glucose (mg/dL)", profile.glucose, 30f..500f) { HealthFeatureStore.update { p -> p.copy(glucose = it) } }
                AssessmentSlider("Cholesterol (mg/dL)", profile.cholesterol, 100f..500f) { HealthFeatureStore.update { p -> p.copy(cholesterol = it) } }
                AssessmentSlider("LDL (mg/dL)", profile.ldl, 20f..400f) { HealthFeatureStore.update { p -> p.copy(ldl = it) } }
                AssessmentSlider("HDL (mg/dL)", profile.hdl, 20f..150f) { HealthFeatureStore.update { p -> p.copy(hdl = it) } }
                AssessmentSlider("Triglycerides (mg/dL)", profile.triglycerides, 30f..1000f) { HealthFeatureStore.update { p -> p.copy(triglycerides = it) } }
                AssessmentSlider("Insulin (mu U/ml)", profile.insulin, 0f..300f) { HealthFeatureStore.update { p -> p.copy(insulin = it) } }
            }

            // 4. LIFESTYLE FACTORS
            AssessmentSection("LIFESTYLE FACTORS") {
                AssessmentDropdown("Smoking Status", profile.smoking, listOf("never", "former", "current")) { HealthFeatureStore.update { p -> p.copy(smoking = it) } }
                AssessmentDropdown("Alcohol Consumption", profile.alcohol, listOf("none", "low", "moderate", "high")) { HealthFeatureStore.update { p -> p.copy(alcohol = it) } }
                AssessmentDropdown("Physical Activity", profile.activity, listOf("low", "moderate", "high", "veryHigh")) { HealthFeatureStore.update { p -> p.copy(activity = it) } }
                AssessmentSlider("Sleep (hours)", profile.sleep, 1f..16f) { HealthFeatureStore.update { p -> p.copy(sleep = it) } }
                AssessmentDropdown("Stress Level", profile.stress, listOf("low", "moderate", "high", "veryHigh")) { HealthFeatureStore.update { p -> p.copy(stress = it) } }
                AssessmentDropdown("Salt Intake", profile.salt, listOf("low", "moderate", "high")) { HealthFeatureStore.update { p -> p.copy(salt = it) } }
            }

            // 5. DIET & NUTRITION
            AssessmentSection("DIET & NUTRITION") {
                AssessmentSlider("Vegetable Servings", profile.vegetables, 0f..10f) { HealthFeatureStore.update { p -> p.copy(vegetables = it) } }
                AssessmentSlider("Main Meals/Day", profile.meals, 1f..6f) { HealthFeatureStore.update { p -> p.copy(meals = it) } }
                AssessmentSlider("Water Intake (L)", profile.water, 0f..8f, 0.5f) { HealthFeatureStore.update { p -> p.copy(water = it) } }
                AssessmentDropdown("High-Calorie Food", profile.highCalorie, listOf("yes", "no")) { HealthFeatureStore.update { p -> p.copy(highCalorie = it) } }
                AssessmentDropdown("Snacking Between Meals", profile.snacking, listOf("never", "sometimes", "frequently", "always")) { HealthFeatureStore.update { p -> p.copy(snacking = it) } }
                AssessmentDropdown("Calorie Monitoring", profile.calorieMonitoring, listOf("yes", "no")) { HealthFeatureStore.update { p -> p.copy(calorieMonitoring = it) } }
            }

            // 6. FAMILY & PERSONAL HISTORY
            AssessmentSection("FAMILY & HISTORY") {
                AssessmentDropdown("Family History Diabetes", profile.familyDiabetes, listOf("yes", "no")) { HealthFeatureStore.update { p -> p.copy(familyDiabetes = it) } }
                AssessmentDropdown("Family History Hypertension", profile.familyHypertension, listOf("yes", "no")) { HealthFeatureStore.update { p -> p.copy(familyHypertension = it) } }
                AssessmentDropdown("Family History Overweight", profile.familyOverweight, listOf("yes", "no")) { HealthFeatureStore.update { p -> p.copy(familyOverweight = it) } }
                AssessmentDropdown("Personal Diabetes Status", profile.personalDiabetes, listOf("yes", "no")) { HealthFeatureStore.update { p -> p.copy(personalDiabetes = it) } }
                if (profile.gender == "female") {
                    AssessmentSlider("Pregnancies", profile.pregnancies, 0f..20f) { HealthFeatureStore.update { p -> p.copy(pregnancies = it) } }
                }
            }

            Button(
                onClick = { vm.runAllPredictions() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = !results.isCalculating,
                shape = MaterialTheme.shapes.medium
            ) {
                if (results.isCalculating) CircularProgressIndicator(color = Color.White)
                else Text("GENERATE HEALTH REPORT", fontWeight = FontWeight.Bold)
            }

            if (results.diabetes != null) {
                FinalReportCard(results)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onNavigateToChat,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("💬 DISCUSS WITH AI COACH", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AssessmentSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun AssessmentSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, step: Float = 1f, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(if (step >= 1f) value.toInt().toString() else String.format(Locale.US, "%.1f", value), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = if (step >= 1f) (range.endInclusive - range.start).toInt() - 1 else 0
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentDropdown(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.uppercase(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.uppercase()) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
fun FinalReportCard(r: AssessmentResults) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📋 CLINICAL ASSESSMENT SUMMARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
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
            Text("Certainty: ${String.format(Locale.US, "%.1f", res.confidence)}%", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isRisk) Color(0xFFD32F2F) else Color(0xFF388E3C)
        )
    }
}
