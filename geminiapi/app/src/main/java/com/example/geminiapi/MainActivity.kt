package com.example.geminiapi

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geminiapi.ui.theme.GeminiapiTheme
import org.maplibre.android.MapLibre
import com.example.geminiapi.llama.LocalLlamaScreen
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.screens.*
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefManager = PreferenceManager(this)
        
        // Initialize store from persistent preferences
        HealthFeatureStore.init(prefManager.getProfile())
        
        val startDest = if (prefManager.isOnboardingCompleted) "home" else "onboarding"
        
        Log.d("MainActivity", "onCreate started. Onboarding: ${prefManager.isOnboardingCompleted}")
        
        val isEmulator = Build.PRODUCT.contains("sdk") || 
                         Build.MODEL.contains("Emulator")
        
        if (!isEmulator) {
            try {
                MapLibre.getInstance(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "MapLibre init failed: ${e.message}")
            }
        }

        setContent {
            GeminiapiTheme {
                val navController = rememberNavController()
                
                // Track the bottom bar selection
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                
                // Synchronize selectedTab with NavController destination
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                LaunchedEffect(currentRoute) {
                    selectedTab = when(currentRoute) {
                        "home" -> 0
                        "workout" -> 1
                        "health_dashboard" -> 2
                        "profile" -> 3
                        else -> selectedTab
                    }
                }
                
                val navItems = listOf(
                    NavItem("Home", AppIcons.Home),
                    NavItem("Workout", AppIcons.Run),
                    NavItem("Health", AppIcons.Heart),
                    NavItem("Profile", AppIcons.Person)
                )

                Scaffold(
                    bottomBar = {
                        // Only show bottom bar on top-level screens
                        if (currentRoute in listOf("home", "workout", "health_dashboard", "profile")) {
                            FloatingNavBar(
                                items = navItems,
                                selectedIndex = selectedTab,
                                onItemSelected = { index ->
                                    val route = when(index) {
                                        0 -> "home"
                                        1 -> "workout"
                                        2 -> "health_dashboard"
                                        else -> "profile"
                                    }
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("onboarding") {
                                OnboardingScreen(onComplete = {
                                    prefManager.isOnboardingCompleted = true
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                })
                            }
                            composable("home") {
                                HomeScreen(
                                    onNavigateToWorkout = { navController.navigate("workout") },
                                    onNavigateToHealth = { navController.navigate("health_dashboard") },
                                    onNavigateToAI = { navController.navigate("ai_coach") },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            composable("workout") {
                                WorkoutScreen(
                                    onNavigateToRun = { navController.navigate("run") },
                                    onNavigateToExercise = { navController.navigate("exercise") },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            composable("health_dashboard") {
                                HealthScreen(
                                    onNavigateToAssessment = { navController.navigate("assessment") },
                                    onNavigateToAI = { navController.navigate("ai_coach") },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    onNavigateToHealthConnect = { navController.navigate("health") },
                                    onNavigateToOldChat = { navController.navigate("old_ai_chat") },
                                    onNavigateToEdit = { navController.navigate("assessment") },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            
                            // New AI Coach Chat
                            composable("ai_coach") {
                                AICoachScreen(onBack = { navController.popBackStack() })
                            }
                            
                            // Existing detail screens
                            composable("old_ai_chat") {
                                BakingScreen(
                                    onNavigateToHealth = { navController.navigate("health") },
                                    onNavigateToRun = { navController.navigate("run") },
                                    onNavigateToExercise = { navController.navigate("exercise") },
                                    onNavigateToAssessment = { navController.navigate("assessment") },
                                    onNavigateToLocalChat = { navController.navigate("local_chat") }
                                )
                            }
                            composable("health") {
                                HealthConnectScreen(onBack = { navController.popBackStack() })
                            }
                            composable("run") {
                                RunScreen(onBack = { navController.popBackStack() })
                            }
                            composable("exercise") {
                                ExerciseAssistScreen(onBack = { navController.popBackStack() })
                            }
                            composable("diabetes") {
                                DiabetesScreen(onBack = { navController.popBackStack() })
                            }
                            composable("obesity") {
                                ObesityScreen(onBack = { navController.popBackStack() })
                            }
                            composable("heart") {
                                HeartScreen(onBack = { navController.popBackStack() })
                            }
                            composable("hypertension") {
                                HypertensionScreen(onBack = { navController.popBackStack() })
                            }
                            composable("assessment") {
                                HealthAssessmentScreen(
                                    onBack = { navController.popBackStack() },
                                    onNavigateToChat = { navController.navigate("ai_coach") }
                                )
                            }
                            composable("local_chat") {
                                LocalLlamaScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}
