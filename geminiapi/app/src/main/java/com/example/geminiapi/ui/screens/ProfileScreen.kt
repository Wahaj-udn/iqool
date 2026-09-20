package com.example.geminiapi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geminiapi.HealthFeatureStore
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToHealthConnect: () -> Unit = {},
    onNavigateToOldChat: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by HealthFeatureStore.profile.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = "Profile",
                subtitle = "Your personal health information"
            )
        }
        item { PersonalInformationCard(profile.age, profile.gender, onNavigateToEdit) }
        item { HealthDataCard(profile.height, profile.weight, profile.smoking, profile.alcohol, onNavigateToEdit) }
        item { HealthConnectCard(onNavigateToHealthConnect) }
        item { PrivacyCard() }
        item { AppSettingsCard() }
        item {
            // Insignificant access to old chat
            TextButton(
                onClick = onNavigateToOldChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("System Diagnostics", style = MaterialTheme.typography.labelSmall, color = Grey400)
            }
        }
    }
}

@Composable
fun PersonalInformationCard(age: Float, gender: String, onEdit: () -> Unit) {
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(diameter = 64.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Personal information", style = MaterialTheme.typography.labelLarge, color = Lime)
                Text(text = "Health User", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = onEdit) {
                Icon(AppIcons.Edit, contentDescription = "Edit", tint = Lime)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        SoftDivider()
        InfoRow(label = "Age", value = "${age.toInt()} years")
        SoftDivider()
        InfoRow(label = "Gender", value = gender.replaceFirstChar { it.uppercase() })
    }
}

@Composable
fun HealthDataCard(height: Float, weight: Float, smoking: String, alcohol: String, onEdit: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Health data", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(AppIcons.Edit, contentDescription = "Edit", tint = Charcoal)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Information used to personalize your health experience.", style = MaterialTheme.typography.bodyMedium, color = mutedContent())
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "Height", value = "${height.toInt()} cm", icon = AppIcons.Height)
        SoftDivider()
        InfoRow(label = "Weight", value = "${weight.toInt()} kg", icon = AppIcons.Weight)
        SoftDivider()
        InfoRow(label = "Smoking", value = smoking.replaceFirstChar { it.uppercase() }, icon = AppIcons.Smoking)
        SoftDivider()
        InfoRow(label = "Alcohol", value = alcohol.replaceFirstChar { it.uppercase() }, icon = AppIcons.Alcohol)
    }
}

@Composable
fun HealthConnectCard(onManage: () -> Unit) {
    AppCard(containerColor = tintOver(AccentBlue)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.Sync, background = AccentBlue, size = 44.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Health Connect", style = MaterialTheme.typography.titleLarge)
            }
            InfoChip(text = "Connected", icon = AppIcons.Check, background = Lime, contentColor = Charcoal)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Health data from supported apps is used to personalize your experience.", style = MaterialTheme.typography.bodyMedium, color = mutedContent())
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(text = "Manage Health Connect", onClick = onManage, modifier = Modifier.fillMaxWidth(), style = AppButtonStyle.Outline)
    }
}

@Composable
fun PrivacyCard() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.Lock, background = AccentPurple, size = 44.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Privacy", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Your health data stays under your control.", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "The app is designed with local-first processing so that personal health information can be processed on your device.", style = MaterialTheme.typography.bodyMedium, color = mutedContent())
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Local processing", style = MaterialTheme.typography.bodyLarge)
            InfoChip(text = "Enabled", icon = AppIcons.Check, background = Lime, contentColor = Charcoal)
        }
    }
}

@Composable
fun AppSettingsCard() {
    AppCard {
        Text(text = "App settings", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "Notifications", value = "Enabled", icon = AppIcons.Notifications, showChevron = true)
        SoftDivider()
        InfoRow(label = "Units", value = "Metric", icon = AppIcons.Height, showChevron = true)
        SoftDivider()
        InfoRow(label = "Data storage", value = "On device", icon = AppIcons.Storage, showChevron = true)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    GeminiapiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileScreen()
        }
    }
}
