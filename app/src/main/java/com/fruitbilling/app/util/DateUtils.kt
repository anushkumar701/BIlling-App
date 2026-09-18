package com.fruitbilling.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy, h:mm a", Locale.getDefault())

    fun getTodayStartAndEndMillis(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    fun formatBillTimestamp(timestamp: Long): String {
        val (startOfDay, endOfDay) = getTodayStartAndEndMillis()
        val date = Date(timestamp)
        return if (timestamp in startOfDay..endOfDay) {
            "Today ${timeFormat.format(date)}"
        } else {
            fullDateTimeFormat.format(date)
        }
    }

    fun formatDetailedTimestamp(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }
}
