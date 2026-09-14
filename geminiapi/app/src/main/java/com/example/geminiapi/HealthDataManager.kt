package com.example.geminiapi

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class HealthDataManager(private val context: Context) {

    private val healthConnectClient by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    suspend fun fetchLast15DaysSummary(): String {
        val client = healthConnectClient ?: return "Health Connect is not available on this device."
        
        val granted = client.permissionController.getGrantedPermissions()
        if (granted.isEmpty()) return "No health permissions granted."

        val start = Instant.now().minus(15, ChronoUnit.DAYS)
        val end = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

        val sb = StringBuilder()
        sb.append("Here is the user's health data for the last 15 days for your context:\n\n")

        try {
            // Aggregate daily data
            val metrics = mutableSetOf<AggregateMetric<*>>()
            if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) metrics.add(StepsRecord.COUNT_TOTAL)
            if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) metrics.add(HeartRateRecord.BPM_AVG)
            if (granted.contains(HealthPermission.getReadPermission(DistanceRecord::class))) metrics.add(DistanceRecord.DISTANCE_TOTAL)
            if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) metrics.add(SleepSessionRecord.SLEEP_DURATION_TOTAL)

            if (metrics.isNotEmpty()) {
                val response = client.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = metrics,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        timeRangeSlicer = Duration.ofDays(1)
                    )
                )

                for (bucket in response) {
                    val dateStr = bucket.startTime.atZone(zoneId).toLocalDate().format(dateFormatter)
                    sb.append("Date: $dateStr\n")
                    
                    bucket.result[StepsRecord.COUNT_TOTAL]?.let { sb.append("- Steps: $it\n") }
                    bucket.result[HeartRateRecord.BPM_AVG]?.let { sb.append("- Avg Heart Rate: $it BPM\n") }
                    bucket.result[DistanceRecord.DISTANCE_TOTAL]?.let { 
                        sb.append("- Distance: ${String.format(Locale.US, "%.2f", it.inMeters / 1000.0)} km\n") 
                    }
                    bucket.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.let { 
                        val hours = it.toHours()
                        val minutes = it.toMinutes() % 60
                        sb.append("- Sleep: ${hours}h ${minutes}m\n")
                    }
                    sb.append("\n")
                }
            }

            // Fetch latest Height and Weight
            if (granted.contains(HealthPermission.getReadPermission(HeightRecord::class))) {
                client.readRecords(
                    ReadRecordsRequest(
                        HeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records.firstOrNull()?.let {
                    sb.append("Current Height: ${String.format(Locale.US, "%.1f", it.height.inMeters * 100)} cm\n")
                }
            }

            if (granted.contains(HealthPermission.getReadPermission(WeightRecord::class))) {
                client.readRecords(
                    ReadRecordsRequest(
                        WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                ).records.firstOrNull()?.let {
                    sb.append("Current Weight: ${String.format(Locale.US, "%.1f", it.weight.inKilograms)} kg\n")
                }
            }

        } catch (e: Exception) {
            return "Error fetching health data: ${e.localizedMessage}"
        }

        return sb.toString()
    }
}
