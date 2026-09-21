package com.example.geminiapi.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapi.R
import com.example.geminiapi.ui.components.*
import com.example.geminiapi.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToAI: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // GREETING
        item {
            ScreenHeader(
                title = "",
                titleImageRes = R.drawable.header_home,
                subtitle = "Here's how you're doing today.",
                trailing = { Avatar() }
            )
        }

        // WEEK STRIP
        item {
            WeekStrip(
                days = uiState.days,
                selectedIndex = uiState.selectedDayIndex,
                onDaySelected = { viewModel.selectDay(it) }
            )
        }

        // TODAY (with Crossfade for data changes)
        item {
            ScreenSection(title = "Daily Summary") {
                Crossfade(targetState = uiState, label = "statsAnimation") { state ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TodayOverviewCard(state)
                        HealthSummaryGrid(state)
                    }
                }
            }
        }

        // TODAY'S ACTIVITY
        item {
            ScreenSection(title = "Today's activity") {
                ActivityCard()
            }
        }

        // YOUR HEALTH
        item {
            ScreenSection(title = "Your health") {
                HealthStatusCard(onNavigateToHealth)
            }
        }

        // AI INSIGHT
        item {
            ScreenSection(title = "AI insight") {
                AIInsightCard(onNavigateToAI)
            }
        }

        // START WORKOUT
        item {
            AppButton(
                text = "Start Workout",
                onClick = onNavigateToWorkout,
                modifier = Modifier.fillMaxWidth(),
                icon = AppIcons.Play
            )
        }
    }
}

@Composable
private fun WeekStrip(
    days: List<DayInfo>,
    selectedIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    val monthLabel = remember { currentMonthLabel() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = monthLabel, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEachIndexed { index, day ->
                DayPill(
                    day = day,
                    selected = index == selectedIndex,
                    modifier = Modifier.weight(1f),
                    onClick = { onDaySelected(index) }
                )
            }
        }
    }
}

@Composable
private fun DayPill(
    day: DayInfo,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.inverseSurface else Lime
    val foreground = if (selected) MaterialTheme.colorScheme.inverseOnSurface else Charcoal

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.letter,
            style = MaterialTheme.typography.labelMedium,
            color = foreground.copy(alpha = 0.7f)
        )
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = foreground
        )
    }
}

@Composable
fun TodayOverviewCard(uiState: HomeUiState) {
    val stepGoal = 10000f
    val progress = (uiState.steps.toFloat() / stepGoal).coerceIn(0f, 1f)
    
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress,
                diameter = 116.dp,
                strokeWidth = 12.dp,
                color = Lime,
                trackColor = Color.White.copy(alpha = 0.12f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "step goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedContent()
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OverviewMetric(icon = AppIcons.Walk, value = String.format(Locale.US, "%,d", uiState.steps), label = "Steps")
                OverviewMetric(icon = AppIcons.Sleep, value = "${uiState.sleepHours}h ${uiState.sleepMinutes}m", label = "Sleep")
                OverviewMetric(icon = AppIcons.HeartRate, value = uiState.hr.toString(), label = "Resting HR")
            }
        }
    }
}

@Composable
private fun OverviewMetric(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconChip(
            icon = icon,
            background = Color.White.copy(alpha = 0.1f),
            tint = Lime,
            size = 36.dp,
            iconSize = 18.dp,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = mutedContent())
        }
    }
}

private val HeartWave = listOf(0.25f, 0.5f, 0.9f, 0.4f, 0.7f, 0.3f, 1f, 0.5f, 0.35f, 0.8f, 0.45f, 0.6f, 0.3f, 0.75f, 0.4f, 0.55f, 0.25f, 0.65f)
private val SleepBars = listOf(0.55f, 0.8f, 0.65f, 0.9f, 0.7f, 0.85f, 0.75f)

