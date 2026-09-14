package com.example.geminiapi

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun HealthConnectScreen(
    viewModel: HealthConnectViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isModeSelected by rememberSaveable { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        // Always re-check permissions to update the UI state
        viewModel.checkPermissions(context)
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation button back to chat at the very top
        Button(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Back to Chat")
        }

        Text(
            text = "Health Connect Data",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val state = uiState) {
            is HealthUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HealthUiState.Success -> {
                if (!isModeSelected) {
                    ModeSelectionView(
                        onModeSelected = { mode ->
                            viewModel.updateMode(mode, context)
                            isModeSelected = true
                        }
                    )
                } else {
                    // Specific Mode View with a Back button to the selection screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isModeSelected = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to selection")
                        }
                        Text("Back to Selection", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (state.selectedMode) {
                        HealthViewMode.SUMMARY -> SummaryView(state.summaryData)
                        HealthViewMode.DAY -> DayView(
                            state.summaryData,
                            state.selectedDate,
                            onDateChange = { viewModel.updateDate(it, context) }
                        )
                        HealthViewMode.WEEK -> WeekMonthView(
                            state.data,
                            state.summaryData,
                            state.selectedMode,
                            state.selectedDate,
                            onDateChange = { viewModel.updateDate(it, context) }
                        )
                        HealthViewMode.MONTH -> WeekMonthView(
                            state.data,
                            state.summaryData,
                            state.selectedMode,
                            state.selectedDate,
                            onDateChange = { viewModel.updateDate(it, context) }
                        )
                    }
                }
            }
            is HealthUiState.Error -> {
                Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            is HealthUiState.PermissionsRequired -> {
                Column {
                    Text(
                        text = "Permissions are required to access health data.",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = {
                        permissionsLauncher.launch(viewModel.permissions)
                    }) {
                        Text("Grant Permissions")
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ModeSelectionView(onModeSelected: (HealthViewMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "How do you want to view your health data?",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Grid-like layout for mode buttons
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton("Summary", onClick = { onModeSelected(HealthViewMode.SUMMARY) })
                ModeButton("Day", onClick = { onModeSelected(HealthViewMode.DAY) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton("Week", onClick = { onModeSelected(HealthViewMode.WEEK) })
                ModeButton("Month", onClick = { onModeSelected(HealthViewMode.MONTH) })
            }
        }
    }
}

@Composable
fun RowScope.ModeButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(60.dp)
    ) {
        Text(text)
    }
}

@Composable
fun SummaryView(data: HealthData) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("30-Day Aggregate Summary", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        HealthDataGrid(data)
    }
}

@Composable
fun DayView(data: HealthData, selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val isNextEnabled = selectedDate.isBefore(LocalDate.now())
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        DateSelector(
            currentDate = selectedDate,
            formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("E, MMM d, yyyy")),
            isNextEnabled = isNextEnabled,
            onPrevious = { onDateChange(selectedDate.minusDays(1)) },
            onNext = { onDateChange(selectedDate.plusDays(1)) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        HealthDataGrid(data)
    }
}

@Composable
fun WeekMonthView(
    dailyData: List<Pair<Instant, HealthData>>,
    summaryData: HealthData,
    mode: HealthViewMode,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val rangeText = if (mode == HealthViewMode.WEEK) {
        val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
    } else {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    val isNextEnabled = if (mode == HealthViewMode.WEEK) {
        val startOfSelectedWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val startOfCurrentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        startOfSelectedWeek.isBefore(startOfCurrentWeek)
    } else {
        val startOfSelectedMonth = selectedDate.with(TemporalAdjusters.firstDayOfMonth())
        val startOfCurrentMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        startOfSelectedMonth.isBefore(startOfCurrentMonth)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DateSelector(
            currentDate = selectedDate,
            formattedDate = rangeText,
            isNextEnabled = isNextEnabled,
            onPrevious = {
                if (mode == HealthViewMode.WEEK) onDateChange(selectedDate.minusWeeks(1))
                else onDateChange(selectedDate.minusMonths(1))
            },
            onNext = {
                if (mode == HealthViewMode.WEEK) onDateChange(selectedDate.plusWeeks(1))
                else onDateChange(selectedDate.plusMonths(1))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Section
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Range Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SummaryItem(
                    "Daily Avg Steps",
                    "${summaryData.steps / (if (mode == HealthViewMode.WEEK) 7 else selectedDate.lengthOfMonth())}"
                )
                SummaryItem("Avg Heart Rate", "${String.format("%.1f", summaryData.heartRate)} bpm")
                SummaryItem("Total Distance", "${String.format("%.2f", summaryData.distance / 1000.0)} km")
                SummaryItem("Total Sleep", formatDuration(summaryData.totalSleepDuration))
            }
        }

        // Daily Breakdown Section
        Text(
            "Daily Breakdown",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                    Text("Steps", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Avg HR", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Dist", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Sleep", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            items(dailyData) { (instant, data) ->
                val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(date.format(DateTimeFormatter.ofPattern("MMM d")), modifier = Modifier.weight(1.5f))
                    Text("${data.steps}", modifier = Modifier.weight(1f))
                    Text("${String.format("%.0f", data.heartRate)}", modifier = Modifier.weight(1f))
                    Text("${String.format("%.1f", data.distance / 1000.0)}", modifier = Modifier.weight(1f))
                    Text(formatDurationShort(data.totalSleepDuration), modifier = Modifier.weight(1.2f))
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DateSelector(currentDate: LocalDate, formattedDate: String, isNextEnabled: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
        }
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = onNext, enabled = isNextEnabled) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Next",
                tint = if (isNextEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
fun HealthDataGrid(data: HealthData) {
    Column {
        HealthDataCard("Steps", "${data.steps}")
        HealthDataCard("Avg Heart Rate", "${String.format("%.1f", data.heartRate)} bpm")
        HealthDataCard("Total Distance", "${String.format("%.2f", data.distance / 1000.0)} km")
        HealthDataCard("Total Sleep", formatDuration(data.totalSleepDuration))
        HealthDataCard("Height", data.height?.let { "${String.format("%.1f", it)} cm" } ?: "No data")
        HealthDataCard("Weight", data.weight?.let { "${String.format("%.1f", it)} kg" } ?: "No data")
    }
}

@Composable
fun HealthDataCard(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun formatDuration(duration: Duration?): String {
    if (duration == null || duration.isZero) return "No data"
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return "${hours}h ${minutes}m"
}

private fun formatDurationShort(duration: Duration?): String {
    if (duration == null || duration.isZero) return "-"
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return "${hours}h${minutes}m"
}
