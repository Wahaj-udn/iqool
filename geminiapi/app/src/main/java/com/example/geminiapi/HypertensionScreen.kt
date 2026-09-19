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
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypertensionScreen(
    onBack: () -> Unit,
    viewModel: HypertensionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hypertension Risk") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "This model evaluates hypertension risk using 44 features. Initializing the 221MB engine may take a few seconds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Vitals Group
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vitals & Lab Results", style = MaterialTheme.typography.titleSmall)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HyperField("Systolic BP", uiState.systolic, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(systolic = v) } }
                        HyperField("Diastolic BP", uiState.diastolic, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(diastolic = v) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HyperField("Heart Rate", uiState.heartRate, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(heartRate = v) } }
                        HyperField("Glucose", uiState.glucose, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(glucose = v) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HyperField("LDL", uiState.ldl, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(ldl = v) } }
                        HyperField("HDL", uiState.hdl, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(hdl = v) } }
                    }
                    HyperField("Triglycerides", uiState.triglycerides) { v -> viewModel.updateField { it.copy(triglycerides = v) } }
                    HyperField("Total Cholesterol", uiState.cholesterol) { v -> viewModel.updateField { it.copy(cholesterol = v) } }
                }
            }

            // Profile Group
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Patient Profile", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HyperField("Age", uiState.age, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(age = v) } }
                        HyperField("BMI", uiState.bmi, Modifier.weight(1f)) { v -> viewModel.updateField { it.copy(bmi = v) } }
                    }
                    
                    HyperDropdown("Country", uiState.country, listOf("India", "Australia", "Brazil", "Canada", "China", "France", "Germany", "Indonesia", "Italy", "Japan", "Mexico", "Russia", "Saudi Arabia", "South Africa", "South Korea", "Spain", "Turkey", "UK", "USA")) { 
                        viewModel.updateField { s -> s.copy(country = it) }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gender Male", modifier = Modifier.weight(1f))
                        Switch(checked = uiState.isMale, onCheckedChange = { b -> viewModel.updateField { it.copy(isMale = b) } })
                    }
                }
            }

            // Lifestyle Group
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lifestyle & History", style = MaterialTheme.typography.titleSmall)
                    
                    HyperDropdown("Smoking Status", uiState.smoking, listOf("Never", "Former", "Current")) {
                        viewModel.updateField { s -> s.copy(smoking = it) }
                    }
                    HyperDropdown("Activity Level", uiState.activity, listOf("High", "Moderate", "Low")) {
                        viewModel.updateField { s -> s.copy(activity = it) }
                    }
                    
                    HyperField("Stress Level (1-10)", uiState.stress) { v -> viewModel.updateField { it.copy(stress = v) } }
                    HyperField("Salt Intake (g/day)", uiState.salt) { v -> viewModel.updateField { it.copy(salt = v) } }
                    HyperField("Sleep (hrs/night)", uiState.sleep) { v -> viewModel.updateField { it.copy(sleep = v) } }
                    HyperField("Alcohol Intake (drinks/week)", uiState.alcohol) { v -> viewModel.updateField { it.copy(alcohol = v) } }

                    HyperSwitch("Family History BP", uiState.hasFamilyHistory) { b -> viewModel.updateField { it.copy(hasFamilyHistory = b) } }
                    HyperSwitch("Diabetes", uiState.hasDiabetes) { b -> viewModel.updateField { it.copy(hasDiabetes = b) } }
                }
            }

            Button(
                onClick = { viewModel.predict() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Run Full Analysis", fontWeight = FontWeight.Bold)
            }

            uiState.result?.let {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HyperField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperDropdown(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}

@Composable
fun HyperSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
