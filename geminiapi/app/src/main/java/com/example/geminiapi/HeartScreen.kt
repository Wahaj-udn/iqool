package com.example.geminiapi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartScreen(
    onBack: () -> Unit,
    viewModel: HeartViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heart Disease Risk") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Evaluate your cardiovascular health risk using clinical parameters and our offline XGBoost model.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Basic Vitals
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Basic Vitals", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.age,
                            onValueChange = { viewModel.updateAge(it) },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sex", style = MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("F")
                                Switch(checked = uiState.isMale, onCheckedChange = { viewModel.updateGender(it) })
                                Text("M")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = uiState.restingBP,
                        onValueChange = { viewModel.updateRestingBP(it) },
                        label = { Text("Resting Blood Pressure (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.cholesterol,
                        onValueChange = { viewModel.updateCholesterol(it) },
                        label = { Text("Serum Cholesterol (mg/dL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Clinical Tests
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Clinical Findings", style = MaterialTheme.typography.titleSmall)
                    
                    HeartDropdown(
                        label = "Chest Pain Type",
                        current = uiState.chestPainType.toInt(),
                        options = listOf("Typical Angina", "Atypical Angina", "Non-anginal Pain", "Asymptomatic"),
                        onSelect = { viewModel.updateChestPain(it.toFloat()) }
                    )

                    HeartDropdown(
                        label = "Resting ECG Results",
                        current = uiState.restECG.toInt(),
                        options = listOf("Normal", "ST-T wave abnormality", "Left ventricular hypertrophy"),
                        onSelect = { viewModel.updateRestECG(it.toFloat()) }
                    )

                    OutlinedTextField(
                        value = uiState.maxHR,
                        onValueChange = { viewModel.updateMaxHR(it) },
                        label = { Text("Max Heart Rate Achieved") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HeartSwitch(
                        label = "Fasting Blood Sugar > 120 mg/dL",
                        checked = uiState.fastingBS,
                        onCheckedChange = { viewModel.updateFastingBS(it) }
                    )

                    HeartSwitch(
                        label = "Exercise Induced Angina",
                        checked = uiState.exerciseAngina,
                        onCheckedChange = { viewModel.updateExerciseAngina(it) }
                    )
                }
            }

            // Advanced Metrics
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Stress & Cardiac Metrics", style = MaterialTheme.typography.titleSmall)
                    
                    OutlinedTextField(
                        value = uiState.oldPeak,
                        onValueChange = { viewModel.updateOldPeak(it) },
                        label = { Text("ST Depression (Oldpeak)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HeartDropdown(
                        label = "Slope of Peak Exercise ST",
                        current = uiState.slope.toInt(),
                        options = listOf("Upsloping", "Flat", "Downsloping"),
                        onSelect = { viewModel.updateSlope(it.toFloat()) }
                    )

                    HeartDropdown(
                        label = "Major Vessels (0-4)",
                        current = uiState.ca.toInt(),
                        options = listOf("0", "1", "2", "3", "4"),
                        onSelect = { viewModel.updateCA(it.toFloat()) }
                    )

                    HeartDropdown(
                        label = "Thalassemia",
                        current = uiState.thal.toInt(),
                        options = listOf("Normal", "Fixed Defect", "Reversible Defect"),
                        onSelect = { viewModel.updateThal(it.toFloat()) }
                    )
                }
            }

            Button(
                onClick = { viewModel.predict() },
                enabled = !uiState.isCalculating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isCalculating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Analyze Heart Risk", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            uiState.resultMessage?.let { label ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Risk Assessment", style = MaterialTheme.typography.labelMedium)
                        Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (uiState.error != null) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartDropdown(label: String, current: Int, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.getOrElse(current) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false })
            }
        }
    }
}

@Composable
fun HeartSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