@Composable
private fun HealthSummaryGrid(uiState: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeartRateTile(uiState.hr, modifier = Modifier.weight(1f))
            StepsTile(uiState.steps, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SleepTile(uiState.sleepHours, uiState.sleepMinutes, modifier = Modifier.weight(1f))
            ExerciseTile(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeartRateTile(hr: Long, modifier: Modifier = Modifier) {
    MetricTile(title = "Heart rate", icon = AppIcons.HeartRate, accent = AccentRed, modifier = modifier) {
        WaveformChart(values = HeartWave, modifier = Modifier.fillMaxWidth().height(40.dp), color = AccentRed)
        Spacer(modifier = Modifier.weight(1f))
        ValueLine(value = hr.toString(), unit = "bpm")
        Text(text = "Resting", style = MaterialTheme.typography.bodySmall, color = mutedContent())
    }
}

@Composable
private fun StepsTile(steps: Long, modifier: Modifier = Modifier) {
    val stepGoal = 10000f
    val progress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val remaining = (stepGoal - steps).toInt().coerceAtLeast(0)
    
    MetricTile(title = "Steps", icon = AppIcons.Walk, accent = AccentOrange, modifier = modifier) {
        Spacer(modifier = Modifier.weight(1f))
        ValueLine(value = String.format(Locale.US, "%,d", steps), unit = "steps")
        Spacer(modifier = Modifier.height(6.dp))
        ProgressBar(progress = progress, color = AccentOrange, trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = String.format(Locale.US, "%,d to goal", remaining), style = MaterialTheme.typography.bodySmall, color = mutedContent())
    }
}

@Composable
private fun SleepTile(hours: Long, minutes: Long, modifier: Modifier = Modifier) {
    val barColors = SleepBars.mapIndexed { index, _ -> if (index == SleepBars.lastIndex) AccentPurple else AccentPurple.copy(alpha = 0.45f) }
    MetricTile(title = "Sleep", icon = AppIcons.Sleep, accent = AccentPurple, modifier = modifier) {
        BarChart(values = SleepBars, barColors = barColors, barWidth = 9.dp, modifier = Modifier.fillMaxWidth().height(40.dp))
        Spacer(modifier = Modifier.weight(1f))
        ValueLine(value = "${hours}h ${minutes}m", unit = "")
        Text(text = "Last night", style = MaterialTheme.typography.bodySmall, color = mutedContent())
    }
}

@Composable
private fun ExerciseTile(modifier: Modifier = Modifier) {
    MetricTile(title = "Exercise", icon = AppIcons.Timer, accent = AccentBlue, modifier = modifier) {
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = 0.64f, diameter = 56.dp, strokeWidth = 7.dp, color = AccentBlue, trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
                Text(text = "64%", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "29", style = MaterialTheme.typography.headlineSmall)
                Text(text = "of 45 min", style = MaterialTheme.typography.bodySmall, color = mutedContent())
            }
        }
    }
}

@Composable
fun ActivityCard() {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.Run, size = 52.dp, iconSize = 26.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Morning Run", style = MaterialTheme.typography.titleMedium)
                Text(text = "4.8 km • 29 min", style = MaterialTheme.typography.bodyMedium, color = mutedContent())
            }
            RouteSketch(modifier = Modifier.size(width = 84.dp, height = 56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.inverseSurface))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Active minutes goal", style = MaterialTheme.typography.bodySmall, color = mutedContent())
            Text(text = "29 / 45 min", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProgressBar(progress = 0.64f, color = Lime)
        Spacer(modifier = Modifier.height(18.dp))
        AppButton(text = "View Workout", onClick = {}, modifier = Modifier.fillMaxWidth(), style = AppButtonStyle.Outline)
    }
}

@Composable
fun HealthStatusCard(onNavigate: () -> Unit) {
    AppCard(containerColor = tintOver(AccentPurple)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconChip(icon = AppIcons.HealthShield, background = AccentPurple, size = 48.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Health assessment", style = MaterialTheme.typography.titleMedium)
                Text(text = "Your latest assessment was updated recently.", style = MaterialTheme.typography.bodyMedium, color = mutedContent())
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(AccentRed, AccentOrange, AccentBlue, AccentPurple).forEach { segment ->
                Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(segment))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            InfoChip(text = "4 areas assessed", icon = AppIcons.Check, background = MaterialTheme.colorScheme.surface)
            Text(text = "View report →", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { onNavigate() })
        }
    }
}

@Composable
fun AIInsightCard(onNavigate: () -> Unit) {
    LimeCard {
        IconChip(icon = AppIcons.Sparkle, background = Charcoal, tint = Lime, size = 44.dp, shape = CircleShape)
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Your activity has been consistent this week.", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "A short recovery walk could help you stay active today.", style = MaterialTheme.typography.bodyMedium, color = Charcoal.copy(alpha = 0.75f) )
        Spacer(modifier = Modifier.height(16.dp))
        AppButton(text = "Ask Health AI", onClick = onNavigate, style = AppButtonStyle.Dark, icon = AppIcons.Sparkle)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    GeminiapiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen()
        }
    }
}
