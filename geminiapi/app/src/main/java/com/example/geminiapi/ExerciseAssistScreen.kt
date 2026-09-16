package com.example.geminiapi

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executors

@Composable
fun ExerciseAssistScreen(
    onBack: () -> Unit,
    viewModel: ExerciseAssistViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            viewModel.setStatus("Permission Denied")
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            ExerciseTopBar(
                title = if (uiState.selectedExercise == null) "Select Exercise" else uiState.selectedExercise!!.uppercase(),
                onBack = {
                    if (uiState.selectedExercise != null) {
                        viewModel.selectExercise(null)
                    } else {
                        onBack()
                    }
                },
                onFlipCamera = if (uiState.selectedExercise != null) { { viewModel.toggleCamera() } } else null
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.selectedExercise == null) {
                ExerciseSelector(onSelect = { viewModel.selectExercise(it) })
            } else {
                if (hasCameraPermission) {
                    CameraWithPoseOverlay(
                        isFrontCamera = uiState.isFrontCamera,
                        onResults = { viewModel.onResults(it) },
                        onError = { viewModel.onError(it) }
                    )

                    PoseOverlay(
                        results = uiState.poseResults,
                        imageWidth = uiState.imageWidth,
                        imageHeight = uiState.imageHeight,
                        isFrontCamera = uiState.isFrontCamera,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating Exit Button
                    IconButton(
                        onClick = { viewModel.selectExercise(null) },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Exercise", tint = Color.White)
                    }

                    // Big Rep Counter at the top
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.repCount.toString(),
                                    color = Color.Cyan,
                                    fontSize = 80.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "REPS",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Info Card at the bottom
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                LinearProgressIndicator(
                                    progress = { uiState.confidence / 10f },
                                    modifier = Modifier.width(100.dp),
                                    color = if (uiState.confidence > 6f) Color.Green else Color.Yellow
                                )
                                Text("Accuracy: ${(uiState.confidence * 10).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            }
                            Text("Mode: ${if (uiState.isFrontCamera) "Front" else "Rear"}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Camera permission is required.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Allow Camera")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseSelector(onSelect: (String) -> Unit) {
    val exercises = listOf("squats", "pushups", "situps")
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Choose your exercise", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 32.dp))
        exercises.forEach { exercise ->
            Button(
                onClick = { onSelect(exercise) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(80.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(exercise.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraWithPoseOverlay(
    isFrontCamera: Boolean,
    onResults: (PoseLandmarkerHelper.ResultBundle) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    val landmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            poseLandmarkerHelperListener = object : PoseLandmarkerHelper.LandmarkerListener {
                override fun onError(error: String) { onError(error) }
                override fun onResults(results: PoseLandmarkerHelper.ResultBundle) {
                    onResults(results)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            landmarkerHelper.clearPoseLandmarker()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            landmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("CameraView", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor) { imageProxy ->
                            landmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
                            imageProxy.close()
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) {
                    Log.e("CameraView", "Re-binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTopBar(
    title: String,
    onBack: () -> Unit,
    onFlipCamera: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (onFlipCamera != null) {
                IconButton(onClick = onFlipCamera) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera")
                }
            }
        }
    )
}
