package com.example.geminiapi.llama

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalLlamaScreen(
    onBack: () -> Unit,
    viewModel: LocalLlamaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var prompt by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    // Check for "All Files Access" permission
    var hasFullStorageAccess by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true 
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFullStorageAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Qwen AI (llama.cpp)") },
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasFullStorageAccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permission Required", fontWeight = FontWeight.Bold)
                        Text("Please grant All Files Access to load the model file.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            }
                        }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else if (!uiState.isLoaded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Model Status: NOT LOADED", fontWeight = FontWeight.Bold)
                        Text("Expected Path: /Download/Qwen3-4B-Q4_K_M.gguf", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadQwenModel() },
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Load 2.5GB Qwen Model")
                        }
                    }
                }
            } else {
                Text("✅ Qwen-4B Model Loaded Successfully", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Enter prompt for local AI...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    enabled = !uiState.isGenerating
                )

                Button(
                    onClick = { 
                        viewModel.sendPrompt(prompt)
                        prompt = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isGenerating && prompt.isNotBlank()
                ) {
                    if (uiState.isGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI is thinking...")
                        }
                    } else {
                        Text("Inference (Native)")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Local Response:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.response)
                        
                        // Auto-scroll when new text arrives
                        LaunchedEffect(uiState.response) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
