package com.example.geminiapi

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminiapi.analysis.*
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class ExerciseAssistUiState(
    val poseResults: PoseLandmarkerResult? = null,
    val inferenceTime: Long = 0L,
    val imageHeight: Int = 0,
    val imageWidth: Int = 0,
    val error: String? = null,
    val status: String = "Initializing...",
    val isFrontCamera: Boolean = false,
    val selectedExercise: String? = null,
    val exerciseName: String = "Detecting...",
    val repCount: Int = 0,
    val confidence: Float = 0f
)

class ExerciseAssistViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ExerciseAssistUiState())
    val uiState: StateFlow<ExerciseAssistUiState> = _uiState.asStateFlow()

    private var poseClassifier: PoseClassifier? = null
    private val heuristicAnalyzer = HeuristicExerciseAnalyzer()
    private val emaSmoothing = EMASmoothing()
    
    // Counter for Squats (KNN based)
    private var squatCounter = RepetitionCounter("squats")

    init {
        loadPoseSamples()
    }

    private fun loadPoseSamples() {
        viewModelScope.launch {
            val samples = withContext(Dispatchers.IO) {
                val allSamples = mutableListOf<PoseSample>()
                val assetManager = getApplication<Application>().assets
                val csvFiles = listOf("pose/squats.csv", "pose/pushups.csv", "pose/situps.csv", "pose/neutral_standing.csv")
                
                for (fileName in csvFiles) {
                    try {
                        assetManager.open(fileName).use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                reader.lineSequence().forEach { line ->
                                    val parts = line.split(",")
                                    if (parts.size >= 101) {
                                        val label = parts[1].trim()
                                        val points = mutableListOf<Point3D>()
                                        for (i in 0 until 33) {
                                            val x = parts[2 + i * 3].toFloatOrNull() ?: 0f
                                            val y = parts[3 + i * 3].toFloatOrNull() ?: 0f
                                            val z = parts[4 + i * 3].toFloatOrNull() ?: 0f
                                            points.add(Point3D(x, y, z))
                                        }
                                        allSamples.add(PoseSample(parts[0], label, PoseEmbedding.getEmbedding(points)))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ExerciseViewModel", "Error loading CSV $fileName: ${e.message}")
                    }
                }
                allSamples
            }
            
            if (samples.isNotEmpty()) {
                poseClassifier = PoseClassifier(samples)
                _uiState.value = _uiState.value.copy(status = "Engine Ready")
            } else {
                _uiState.value = _uiState.value.copy(error = "No pose samples loaded. Check assets/pose folder.")
            }
        }
    }

    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
    }

    fun selectExercise(exercise: String?) {
        _uiState.value = _uiState.value.copy(
            selectedExercise = exercise,
            repCount = 0,
            exerciseName = exercise ?: "Detecting..."
        )
        heuristicAnalyzer.reset()
        squatCounter = RepetitionCounter("squats") // Reset squat counter
    }

    fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        val results = resultBundle.results
        if (results.isEmpty() || results[0].landmarks().isEmpty()) {
            _uiState.value = _uiState.value.copy(
                poseResults = null,
                status = "No person detected",
                confidence = 0f
            )
            return
        }

        val poseResult = results[0]
        val landmarks = poseResult.landmarks()[0]
        val points = landmarks.map { Point3D(it.x(), it.y(), it.z()) }

        val selected = _uiState.value.selectedExercise
        var currentExerciseReps = _uiState.value.repCount
        var activeLabel = _uiState.value.exerciseName
        var displayConfidence = 0f

        if (selected != null) {
            when(selected) {
                "squats" -> {
                    // Revert to KNN for Squats
                    val rawResult = poseClassifier?.classify(points) ?: ClassificationResult("unknown", emptyMap())
                    val smoothedResult = emaSmoothing.getSmoothedResult(rawResult)
                    currentExerciseReps = squatCounter.addClassificationResult(smoothedResult)
                    displayConfidence = smoothedResult.confidences["squats"] ?: 0f
                    activeLabel = "squats"
                }
                "pushups" -> {
                    // More Robust Heuristics for Pushups
                    val hResult = heuristicAnalyzer.analyzePushup(points)
                    currentExerciseReps = hResult.repCount
                    displayConfidence = hResult.progress * 10f
                    activeLabel = "pushups"
                }
                "situps" -> {
                    // Heuristics for Situps
                    val hResult = heuristicAnalyzer.analyzeSitup(points)
                    currentExerciseReps = hResult.repCount
                    displayConfidence = hResult.progress * 10f
                    activeLabel = "situps"
                }
            }
        } else {
            // Auto-detect mode (AI Classification)
            val rawResult = poseClassifier?.classify(points) ?: ClassificationResult("unknown", emptyMap())
            val smoothedResult = emaSmoothing.getSmoothedResult(rawResult)
            
            if (smoothedResult.winnerClassName != "neutral_standing" && smoothedResult.winnerClassName != "unknown") {
                activeLabel = smoothedResult.winnerClassName
            }
            displayConfidence = smoothedResult.confidences[activeLabel] ?: 0f
        }

        _uiState.value = _uiState.value.copy(
            poseResults = poseResult,
            inferenceTime = resultBundle.inferenceTime,
            imageHeight = resultBundle.inputImageHeight,
            imageWidth = resultBundle.inputImageWidth,
            status = if (displayConfidence > 2f) "Form Detected" else "Adjust Position",
            exerciseName = activeLabel,
            repCount = currentExerciseReps,
            confidence = displayConfidence
        )
    }

    fun onError(error: String) {
        _uiState.value = _uiState.value.copy(error = error, status = "Error")
    }

    fun setStatus(status: String) {
        _uiState.value = _uiState.value.copy(status = status)
    }
}
