package com.example.geminiapi.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.example.geminiapi.HealthDataManager
import com.example.geminiapi.HealthFeatureStore
import com.example.geminiapi.PreferenceManager
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefManager = remember { PreferenceManager(context) }
    val healthDataManager = remember { HealthDataManager(context) }
    val profile by HealthFeatureStore.profile.collectAsState()
    val scrollState = rememberScrollState()
    var step by remember { mutableIntStateOf(1) }

    // Permission Launchers
    val healthLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ -> }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            IconChip(
                icon = if (step < 3) AppIcons.HealthShield else AppIcons.Lock,
                size = 80.dp,
                iconSize = 40.dp,
                background = Lime.copy(alpha = 0.1f),
                tint = Lime
            )

            Text(
                text = when(step) {
                    1 -> "Welcome to HealthAI"
                    2 -> "Lifestyle & Vitals"
                    else -> "App Permissions"
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Charcoal
            )

            Text(
                text = when(step) {
                    1 -> "Let's set up your basic profile to personalize your experience."
                    2 -> "Your lifestyle habits and baseline vitals for your first AI analysis."
                    else -> "Grant access to health and hardware features to enable all tracking."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Grey600,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (step == 1) {
                AssessmentSection("BASIC INFORMATION") {
                    AssessmentSlider("Age", profile.age, 1f..120f) { HealthFeatureStore.update { p -> p.copy(age = it) } }
                    AssessmentDropdown("Gender", profile.gender, listOf("male", "female")) { HealthFeatureStore.update { p -> p.copy(gender = it) } }
                    AssessmentSlider("Height (cm)", profile.height, 50f..250f) { HealthFeatureStore.update { p -> p.copy(height = it) } }
                    AssessmentSlider("Weight (kg)", profile.weight, 20f..300f) { HealthFeatureStore.update { p -> p.copy(weight = it) } }
                }
                
                AppButton(
                    text = "NEXT: LIFESTYLE",
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (step == 2) {
                AssessmentSection("LIFESTYLE & VITALS") {
                    AssessmentDropdown("Smoking Status", profile.smoking, listOf("never", "former", "current")) { HealthFeatureStore.update { p -> p.copy(smoking = it) } }
                    AssessmentDropdown("Physical Activity", profile.activity, listOf("low", "moderate", "high", "veryHigh")) { HealthFeatureStore.update { p -> p.copy(activity = it) } }
                    AssessmentSlider("Systolic BP", profile.systolic, 60f..250f) { HealthFeatureStore.update { p -> p.copy(systolic = it) } }
                    AssessmentSlider("Glucose (mg/dL)", profile.glucose, 30f..500f) { HealthFeatureStore.update { p -> p.copy(glucose = it) } }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton(
                        text = "BACK",
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f),
                        style = AppButtonStyle.Outline
                    )
                    AppButton(
                        text = "NEXT: PERMISSIONS",
                        onClick = { step = 3 },
                        modifier = Modifier.weight(2f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PermissionCard(
                        title = "Health Connect",
                        desc = "Sync steps, sleep, and heart rate data.",
                        icon = AppIcons.Sync,
                        onClick = { healthLauncher.launch(healthDataManager.permissions) }
                    )
                    
                    PermissionCard(
                        title = "Camera & Location",
                        desc = "Required for Exercise Assist and Run tracking.",
                        icon = AppIcons.Camera,
                        onClick = {
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        }
                    )

                    PermissionCard(
                        title = "All Files Access",
                        desc = "Needed to load the 2.5GB local AI model.",
                        icon = AppIcons.Storage,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppButton(
                        text = "BACK",
                        onClick = { step = 2 },
                        modifier = Modifier.weight(1f),
                        style = AppButtonStyle.Outline
                    )
                    AppButton(
                        text = "FINISH & START",
                        onClick = {
                            prefManager.saveFullProfile(profile)
                            onComplete()
                        },
                        modifier = Modifier.weight(2f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PermissionCard(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = icon, background = Lime, size = 44.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = mutedContent())
            }
            Text("GRANT", color = Lime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}
