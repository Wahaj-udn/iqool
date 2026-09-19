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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiabetesScreen(
    onBack: () -> Unit,
    viewModel: DiabetesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diabetes Risk Prediction") },
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
                "Clinical Risk Assessment (8-feature model)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Input Fields Group
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiabetesInputField("Pregnancies", uiState.pregnancies) { viewModel.updatePregnancies(it) }
                    DiabetesInputField("Glucose (mg/dL)", uiState.glucose) { viewModel.updateGlucose(it) }
                    DiabetesInputField("Blood Pressure (mmHg)", uiState.bloodPressure) { viewModel.updateBP(it) }
                    DiabetesInputField("Skin Thickness (mm)", uiState.skinThickness) { viewModel.updateSkin(it) }
                    DiabetesInputField("Insulin (mu U/ml)", uiState.insulin) { viewModel.updateInsulin(it) }
                    DiabetesInputField("BMI", uiState.bmi) { viewModel.updateBmi(it) }
                    DiabetesInputField("Diabetes Pedigree", uiState.pedigree) { viewModel.updatePedigree(it) }
                    DiabetesInputField("Age", uiState.age) { viewModel.updateAge(it) }
                }
            }

            Button(
                onClick = { viewModel.predict() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Analyze Diabetes Risk", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Results Section
            uiState.resultMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isError) MaterialTheme.colorScheme.errorContainer 
                                         else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (uiState.isError) MaterialTheme.colorScheme.onErrorContainer 
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        uiState.probability?.let { prob ->
                            Text(
                                text = "Certainty: ${String.format(Locale.US, "%.2f", prob)}%",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Text(
                text = "Disclaimer: This model uses the Pima Indians Diabetes dataset logic. It is for educational purposes only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DiabetesInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
