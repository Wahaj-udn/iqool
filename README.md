# HealthSync — AI-Powered Personal Health Assistant

> A comprehensive Android health & fitness app combining on-device ML inference, real-time pose detection, GPS run tracking, and LLM-based coaching — all privacy-first with local processing.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Screenshots & Screens](#screens)
- [ML Models](#ml-models)
- [Setup & Build](#setup--build)
- [Project Structure](#project-structure)
- [Permissions](#permissions)
- [Dependencies](#dependencies)
- [Known Limitations](#known-limitations)

---

## Overview

HealthSync is a native Android application that puts a full suite of health intelligence on-device. It uses ONNX Runtime to run trained ML models for disease risk assessment, MediaPipe for real-time pose landmark detection, llama.cpp (via JNI) for offline LLM inference, ML Kit for nutrition label OCR, and MapLibre for offline GPS run tracking — all without sending sensitive health data to external servers.

The Gemini API is used only for the conversational AI coach, with user health context injected at runtime via the Android Health Connect API.

---

## Key Features

### Health Assessment
- **Diabetes Risk** — 8-feature ONNX model (Pima Indians dataset logic)
- **Heart Disease Risk** — 13-feature XGBoost ONNX model
- **Hypertension Risk** — 44-feature ONNX model (221 MB, lazy-loaded)
- **Obesity Classification** — 7-class ONNX model with 94.8% accuracy across weight categories
- Unified health profile feeds all four models simultaneously via `HealthAssessmentViewModel`

### Live Exercise Coach
- Real-time pose landmark detection via **MediaPipe Pose Landmarker Lite** running on-device (CPU)
- Rep counting for **Squats** (KNN classifier + EMA smoothing), **Push-ups** (heuristic elbow angle), **Sit-ups** (heuristic hip angle)
- Front/rear camera toggle with mirrored landmark rendering
- Visual skeleton overlay drawn on a Compose `Canvas`

### Run Tracking
- High-accuracy GPS tracking (1 s interval, 500 ms min update) via **Fused Location Provider**
- Real-time route rendering on an **offline MapLibre** map (OpenFreeMap tiles)
- Path smoothing: ignores points with accuracy > 20 m, skips points < 2 m apart
- Offline tile download for Hyderabad region (zoom 10–15)

### AI Coaching
- **Gemini Flash** — cloud conversational coach with full health context injected on first message (profile, vitals, ONNX results, 15-day Health Connect summary)
- **Local Qwen3-4B** — fully offline LLM running via llama.cpp JNI bridge (2.5 GB GGUF, streaming token output)
- **Nutrition OCR** — ML Kit text recognition + spatial reconstruction fed into local Qwen3 for personalized food assessment

### Health Connect Integration
- Reads Steps, Heart Rate, Distance, Sleep, Height, Weight
- Day / Week / Month aggregated views
- Auto-syncs height & weight into the obesity assessment form

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow (MVVM) |
| ML Inference | ONNX Runtime Android (`onnxruntime-android`) |
| Pose Detection | MediaPipe Tasks Vision |
| Local LLM | llama.cpp via JNI (C++17, CMake) |
| OCR | Google ML Kit Text Recognition |
| Maps | MapLibre Android SDK (offline tiles) |
| Cloud AI | Google Generative AI SDK (Gemini) |
| Health Data | Android Health Connect (`androidx.health.connect`) |
| Location | Google Play Services — Fused Location Provider |
| Camera | CameraX (camera-core, camera2, lifecycle, view) |
| Build | Gradle 9.6.0 + AGP 9.4.0 + Kotlin 2.2.10 |
| NDK | r30.0.16248370 (arm64-v8a, x86_64) |
| Min SDK | 27 (Android 8.1) |
| Target SDK | 37 |

---

## Architecture

```
MainActivity
│
├── NavHost (Compose Navigation)
│   ├── WelcomeScreen / OnboardingScreen
│   ├── HomeScreen          ← HomeViewModel (HealthDataManager)
│   ├── WorkoutScreen       ← navigation only
│   │   ├── RunScreen       ← LocationViewModel (FusedLocation + MapLibre)
│   │   └── ExerciseAssistScreen ← ExerciseAssistViewModel
│   │                              (PoseClassifier + HeuristicAnalyzer)
│   ├── HealthScreen        ← HealthFeatureStore (singleton)
│   │   └── HealthAssessmentScreen ← HealthAssessmentViewModel
│   │                                (4× ONNX sessions)
│   ├── ProfileScreen
│   │   └── HealthConnectScreen ← HealthConnectViewModel
│   ├── AICoachScreen       ← BakingViewModel (Gemini)
│   ├── NutritionOcrScreen  ← NutritionOcrViewModel (ML Kit + LlamaEngine)
│   └── LocalLlamaScreen    ← LocalLlamaViewModel (llama.cpp JNI)
│
├── HealthFeatureStore      ← Singleton StateFlow store for unified health profile
└── PreferenceManager       ← SharedPreferences persistence
```

**Key design choices:**
- `HealthFeatureStore` is a singleton `object` holding a `MutableStateFlow<UnifiedHealthProfile>` and `MutableStateFlow<AssessmentResults?>`, making health data available across all screens without passing arguments through the nav graph.
- ONNX sessions are opened per-ViewModel and closed in `onCleared()` to avoid native memory leaks. The 221 MB hypertension model is lazy-loaded on first assessment run.
- `LlamaEngine` is a singleton (`shared` lazy instance) so the 2.5 GB Qwen model stays resident across screens once loaded.

---

## Screens

| Screen | Route | Description |
|---|---|---|
| Welcome | `welcome` | Animated intro, user enters their name |
| Onboarding | `onboarding` | 3-step flow: basic info → lifestyle/vitals → permissions |
| Home | `home` | Daily summary, week strip, AI insight card |
| Workout | `workout` | Run card + Exercise Coach card |
| Run | `run` | Live GPS map, start/stop tracking, offline MapLibre |
| Exercise Assist | `exercise` | Camera + pose overlay, rep counter |
| Health Dashboard | `health_dashboard` | Health report card, resting HR chart, assessment areas |
| Health Assessment | `assessment` | Full multi-section form → 4-model inference → report |
| AI Coach | `ai_coach` | Gemini chat with health context |
| Nutrition OCR | `nutrition_ocr` | Camera/gallery → ML Kit OCR → Qwen3 assessment |
| Profile | `profile` | Personal info, Health Connect, privacy info |
| Health Connect | `health` | Day/Week/Month Health Connect data viewer |
| Local LLM | `local_chat` | Offline Qwen3-4B chat via llama.cpp |

---

## ML Models

All models are stored in `app/src/main/assets/` and run fully on-device.

### Disease Risk Models (ONNX Runtime)

| Model File | Task | Input Features | Output |
|---|---|---|---|
| `diabetes_model.onnx` | Diabetes risk | 8 (pregnancies, glucose, BP, skin thickness, insulin, BMI, pedigree, age) | Class 0/1 + probabilities |
| `heart_model.onnx` | Heart disease risk | 13 (age, sex, chest pain, resting BP, cholesterol, fasting BS, ECG, max HR, angina, oldpeak, slope, CA, thal) | Class 0/1 + probabilities |
| `hypertension_model.onnx` | Hypertension risk | 44 (continuous vitals + one-hot country/smoking/activity/history flags) | Class 0/1 |
| `obesity_model.onnx` | Obesity classification | 23 (age, height, weight, lifestyle factors + one-hot encoded categoricals) | 7-class (Insufficient Weight → Obesity Type III) |

Output parsing is handled by `HealthOutputParser` — note that label polarity differs per model (e.g. heart model: class 1 = Healthy, class 0 = At Risk).

### Pose Classification (MediaPipe + KNN)

- Model: `pose_landmarker_lite.task` (MediaPipe, CPU delegate)
- 33 body landmarks → normalized `PoseEmbedding` (18 relative joint vectors, torso-size normalized)
- KNN classifier with Top-K=5 over CSV training data in `assets/pose/` (squats, pushups, situps, neutral_standing)
- EMA smoothing (window=10) → `RepetitionCounter` with enter/exit confidence thresholds

### Local LLM

- Model: `Qwen3-4B-Q4_K_M.gguf` (user-downloaded to `/storage/emulated/0/Download/`)
- Runtime: llama.cpp compiled via CMake for `arm64-v8a` and `x86_64`
- Context: 2048 tokens, 8 threads, batch 512
- Sampler chain: Top-K(40) → Top-P(0.95) → Temp(0.7) → Dist
- Qwen chat template applied in `llama-jni.cpp`
- Streaming: each token is passed back to Kotlin via a JNI callback

---

## Setup & Build

### Prerequisites

- Android Studio Hedgehog or newer
- NDK `r30.0.16248370` (set in `build.gradle.kts`)
- CMake 4.1.2
- llama.cpp source at `D:/Wahaj/AI/llama.cpp` (path configured in `CMakeLists.txt` — **update this for your machine**)
- JDK 25 (toolchain via Foojay resolver)

### Steps

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd geminiapi
   ```

2. **Update the llama.cpp path** in `app/src/main/cpp/CMakeLists.txt`:
   ```cmake
   set(LLAMA_DIR "/your/path/to/llama.cpp")
   ```

3. **Place ONNX model files** in `app/src/main/assets/`:
   - `diabetes_model.onnx`
   - `heart_model.onnx`
   - `hypertension_model.onnx`
   - `obesity_model.onnx`

4. **Place MediaPipe model** in `app/src/main/assets/`:
   - `pose_landmarker_lite.task`

5. **Place pose CSV training data** in `app/src/main/assets/pose/`:
   - `squats.csv`, `pushups.csv`, `situps.csv`, `neutral_standing.csv`

6. **Configure Firebase/Google Services** — `google-services.json` is already present (project: `com-example-geminiapi-9f83b`).

7. **Gemini API Key** — set in `BakingViewModel.kt`:
   ```kotlin
   private val apiKey = "YOUR_GEMINI_API_KEY"
   ```

8. **Build & run:**
   ```bash
   ./gradlew assembleDebug
   ```

9. **For local LLM** — download `Qwen3-4B-Q4_K_M.gguf` and place it in the device's `/storage/emulated/0/Download/` folder. Grant "All Files Access" permission from the app's Local LLM screen.

---

## Project Structure

```
geminiapi/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── pose/           # KNN training CSVs
│   │   │   ├── *.onnx          # Disease risk models
│   │   │   └── *.task          # MediaPipe model
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   └── llama-jni.cpp   # JNI bridge to llama.cpp
│   │   └── java/com/example/geminiapi/
│   │       ├── MainActivity.kt
│   │       ├── analysis/
│   │       │   ├── OnnxInference.kt       # ONNX session management
│   │       │   ├── HealthOutputParser.kt  # Model output → ModelResult
│   │       │   ├── PoseAnalysisClasses.kt # KNN classifier, EMA, counters
│   │       │   ├── HeuristicExerciseAnalyzer.kt
│   │       │   ├── DiabetesPredictor.kt
│   │       │   └── HypertensionPredictor.kt
│   │       ├── llama/
│   │       │   ├── LlamaEngine.kt         # Singleton JNI wrapper
│   │       │   ├── LocalLlamaViewModel.kt
│   │       │   ├── LocalLlamaScreen.kt
│   │       │   └── SpatialTextReconstructor.kt  # ML Kit OCR layout recovery
│   │       ├── ui/
│   │       │   ├── components/            # Reusable Compose components
│   │       │   │   ├── Charts.kt          # Line, Bar, Waveform, Progress charts
│   │       │   │   ├── Components.kt      # AppCard, MetricTile, AppButton, etc.
│   │       │   │   ├── FloatingNavBar.kt
│   │       │   │   ├── AppIcons.kt
│   │       │   │   └── DateUtils.kt
│   │       │   ├── screens/
│   │       │   │   ├── HomeScreen.kt
│   │       │   │   ├── WorkoutScreen.kt
│   │       │   │   ├── HealthScreen.kt
│   │       │   │   ├── ProfileScreen.kt
│   │       │   │   ├── AICoachScreen.kt
│   │       │   │   ├── NutritionOcrScreen.kt
│   │       │   │   ├── NutritionOcrViewModel.kt
│   │       │   │   ├── HomeViewModel.kt
│   │       │   │   ├── OnboardingScreen.kt
│   │       │   │   └── WelcomeScreen.kt
│   │       │   └── theme/
│   │       │       ├── Color.kt    # Brand palette (Charcoal, Lime, accents)
│   │       │       ├── Theme.kt    # Light/Dark MaterialTheme
│   │       │       └── Type.kt     # Typography scale
│   │       ├── BakingViewModel.kt       # Gemini AI coach
│   │       ├── HealthFeatureStore.kt    # Singleton state store
│   │       ├── HealthDataManager.kt     # Health Connect queries
│   │       ├── HealthConnectViewModel.kt
│   │       ├── HealthAssessmentViewModel.kt
│   │       ├── ExerciseAssistViewModel.kt
│   │       ├── LocationViewModel.kt
│   │       ├── DiabetesViewModel.kt
│   │       ├── HeartViewModel.kt
│   │       ├── HypertensionViewModel.kt
│   │       ├── ObesityViewModel.kt
│   │       ├── PoseLandmarkerHelper.kt
│   │       ├── PoseOverlay.kt
│   │       ├── PreferenceManager.kt
│   │       └── RunScreen.kt
│   └── build.gradle.kts
└── gradle/
    ├── libs.versions.toml
    └── wrapper/gradle-wrapper.properties
```

---

## Permissions

| Permission | Purpose |
|---|---|
| `CAMERA` | Exercise Assist pose detection, Nutrition OCR |
| `ACCESS_FINE_LOCATION` | GPS run tracking |
| `ACCESS_COARSE_LOCATION` | GPS fallback |
| `MANAGE_EXTERNAL_STORAGE` | Loading Qwen3 GGUF from Downloads |
| `READ_EXTERNAL_STORAGE` | Legacy storage access |
| `health.READ_STEPS` | Health Connect steps data |
| `health.READ_HEART_RATE` | Health Connect HR data |
| `health.READ_DISTANCE` | Health Connect distance data |
| `health.READ_SLEEP_SESSION` | Health Connect sleep data |
| `health.READ_HEIGHT` | Health Connect height (auto-fill) |
| `health.READ_WEIGHT` | Health Connect weight (auto-fill) |

---

## Dependencies

Key library versions (see `gradle/libs.versions.toml` for full list):

| Library | Version |
|---|---|
| AGP | 9.4.0 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.8.8 |
| Google Generative AI SDK | 0.9.0 |
| MapLibre Android SDK | 13.6.1 |
| CameraX | 1.4.1 |
| MediaPipe Tasks Vision | 0.10.14 |
| ONNX Runtime Android | 1.30.0 |
| Health Connect | 1.1.0 |
| Play Services Location | 21.4.0 |
| ML Kit Text Recognition | 16.0.1 |

---

## Known Limitations

- **llama.cpp path is hardcoded** to `D:/Wahaj/AI/llama.cpp` in CMakeLists.txt — must be updated per machine before building.
- **Hypertension model (221 MB)** has a noticeable cold-start delay on first inference; subsequent runs reuse the loaded session.
- **MapLibre offline tiles** require a one-time download (Hyderabad region only by default). Change `LatLngBounds` in `RunScreen.kt` for other regions.
- **Local Qwen3 requires All Files Access** — a broad permission granted only at user request. The model path is hardcoded to `/storage/emulated/0/Download/`.
- **Resting HR and sleep data** on the Home screen fall back to hardcoded values (75 bpm, 8h 22m) when Health Connect has no data for the selected day.
- The app targets emulator environments by skipping MapLibre init when `Build.PRODUCT` contains "sdk".
- Gemini API key is embedded in source — use `local.properties` or Android secrets plugin for production.
