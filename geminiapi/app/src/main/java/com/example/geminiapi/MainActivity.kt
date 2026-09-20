package com.example.geminiapi

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geminiapi.ui.theme.GeminiapiTheme
import org.maplibre.android.MapLibre
import com.example.geminiapi.llama.LocalLlamaScreen
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate started")
        
        // Disable MapLibre on emulators if causing black screen
        val isEmulator = Build.PRODUCT.contains("sdk") ||
                         Build.MODEL.contains("Emulator")
        
        if (!isEmulator) {
            try {
                MapLibre.getInstance(this)
                Log.d("MainActivity", "MapLibre initialized")
            } catch (e: Exception) {
                Log.e("MainActivity", "MapLibre init failed: ${e.message}")
            }
        } else {
            Log.d("MainActivity", "Skipping MapLibre on emulator to avoid rendering bugs")
        }

        setContent {
            GeminiapiTheme {
                val navController = rememberNavController()
                Scaffold { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        NavHost(navController = navController, startDestination = "chat") {
                            composable("chat") {
                                BakingScreen(
                                    onNavigateToHealth = {
                                        navController.navigate("health")
                                    },
                                    onNavigateToRun = {
                                        navController.navigate("run")
                                    },
                                    onNavigateToExercise = {
                                        navController.navigate("exercise")
                                    },
                                    onNavigateToAssessment = {
                                        navController.navigate("assessment")
                                    },
                                    onNavigateToLocalChat = {
                                        navController.navigate("local_chat")
                                    }
                                )
                            }
                            composable("health") {
                                HealthConnectScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("run") {
                                RunScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("exercise") {
                                ExerciseAssistScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("diabetes") {
                                DiabetesScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("obesity") {
                                ObesityScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("heart") {
                                HeartScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("hypertension") {
                                HypertensionScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                            composable("assessment") {
                                HealthAssessmentScreen(
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToChat = {
                                        navController.navigate("chat")
                                    }
                                )
                            }
                            composable("local_chat") {
                                LocalLlamaScreen(onBack = {
                                    navController.popBackStack()
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
