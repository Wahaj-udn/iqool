package com.example.geminiapi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*

@Composable
fun WorkoutScreen(
    onNavigateToRun: () -> Unit = {},
    onNavigateToExercise: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // HEADER
        item {
            ScreenHeader(
                title = "Workout",
                subtitle = "Move, train and improve at your own pace."
            )
        }

        // START A WORKOUT (Run + Live Exercise Coach)
        item {
            ScreenSection(title = "Start a workout") {
                RunCard(onNavigateToRun)
                ExerciseCoachCard(onNavigateToExercise)
            }
        }

        // WEEKLY SUMMARY CHART
        item {
            ScreenSection(title = "This week") {
                WeeklySummaryCard()
            }
        }

        // RECENT WORKOUTS
        item {
            ScreenSection(title = "Recent workouts") {
                WorkoutHistoryCard(
                    icon = AppIcons.Run,
                    accent = Lime,
                    title = "Morning Run",
                    details = "4.8 km • 29 min",
                    date = "20 Sep"
                )
                WorkoutHistoryCard(
                    icon = AppIcons.Strength,
                    accent = AccentPurple,
                    title = "Strength Training",
                    details = "32 min • 6 exercises",
                    date = "18 Sep"
                )
                WorkoutHistoryCard(
                    icon = AppIcons.Run,
                    accent = Lime,
                    title = "Evening Run",
                    details = "3.2 km • 21 min",
                    date = "16 Sep"
                )
                AppButton(
                    text = "View All Workouts",
                    onClick = {
                        // Workout history screen will be added later
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AppButtonStyle.Outline
                )
            }
        }
    }
}

@Composable
fun RunCard(onStart: () -> Unit) {
    HeroCard {
        RouteSketch(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            routeColor = Lime,
            gridColor = Color.White.copy(alpha = 0.08f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.Run, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Run", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Track your route, pace and performance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedContent()
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chipColor = Color.White.copy(alpha = 0.1f)
            val chipContent = LocalContentColor.current
            InfoChip("Route", icon = AppIcons.Location, background = chipColor, contentColor = chipContent)
            InfoChip("Pace", icon = AppIcons.Speed, background = chipColor, contentColor = chipContent)
            InfoChip("Stats", icon = AppIcons.Insights, background = chipColor, contentColor = chipContent)
        }
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(
            text = "Start Run",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.Lime,
            icon = AppIcons.Play
        )
    }
}

@Composable
fun ExerciseCoachCard(onStart: () -> Unit) {
    LimeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(
                icon = AppIcons.Camera,
                background = Charcoal,
                tint = Lime,
                size = 52.dp,
                iconSize = 26.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Live Exercise Coach",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Use your camera for real-time exercise guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal.copy(alpha = 0.75f)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(
            text = "Start Exercise",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.Dark
        )
    }
}

@Composable
private fun WeeklySummaryCard() {
    val days = remember { lastDays(7) }
    val minutes = listOf(0f, 0f, 21f, 0f, 32f, 0f, 29f)
    val restDayColor = MaterialTheme.colorScheme.outlineVariant
    val workoutColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val barColors = minutes.mapIndexed { index, value ->
        when {
            value == 0f -> restDayColor
            index == minutes.lastIndex -> Lime
            else -> workoutColor
        }
    }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "82 min", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Active this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedContent()
                )
            }
            InfoChip(text = "3 workouts", icon = AppIcons.Strength)
        }
        Spacer(modifier = Modifier.height(20.dp))
        BarChart(
            values = minutes,
            barColors = barColors,
            labels = days.map { it.letter },
            barWidth = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Composable
fun WorkoutHistoryCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    details: String,
    date: String
) {
    AppCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = icon, background = accent, size = 48.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedContent()
                )
            }
            InfoChip(text = date)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WorkoutScreenPreview() {
    GeminiapiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WorkoutScreen()
        }
    }
}
