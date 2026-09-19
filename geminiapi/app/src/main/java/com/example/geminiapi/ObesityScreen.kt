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
fun ObesityScreen(
    onBack: () -> Unit,
    viewModel: ObesityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.syncWithHealthConnect()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Obesity Level Analysis") },
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
                "Provide your lifestyle details to analyze your weight category using our 94.8% accurate offline model.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Basic Info
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Basic Information", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.age,
                            onValueChange = { viewModel.updateAge(it) },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gender", style = MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("F")
                                Switch(checked = uiState.isMale, onCheckedChange = { viewModel.updateGender(it) })
                                Text("M")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = uiState.height,
                        onValueChange = { viewModel.updateHeight(it) },
                        label = { Text("Height (m)") },
                        placeholder = { Text("e.g. 1.75") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.weight,
                        onValueChange = { viewModel.updateWeight(it) },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.syncWithHealthConnect() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sync with Health Connect")
                    }
                }
            }

            // Eating Habits
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Eating Habits", style = MaterialTheme.typography.titleSmall)
                    
                    HabitSlider(
                        label = "Vegetables frequency (1-3)",
                        value = uiState.fcvc,
                        onValueChange = { viewModel.updateFcvc(it) },
                        range = 1f..3f
                    )
                    
                    HabitSlider(
                        label = "Meals per day",
                        value = uiState.ncp,
                        onValueChange = { viewModel.updateNcp(it) },
                        range = 1f..4f
                    )

                    HabitSlider(
                        label = "Water intake (1-3)",
                        value = uiState.ch2o,
                        onValueChange = { viewModel.updateCh2o(it) },
                        range = 1f..3f
                    )

                    HabitSwitch(label = "Family history with overweight", checked = uiState.familyHistory, onCheckedChange = { viewModel.updateFamilyHistory(it) })
                    HabitSwitch(label = "Frequent high caloric food", checked = uiState.favc, onCheckedChange = { viewModel.updateFavc(it) })

                    DropdownSelection(
                        label = "Eats between meals",
                        current = uiState.caec,
                        options = listOf("no", "Sometimes", "Frequently", "Always"),
                        onSelect = { viewModel.updateCaec(it) }
                    )
                }
            }

            // Lifestyle
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lifestyle & Activity", style = MaterialTheme.typography.titleSmall)

                    HabitSlider(
                        label = "Physical activity frequency (0-3)",
                        value = uiState.faf,
                        onValueChange = { viewModel.updateFaf(it) },
                        range = 0f..3f
                    )

                    HabitSlider(
                        label = "Tech usage duration (0-2)",
                        value = uiState.tue,
                        onValueChange = { viewModel.updateTue(it) },
                        range = 0f..2f
                    )

                    HabitSwitch(label = "Smokes", checked = uiState.smoke, onCheckedChange = { viewModel.updateSmoke(it) })
                    HabitSwitch(label = "Monitors calorie consumption", checked = uiState.scc, onCheckedChange = { viewModel.updateScc(it) })

                    DropdownSelection(
                        label = "Alcohol consumption",
                        current = uiState.calc,
                        options = listOf("no", "Sometimes", "Frequently", "Always"),
                        onSelect = { viewModel.updateCalc(it) }
                    )

                    DropdownSelection(
                        label = "Primary Transport",
                        current = uiState.transport,
                        options = listOf("Walking", "Bike", "Motorbike", "Public_Transportation", "Automobile"),
                        onSelect = { viewModel.updateTransport(it) }
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
                    Text("Analyze Weight Category", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Result
            uiState.resultLabel?.let { label ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Analysis Result", style = MaterialTheme.typography.labelMedium)
                        Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

@Composable
fun HabitSlider(label: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(String.format("%.1f", value), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
fun HabitSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelection(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
