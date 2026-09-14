package com.example.geminiapi

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class HealthViewMode { SUMMARY, DAY, WEEK, MONTH }

data class HealthData(
    val steps: Long = 0,
    val heartRate: Double = 0.0,
    val distance: Double = 0.0,
    val totalSleepDuration: Duration? = null,
    val height: Double? = null,
    val weight: Double? = null
)

sealed class HealthUiState {
    object Uninitialized : HealthUiState()
    object Loading : HealthUiState()
    data class Success(
        val data: List<Pair<Instant, HealthData>>,
        val summaryData: HealthData,
        val selectedMode: HealthViewMode,
        val selectedDate: LocalDate
    ) : HealthUiState()
    data class Error(val message: String) : HealthUiState()
    object PermissionsRequired : HealthUiState()
}

class HealthConnectViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Uninitialized)
    val uiState: StateFlow<HealthUiState> = _uiState

    private val _selectedMode = MutableStateFlow(HealthViewMode.SUMMARY)
    val selectedMode: StateFlow<HealthViewMode> = _selectedMode

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    fun checkPermissions(context: Context) {
        val healthConnectClient = getClient(context) ?: return
        viewModelScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.isNotEmpty()) {
                fetchData(healthConnectClient)
            } else {
                _uiState.value = HealthUiState.PermissionsRequired
            }
        }
    }

    fun updateMode(mode: HealthViewMode, context: Context) {
        _selectedMode.value = mode
        _selectedDate.value = LocalDate.now()
        val client = getClient(context) ?: return
        fetchData(client)
    }

    fun updateDate(date: LocalDate, context: Context) {
        _selectedDate.value = date
        val client = getClient(context) ?: return
        fetchData(client)
    }

    private fun getClient(context: Context): HealthConnectClient? {
        return if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            _uiState.value = HealthUiState.Error("Health Connect is not available")
            null
        }
    }

    fun fetchData(healthConnectClient: HealthConnectClient) {
        val mode = _selectedMode.value
        val date = _selectedDate.value
        val zoneId = ZoneId.systemDefault()

        _uiState.value = HealthUiState.Loading
        viewModelScope.launch {
            try {
                val start: Instant
                val end: Instant

                when (mode) {
                    HealthViewMode.SUMMARY -> {
                        start = Instant.now().minus(30, ChronoUnit.DAYS)
                        end = Instant.now()
                    }
                    HealthViewMode.DAY -> {
                        start = date.atStartOfDay(zoneId).toInstant()
                        end = date.plusDays(1).atStartOfDay(zoneId).toInstant()
                    }
                    HealthViewMode.WEEK -> {
                        val firstDayOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        start = firstDayOfWeek.atStartOfDay(zoneId).toInstant()
                        end = firstDayOfWeek.plusDays(7).atStartOfDay(zoneId).toInstant()
                    }
                    HealthViewMode.MONTH -> {
                        val firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
                        start = firstDayOfMonth.atStartOfDay(zoneId).toInstant()
                        end = firstDayOfMonth.plusMonths(1).atStartOfDay(zoneId).toInstant()
                    }
                }

                val timeRangeFilter = TimeRangeFilter.between(start, end)
                val granted = healthConnectClient.permissionController.getGrantedPermissions()

                val metrics = mutableSetOf<AggregateMetric<*>>()
                if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) metrics.add(StepsRecord.COUNT_TOTAL)
                if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) metrics.add(HeartRateRecord.BPM_AVG)
                if (granted.contains(HealthPermission.getReadPermission(DistanceRecord::class))) metrics.add(DistanceRecord.DISTANCE_TOTAL)
                if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) metrics.add(SleepSessionRecord.SLEEP_DURATION_TOTAL)

                val dataList = mutableListOf<Pair<Instant, HealthData>>()

                val aggregateResponse = if (metrics.isNotEmpty()) {
                    healthConnectClient.aggregate(
                        AggregateRequest(
                            metrics = metrics,
                            timeRangeFilter = timeRangeFilter
                        )
                    )
                } else null
                val summaryData = extractHealthData(aggregateResponse, healthConnectClient, timeRangeFilter, granted)

                if (mode == HealthViewMode.WEEK || mode == HealthViewMode.MONTH) {
                    val response = healthConnectClient.aggregateGroupByDuration(
                        AggregateGroupByDurationRequest(
                            metrics = metrics,
                            timeRangeFilter = timeRangeFilter,
                            timeRangeSlicer = Duration.ofDays(1)
                        )
                    )
                    for (bucket in response) {
                        dataList.add(
                            bucket.startTime to extractHealthData(
                                bucket.result,
                                healthConnectClient,
                                TimeRangeFilter.between(bucket.startTime, bucket.endTime),
                                granted
                            )
                        )
                    }
                } else {
                    dataList.add(start to summaryData)
                }

                _uiState.value = HealthUiState.Success(
                    data = dataList,
                    summaryData = summaryData,
                    selectedMode = mode,
                    selectedDate = date
                )
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error(e.localizedMessage ?: "Failed to fetch data")
            }
        }
    }

    private suspend fun extractHealthData(
        aggregateResponse: AggregationResult?,
        healthConnectClient: HealthConnectClient,
        timeRangeFilter: TimeRangeFilter,
        granted: Set<String>
    ): HealthData {
        val steps = aggregateResponse?.get(StepsRecord.COUNT_TOTAL) ?: 0L
        val heartRate = aggregateResponse?.get(HeartRateRecord.BPM_AVG)?.toDouble() ?: 0.0
        val distance = aggregateResponse?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters ?: 0.0
        val totalSleepDuration = aggregateResponse?.get(SleepSessionRecord.SLEEP_DURATION_TOTAL)

        var height: Double? = null
        if (granted.contains(HealthPermission.getReadPermission(HeightRecord::class))) {
            height = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeightRecord::class,
                    timeRangeFilter = timeRangeFilter,
                    ascendingOrder = false,
                    pageSize = 1
                )
            ).records.firstOrNull()?.height?.inMeters?.let { it * 100 }
        }

        var weight: Double? = null
        if (granted.contains(HealthPermission.getReadPermission(WeightRecord::class))) {
            weight = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    WeightRecord::class,
                    timeRangeFilter = timeRangeFilter,
                    ascendingOrder = false,
                    pageSize = 1
                )
            ).records.firstOrNull()?.weight?.inKilograms
        }

        return HealthData(
            steps = steps,
            heartRate = heartRate,
            distance = distance,
            totalSleepDuration = totalSleepDuration,
            height = height,
            weight = weight
        )
    }
}
