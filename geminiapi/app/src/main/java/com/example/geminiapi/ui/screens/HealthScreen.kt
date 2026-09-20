package com.example.geminiapi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geminiapi.AssessmentResults
import com.example.geminiapi.HealthFeatureStore
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HealthScreen(
    onNavigateToAssessment: () -> Unit = {},
    onNavigateToAI: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lastResults by HealthFeatureStore.lastResults.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ScreenHeader(
                title = "Health",
                subtitle = "Understand your health"
            )
        }
        item { HealthReportCard(lastResults, onNavigateToAssessment) }
        item { RestingHeartRateCard() }
        item { HealthProfileCard(onNavigateToAssessment) }
        item { HealthAICard(onNavigateToAI) }
        item {
            ScreenSection(title = "Assessment areas") {
                AssessmentAreasGrid(lastResults)
            }
        }
    }
}

@Composable
fun HealthReportCard(results: AssessmentResults?, onNavigate: () -> Unit) {
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val areasCount = if (results != null) "4" else "0"
            ProgressRing(
                progress = if (results != null) 1f else 0f,
                diameter = 88.dp,
                strokeWidth = 10.dp,
                color = Lime,
                trackColor = Color.White.copy(alpha = 0.12f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = areasCount, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "areas",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedContent()
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your health report",
                    style = MaterialTheme.typography.labelLarge,
                    color = Lime
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your latest health assessment",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (results != null) "4 health areas assessed" else "No assessment yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AppButton(
            text = if (results != null) "View Health Report" else "Start Assessment",
            onClick = onNavigate,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.Lime
        )
    }
}

@Composable
private fun RestingHeartRateCard() {
    val days = remember { lastDays(7) }
    val trend = listOf(71f, 70f, 72f, 69f, 70f, 68f, 68f)

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.HeartRate, background = AccentRed, size = 40.dp, iconSize = 20.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Resting heart rate", style = MaterialTheme.typography.titleMedium)
                Text(text = "Last 7 days", style = MaterialTheme.typography.bodySmall, color = mutedContent())
            }
            ValueLine(value = "68", unit = "bpm")
        }
        Spacer(modifier = Modifier.height(12.dp))
        LineChart(values = trend, lineColor = AccentRed, modifier = Modifier.fillMaxWidth().height(110.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                Text(text = day.letter, style = MaterialTheme.typography.labelSmall, color = mutedContent(0.55f))
            }
        }
    }
}

@Composable
fun HealthProfileCard(onNavigate: () -> Unit) {
    AppCard {
        Text(text = "Health profile", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Data completeness", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "82% of health data available", style = MaterialTheme.typography.bodyMedium, color = mutedContent())
            }
            Spacer(modifier = Modifier.width(12.dp))
            ProgressRing(progress = 0.82f, diameter = 72.dp, strokeWidth = 9.dp, color = AccentPurple, trackColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(text = "82%", style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryProgress(label = "Vitals", progress = 0.8f, color = AccentRed)
            CategoryProgress(label = "Lifestyle", progress = 1f, color = AccentPurple)
            CategoryProgress(label = "Nutrition", progress = 0.7f, color = AccentOrange)
        }
        Spacer(modifier = Modifier.height(20.dp))
        AppButton(text = "Update Assessment", onClick = onNavigate, modifier = Modifier.fillMaxWidth(), style = AppButtonStyle.Outline)
    }
}

@Composable
private fun CategoryProgress(label: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = mutedContent())
        }
        Spacer(modifier = Modifier.height(6.dp))
        ProgressBar(progress = progress, color = color, height = 8.dp)
    }
}

@Composable
fun HealthAICard(onNavigate: () -> Unit) {
    LimeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.Sparkle, background = Charcoal, tint = Lime, size = 44.dp, shape = CircleShape)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Health AI", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Your personal health assistant", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Ask about your health data or understand your risk assessments.", style = MaterialTheme.typography.bodyMedium, color = Charcoal.copy(alpha = 0.75f))
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(text = "Ask Health AI", onClick = onNavigate, modifier = Modifier.fillMaxWidth(), style = AppButtonStyle.Dark)
    }
}

@Composable
private fun AssessmentAreasGrid(results: AssessmentResults?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssessmentTile(
                title = "Diabetes",
                status = results?.diabetes?.label ?: "Not Assessed",
                icon = AppIcons.Blood,
                accent = AccentOrange,
                isAssessed = results?.diabetes != null,
                modifier = Modifier.weight(1f)
            )
            AssessmentTile(
                title = "Heart Health",
                status = results?.heart?.label ?: "Not Assessed",
                icon = AppIcons.Heart,
                accent = AccentRed,
                isAssessed = results?.heart != null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssessmentTile(
                title = "Blood Pressure",
                status = results?.hypertension?.label ?: "Not Assessed",
                icon = AppIcons.HeartRate,
                accent = AccentBlue,
                isAssessed = results?.hypertension != null,
                modifier = Modifier.weight(1f)
            )
            AssessmentTile(
                title = "Weight & Nutrition",
                status = results?.obesity?.label ?: "Not Assessed",
                icon = AppIcons.Weight,
                accent = AccentPurple,
                isAssessed = results?.obesity != null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AssessmentTile(
    title: String,
    status: String,
    icon: ImageVector,
    accent: Color,
    isAssessed: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(24.dp),
        color = tintOver(accent),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IconChip(icon = icon, background = accent, size = 40.dp, iconSize = 20.dp)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAssessed) AppIcons.Check else AppIcons.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = status, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HealthScreenPreview() {
    GeminiapiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HealthScreen()
        }
    }
}
