package com.example.geminiapi.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapi.ui.components.AppButton
import com.example.geminiapi.ui.components.AppButtonStyle
import com.example.geminiapi.ui.components.AppIcons
import com.example.geminiapi.ui.components.ScreenHeader
import com.example.geminiapi.llama.SpatialTextReconstructor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun NutritionOcrScreen(
    onBack: () -> Unit,
    viewModel: NutritionOcrViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var isCameraActive by remember { mutableStateOf(false) }
    var localLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            localLoading = true
            localError = null
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val text = SpatialTextReconstructor.reconstruct(visionText)
                        viewModel.onTextExtracted(text)
                        localLoading = false
                    }
                    .addOnFailureListener { e ->
                        localError = "OCR Processing Failed: ${e.localizedMessage}"
                        localLoading = false
                    }
            } catch (e: Exception) {
                localError = "Failed to load image: ${e.localizedMessage}"
                localLoading = false
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCameraActive = true
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val displayError = localError ?: uiState.error

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "Scan Label",
            subtitle = "Offline Nutrition Text Extractor",
            trailing = {
                TextButton(onClick = onBack) {
                    Text("Back", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        if (isCameraActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            controller = cameraController
                            cameraController.bindToLifecycle(lifecycleOwner)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppButton(
                            text = "Capture",
                            onClick = {
                                localLoading = true
                                localError = null
                                val photoFile = File(context.cacheDir, "ocr_capture_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                cameraController.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            isCameraActive = false
                                            try {
                                                val inputImage = InputImage.fromFilePath(context, Uri.fromFile(photoFile))
                                                textRecognizer.process(inputImage)
                                                    .addOnSuccessListener { visionText ->
                                                        val text = SpatialTextReconstructor.reconstruct(visionText)
                                                        viewModel.onTextExtracted(text)
                                                        localLoading = false
                                                    }
                                                    .addOnFailureListener { e ->
                                                        localError = "OCR Failed: ${e.localizedMessage}"
                                                        localLoading = false
                                                    }
                                            } catch (e: Exception) {
                                                localError = "Error loading captured file: ${e.localizedMessage}"
                                                localLoading = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            localError = "Capture failed: ${exception.localizedMessage}"
                                            localLoading = false
                                        }
                                    }
                                )
                            },
                            style = AppButtonStyle.Dark,
                            icon = AppIcons.Camera
                        )

                        AppButton(
                            text = "Cancel",
                            onClick = { isCameraActive = false },
                            style = AppButtonStyle.Outline
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppButton(
                        text = "Take Photo",
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                isCameraActive = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        style = AppButtonStyle.Dark,
                        icon = AppIcons.Camera,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    AppButton(
                        text = "Upload Photo",
                        onClick = { imagePickerLauncher.launch("image/*") },
                        style = AppButtonStyle.Dark,
                        icon = AppIcons.Edit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (localLoading || uiState.isLoading || uiState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        displayError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (uiState.extractedText.isNotEmpty()) {
            Text(
                text = "Extracted Nutrition Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = uiState.extractedText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AppButton(
                text = if (uiState.isGenerating) "Assessing..." else "Assess for Me",
                onClick = { viewModel.assessFood() },
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.Dark,
                icon = AppIcons.Sparkle
            )
        }

        if (uiState.assessmentResponse.isNotEmpty()) {
            Text(
                text = "Personalized Food Assessment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = uiState.assessmentResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (uiState.extractedText.isNotEmpty() || displayError != null) {
            AppButton(
                text = "Clear / Scan Another",
                onClick = {
                    viewModel.clear()
                    localError = null
                },
                modifier = Modifier.fillMaxWidth(),
                style = AppButtonStyle.Outline,
                icon = AppIcons.Sync
            )
        }
    }
}
