package com.example.geminiapi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geminiapi.HealthFeatureStore
import com.example.geminiapi.ui.components.AppButtonStyle
import com.example.geminiapi.ui.components.AppButton
import com.example.geminiapi.ui.components.AppIcons
import com.example.geminiapi.ui.components.IconChip
import com.example.geminiapi.ui.theme.Charcoal
import com.example.geminiapi.ui.theme.Lime

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconChip(
                    icon = AppIcons.Sparkle,
                    size = 100.dp,
                    iconSize = 50.dp,
                    background = Lime,
                    tint = Charcoal,
                    shape = CircleShape
                )

                Text(
                    text = "Welcome to\nHealthAI",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lime,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )

                Text(
                    text = "Your journey to a healthier life starts with a conversation.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What's your name?", color = Lime) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Lime,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Lime,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                AppButton(
                    text = "START JOURNEY",
                    onClick = {
                        HealthFeatureStore.update { it.copy(name = name) }
                        onContinue()
                    },
                    enabled = name.isNotBlank(),
                    style = AppButtonStyle.Lime,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
