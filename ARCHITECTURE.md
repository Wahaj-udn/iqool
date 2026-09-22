# HealthSync — Architecture Deep Dive

<img width="1366" height="768" alt="HealthSync Architecture" src="https://github.com/user-attachments/assets/adc01d28-addf-4839-889a-9a1ec0f67038" />




## Table of Contents

- [High-Level Overview](#high-level-overview)
- [Layer Breakdown](#layer-breakdown)
- [State Management](#state-management)
- [Data Flow](#data-flow)
- [ML Subsystem](#ml-subsystem)
- [Native Layer (JNI / llama.cpp)](#native-layer-jni--llamacpp)
- [Navigation Architecture](#navigation-architecture)
- [Health Connect Integration](#health-connect-integration)
- [Camera Pipeline](#camera-pipeline)
- [Threading Model](#threading-model)
- [Design Patterns](#design-patterns)

---

## High-Level Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                      │
│   Screens · Components · Charts · NavBar · Theme               │
└────────────────────────┬────────────────────────────────────────┘
                         │ observes StateFlow
┌────────────────────────▼────────────────────────────────────────┐
│                     ViewModel Layer                             │
│  HomeVM · ExerciseVM · HealthAssessmentVM · BakingVM · ...     │
└──────┬──────────────┬───────────────┬────────────────┬──────────┘
       │              │               │                │
┌──────▼──────┐ ┌─────▼──────┐ ┌────▼────────┐ ┌────▼──────────┐
│HealthFeature│ │ OnnxInfer- │ │ LlamaEngine │ │HealthDataMgr  │
│   Store     │ │   ence     │ │  (JNI/C++)  │ │(Health Connect│
│ (Singleton) │ │(ONNX RT)   │ │             │ │  + Location)  │
└─────────────┘ └────────────┘ └─────────────┘ └───────────────┘
                      │                │
              ┌───────▼──────┐  ┌──────▼───────┐
              │  .onnx files │  │ .gguf model  │
              │  (assets/)   │  │ (Downloads/) │
              └──────────────┘  └──────────────┘
```

The app is structured in three layers — **UI**, **ViewModel**, and **Data/ML** — following unidirectional data flow. State always flows down; events always flow up.

---

## Layer Breakdown

### UI Layer — Jetpack Compose

All screens are stateless Composables that receive state from their ViewModel via `collectAsState()`. They emit events upward through lambda callbacks.

```kotlin
// Pattern used throughout
@Composable
fun SomeScreen(viewModel: SomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // render uiState, call viewModel.doSomething() on user action
}
```

**Shared component library** (`ui/components/`):

| Component | Purpose |
|---|---|
| `AppCard`, `HeroCard`, `LimeCard` | Themed surface containers |
| `MetricTile` | Fixed-height tile with icon, accent colour, custom content |
| `AppButton` | 4 styles: Primary, Dark, Lime, Outline |
| `ProgressRing`, `ProgressBar` | Animated circular / linear progress |
| `LineChart`, `BarChart`, `WaveformChart` | Canvas-based charts with EMA animation |
| `FloatingNavBar` | Dark pill nav with lime active capsule |
| `IconChip` | Rounded icon container used everywhere |
| `RouteSketch` | Decorative SVG-style path drawn on Canvas |

**Theme** (`ui/theme/`):
- Two colour schemes (Light / Dark) built on Material 3 `ColorScheme`
- Brand palette: `Charcoal` (#192126), `Lime` (#BBF246), five metric accents
- Single `Typography` with ExtraBold display weights throughout

---

### ViewModel Layer

Each screen has a dedicated ViewModel holding a `MutableStateFlow<UiState>` sealed/data class. ViewModels:

- Launch coroutines via `viewModelScope`
- Call into data/ML layer on `Dispatchers.IO` or `Dispatchers.Default`
- Post results back to `_uiState` on the main thread

**ViewModel inventory:**

| ViewModel | Owns |
|---|---|
| `HomeViewModel` | `HealthDataManager` — fetches daily stats per selected day |
| `ExerciseAssistViewModel` | `PoseClassifier`, `HeuristicExerciseAnalyzer`, `RepetitionCounter`, `EMASmoothing` |
| `HealthAssessmentViewModel` | 4× `OrtSession` (ONNX), `PreferenceManager` |
| `HealthConnectViewModel` | `HealthConnectClient`, mode/date selection |
| `BakingViewModel` | Gemini `GenerativeModel` + chat session + `HealthDataManager` |
| `LocationViewModel` | `FusedLocationProviderClient`, path point list |
| `NutritionOcrViewModel` | `LlamaEngine.shared`, `HealthDataManager` |
| `LocalLlamaViewModel` | `LlamaEngine.shared` |
| `DiabetesViewModel` | `DiabetesPredictor` (own ONNX session) |
| `HeartViewModel` | `OnnxInference.loadSession("heart_model.onnx")` |
| `HypertensionViewModel` | `HypertensionPredictor` (lazy-loaded 221 MB session) |
| `ObesityViewModel` | `OnnxInference.loadSession("obesity_model.onnx")` |

---

### Data / ML Layer

Three distinct subsystems sit below the ViewModels:

1. **`HealthFeatureStore`** — singleton `object` acting as an in-memory store
2. **ONNX Runtime** — model inference via `OnnxInference` utility object
3. **LlamaEngine** — singleton JNI wrapper around llama.cpp

---

## State Management

### HealthFeatureStore (Global Singleton)

```
HealthFeatureStore
├── _profile: MutableStateFlow<UnifiedHealthProfile>   ← writable
├── _lastResults: MutableStateFlow<AssessmentResults?> ← writable
├── profile: StateFlow<UnifiedHealthProfile>           ← read-only
└── lastResults: StateFlow<AssessmentResults?>         ← read-only
```

`UnifiedHealthProfile` is a single flat data class holding all health parameters (age, gender, vitals, labs, lifestyle, dietary, family history, etc.). Any screen can update it via `HealthFeatureStore.update { ... }`, which also recomputes BMI automatically.

```kotlin
// Updating from HealthAssessmentScreen
HealthFeatureStore.update { p -> p.copy(glucose = v) }

// Reading from BakingViewModel to inject Gemini context
val profile = HealthFeatureStore.profile.value
val results = HealthFeatureStore.lastResults.value
```

This eliminates prop-drilling through the nav graph and avoids a shared ViewModel factory.

### Local UI State Pattern

Each ViewModel defines its own sealed/data class:

```kotlin
data class ExerciseAssistUiState(
    val poseResults: PoseLandmarkerResult? = null,
    val repCount: Int = 0,
    val confidence: Float = 0f,
    val selectedExercise: String? = null,
    // ...
)
```

StateFlow emissions are always full replacements (`copy()`), never mutations — making state changes predictable and diffable by Compose.

---

## Data Flow

### Health Assessment Flow

```
User fills form fields
        │
        ▼ (HealthFeatureStore.update)
UnifiedHealthProfile updated
        │
        ▼ (Button click → HealthAssessmentViewModel.runAllPredictions)
prefManager.saveFullProfile(p)   ← persist to SharedPreferences
        │
        ├──▶ runDiabetes(p)  → OnnxInference.runFullInference → HealthOutputParser.parseDiabetes
        ├──▶ runHeart(p)     → OnnxInference.runFullInference → HealthOutputParser.parseHeart
        ├──▶ runHypertension(p) → HypertensionPredictor.predict
        └──▶ runObesity(p)   → OnnxInference.runFullInference → HealthOutputParser.parseObesity
                │
                ▼ (all on Dispatchers.Default)
        AssessmentResults(diabetes, heart, hypertension, obesity)
                │
                ▼
        HealthFeatureStore.setResults(finalResults)  ← available app-wide
                │
                ▼
        _results.value = finalResults  ← triggers Compose recomposition
```

### Exercise Rep Counting Flow (Squats — KNN path)

```
Camera frame (ImageProxy)
        │
        ▼ PoseLandmarkerHelper.detectLiveStream
MediaPipe → PoseLandmarkerResult (33 landmarks × xyz)
        │
        ▼ ExerciseAssistViewModel.onResults
List<Point3D>
        │
        ├──▶ PoseEmbedding.getEmbedding (normalize + 18 relative vectors)
        │          │
        │          ▼ PoseClassifier.classify (KNN, Top-K=5, weighted distance)
        │     ClassificationResult {winnerClass, confidences}
        │          │
        │          ▼ EMASmoothing.getSmoothedResult (window=10)
        │     smoothed ClassificationResult
        │          │
        │          ▼ RepetitionCounter.addClassificationResult
        │     repCount (Int)
        │
        └──▶ _uiState.value = copy(repCount, confidence, poseResults)
```

### Exercise Rep Counting Flow (Push-ups — Heuristic path)

```
List<Point3D>
        │
        ▼ HeuristicExerciseAnalyzer.analyzePushup
Elbow angle (Shoulder-Elbow-Wrist) via atan2
Torso vertical distance check (isHorizontal guard)
        │
        ▼ State machine: DOWN_ANGLE < 95° → inDownState=true
                         UP_ANGLE  > 155° → pushupReps++, inDownState=false
        │
        ▼ HeuristicResult {repCount, progress, isDown}
```

---

## ML Subsystem

### ONNX Runtime Session Lifecycle

```kotlin
object OnnxInference {
    private val env = OrtEnvironment.getEnvironment()  // process-level singleton

    fun loadSession(context: Context, assetName: String): OrtSession?
    fun runInference(session, inputFloats): Long          // class ID only
    fun runFullInference(session, inputFloats): OrtSession.Result  // class + probs
}
```

- Sessions are opened once per ViewModel and closed in `onCleared()`
- `OrtSession.Result` is returned as `use {}` block to ensure native memory release
- Input tensors are always shape `[1, N]` Float32

**Output parsing** (`HealthOutputParser`):

```
parseDiabetes  → class 1 = "At Risk",  riskScore = probs[1] * 100
parseHeart     → class 1 = "Healthy",  riskScore = probs[0] * 100  (inverted!)
parseHypertension → class 0 = "At Risk", riskScore = probs[0] * 100
parseObesity   → 7-class label array indexed by classId
```

Note the heart model polarity inversion — class 0 is elevated risk; `riskScore` always represents the probability of the bad outcome for UI consistency.

### Pose Embedding & KNN

```
PoseEmbedding.getEmbedding(landmarks: List<Point3D>): List<Point3D>

Step 1 — Translate: subtract hip midpoint from all landmarks
Step 2 — Scale: divide by max(torsoSize × 2.5, maxLandmarkDist) × 100
Step 3 — Extract 18 joint-pair difference vectors:
  left/right shoulder-elbow, elbow-wrist,
  hip-knee, knee-ankle (both sides),
  wrist-to-wrist, ankle-to-ankle,
  shoulder-to-wrist (full arm), hip-to-ankle (full leg),
  shoulder-to-hip (torso), shoulder width, hip width
```

KNN distance metric:
```
dist = sqrt( Σ (diff.x × 1.0)² + (diff.y × 1.0)² + (diff.z × 0.2)² ) / N
```
Z-axis is down-weighted (0.2) because depth from a single monocular camera is noisy.

EMA smoothing per class:
```
history[class].prepend(confidence)
smoothed[class] = mean(history[class][-windowSize:])
```

---

## Native Layer (JNI / llama.cpp)

```
Kotlin: LlamaEngine.shared.completion(prompt, thinkingEnabled, onToken)
              │
              ▼ (JNI)
C++: Java_com_example_geminiapi_llama_LlamaEngine_doCompletion
              │
              ├── Clear KV cache (llama_memory_seq_rm)
              ├── Apply Qwen chat template (system + user + assistant prefix)
              ├── Tokenize → llama_batch
              ├── llama_decode (prefill)
              └── Generation loop (max 256 tokens):
                    llama_sampler_sample → token
                    llama_vocab_is_eog? → break
                    llama_token_to_piece → string piece
                    JNI callback → Kotlin onToken(piece)   ← streaming
                    llama_decode (single token extend)
```

**LlamaEngine singleton pattern:**

```kotlin
companion object {
    val shared by lazy { LlamaEngine() }  // one instance, reused across screens
}
```

The model stays loaded in memory once initialized. `NutritionOcrViewModel` and `LocalLlamaViewModel` both reference `LlamaEngine.shared`, so loading the model in one screen makes it immediately available in the other.

**Sampler chain:**
```
Top-K(40) → Top-P(0.95, min_keep=1) → Temp(0.7) → Dist(DEFAULT_SEED)
```

---

## Navigation Architecture

Navigation is entirely managed by a single `NavHost` in `MainActivity`. There is no fragment back stack.

```
NavHost(startDestination = "home" | "welcome")
│
├── "welcome"       → WelcomeScreen
├── "onboarding"    → OnboardingScreen
├── "home"          → HomeScreen          ┐
├── "workout"       → WorkoutScreen       │ Bottom nav
├── "health_dashboard" → HealthScreen     │ tabs
├── "profile"       → ProfileScreen       ┘
├── "run"           → RunScreen
├── "exercise"      → ExerciseAssistScreen
├── "assessment"    → HealthAssessmentScreen
├── "ai_coach"      → AICoachScreen
├── "nutrition_ocr" → NutritionOcrScreen
├── "health"        → HealthConnectScreen
├── "local_chat"    → LocalLlamaScreen
├── "old_ai_chat"   → BakingScreen
├── "diabetes"      → DiabetesScreen
├── "heart"         → HeartScreen
├── "hypertension"  → HypertensionScreen
└── "obesity"       → ObesityScreen
```

**Bottom bar synchronisation:**

```kotlin
val currentRoute = navBackStackEntry?.destination?.route
LaunchedEffect(currentRoute) {
    selectedTab = when(currentRoute) {
        "home" -> 0;  "workout" -> 1
        "health_dashboard" -> 2;  "profile" -> 3
        else -> selectedTab   // preserve tab on sub-screens
    }
}
```

The bottom bar is hidden on all sub-screens (any route not in the top-level set).

---

## Health Connect Integration

```
HealthConnectClient (singleton per context)
        │
        ├── permissionController.getGrantedPermissions()
        │         → if empty: PermissionsRequired state
        │
        ├── aggregate(AggregateRequest) → steps, avg HR, distance, sleep total
        │
        ├── aggregateGroupByDuration(duration = 1 day)
        │         → List<AggregationResultGroupedByDuration>
        │         → used for Week/Month views
        │
        └── readRecords(HeightRecord / WeightRecord, pageSize=1, ascending=false)
                  → most recent value only
```

`HealthDataManager.fetchLast15DaysSummary()` returns a formatted string (not a data class) specifically designed to be pasted verbatim into the Gemini prompt context window. Daily buckets are formatted as:

```
Date: 2026-09-08
- Steps: 8432
- Avg Heart Rate: 71.0 BPM
- Distance: 6.12 km
- Sleep: 7h 22m
```

---

## Camera Pipeline

### Exercise Assist (Live Pose Detection)

```
CameraX ImageAnalysis (STRATEGY_KEEP_ONLY_LATEST)
        │  single background thread executor
        ▼
PoseLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
        │
        ├── imageProxy.toBitmap()
        ├── Matrix.postRotate(rotationDegrees)
        ├── BitmapImageBuilder → MPImage
        └── poseLandmarker.detectAsync(mpImage, uptimeMillis)
                │  async result callback
                ▼
        returnLivestreamResult → poseLandmarkerHelperListener.onResults
                │
                ▼ (main thread via ViewModel)
        ExerciseAssistViewModel.onResults → classify → uiState update
                │
                ▼ (Compose)
        PoseOverlay Canvas redraws landmarks + connections
```

Front camera mirroring:
```kotlin
val x = if (isFrontCamera) (1f - landmark.x()) * canvasWidth
        else landmark.x() * canvasWidth
```

### Nutrition OCR (Still Image)

```
User: Take Photo (CameraX ImageCapture) or Pick from Gallery (GetContent)
        │
        ▼ InputImage.fromFilePath / fromBitmap
ML Kit TextRecognition.process(inputImage)
        │
        ▼ VisionText (blocks → lines → elements with bounding boxes)
SpatialTextReconstructor.reconstruct(visionText)
        │  Groups lines by vertical proximity (threshold = 65% of taller element height)
        │  Sorts within row left-to-right by boundingBox.left
        │  Multi-element rows joined with " : " separator
        ▼
Structured text string → viewModel.onTextExtracted(text)
        │
        ▼ parseStructuredNutrition (regex on each line)
Map<String, Double?> {calories, fat, carbs, protein, sodium, ...}
        │
        ▼ assessFood() → LlamaEngine.shared.completion(buildPrompt(...))
```

---

## Threading Model

| Operation | Dispatcher | Notes |
|---|---|---|
| UI rendering | Main | All Compose state reads |
| ONNX inference | `Dispatchers.Default` | CPU-bound, parallel to UI |
| Health Connect queries | `viewModelScope` (IO internally) | Suspend functions |
| llama.cpp completion | `Dispatchers.IO` | Blocks thread for duration of generation |
| LlamaEngine token callback | IO thread → `Dispatchers.Main` via `viewModelScope.launch` | Each token posted to main |
| GPS location callbacks | Main Looper | `LocationCallback` registered on main |
| Camera analysis | Single-thread executor | `Executors.newSingleThreadExecutor()` per screen |
| Gemini API call | `Dispatchers.IO` | `chat.sendMessage` is a suspending function |
| ML Kit OCR | Internal thread pool | Callback-based, results on calling thread |

---

## Design Patterns

| Pattern | Where Used |
|---|---|
| **MVVM** | All screens — ViewModel holds state, UI observes |
| **Unidirectional Data Flow** | StateFlow down, lambda events up |
| **Singleton Store** | `HealthFeatureStore`, `LlamaEngine.shared`, `OnnxInference.env` |
| **Repository-lite** | `HealthDataManager` abstracts Health Connect API |
| **Strategy** | `HeuristicExerciseAnalyzer` vs KNN path selected at runtime per exercise |
| **State Machine** | `RepetitionCounter` (enter/exit thresholds), `HeuristicExerciseAnalyzer` (`inDownState`) |
| **Lazy Loading** | Hypertension model (221 MB) loaded only when assessment runs; Qwen3 loaded only on demand |
| **Object Pool** | `OrtEnvironment` shared across all ONNX sessions via `OnnxInference.env` |
| **Observer** | `StateFlow` + `collectAsState()` throughout; `OfflineRegion.OfflineRegionObserver` for map tiles |
| **Command** | Each `viewModel.updateXxx()` is a single-field update command returning a new state copy |
