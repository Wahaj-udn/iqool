package com.example.geminiapi.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapi.BakingViewModel
import com.example.geminiapi.UiState
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.Charcoal
import com.example.geminiapi.ui.theme.Lime
import com.google.ai.client.generativeai.type.TextPart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachScreen(
    onBack: () -> Unit,
    viewModel: BakingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    var userMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show Gemini errors as disappearing popups
    LaunchedEffect(uiState) {
        if (uiState is UiState.Error) {
            snackbarHostState.showSnackbar(
                message = "Gemini Error: ${(uiState as UiState.Error).errorMessage}",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconChip(icon = AppIcons.Sparkle, size = 32.dp, iconSize = 16.dp, background = Lime)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Health Coach", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userMessage,
                        onValueChange = { userMessage = it },
                        placeholder = { Text("Ask your health coach...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Lime,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    FloatingActionButton(
                        onClick = {
                            if (userMessage.isNotBlank()) {
                                viewModel.sendPrompt(null, userMessage)
                                userMessage = ""
                            }
                        },
                        containerColor = Lime,
                        contentColor = Charcoal,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Charcoal, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (chatHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconChip(icon = AppIcons.Insights, size = 64.dp, iconSize = 32.dp, background = Lime.copy(alpha = 0.1f), tint = Lime)
                        Text(
                            "Your Personal AI Coach",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Discuss your results and get a custom plan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedContent()
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = false // Normal chat order
                ) {
                    items(chatHistory) { content ->
                        val isUser = content.role == "user"
                        val text = content.parts.filterIsInstance<TextPart>().firstOrNull()?.text ?: ""
                        
                        CoachChatBubble(text = text, isUser = isUser)
                    }
                }
            }
        }
    }
}

@Composable
fun CoachChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        AppCard(
            modifier = Modifier.widthIn(max = 300.dp),
            containerColor = if (isUser) Lime else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) Charcoal else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            contentPadding = PaddingValues(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )
        }
    }
}
