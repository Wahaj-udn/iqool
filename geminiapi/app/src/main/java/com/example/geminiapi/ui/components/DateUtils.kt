package com.example.geminiapi.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayInfo(
    val letter: String,
    val dayOfMonth: Int
)

/** The last [count] days, oldest first, ending with today. */
fun lastDays(count: Int = 7): List<DayInfo> {
    val letterFormat = SimpleDateFormat("EEE", Locale.US)
    return (count - 1 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        DayInfo(
            letter = letterFormat.format(cal.time).take(1).uppercase(),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}

/** e.g. "September 2026" */
fun currentMonthLabel(): String = 
    SimpleDateFormat("MMMM yyyy", Locale.US).format(Calendar.getInstance().time)
