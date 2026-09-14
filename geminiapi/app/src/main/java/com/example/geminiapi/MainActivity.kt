package com.example.geminiapi

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MapLibre globally
        try {
            MapLibre.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
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
                        }
                    }
                }
            }
        }
    }
}
