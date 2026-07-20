package com.teum.app.dashboard

import java.util.Calendar
import java.util.TimeZone

data class DashboardDateRange(
    val startOfTodayMillis: Long,
    val startOfSevenDayPeriodMillis: Long,
    val startOfTomorrowMillis: Long
)

object DashboardDateRangeCalculator {
    fun calculate(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DashboardDateRange {
        val startOfToday = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfSevenDayPeriod = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val startOfTomorrow = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        return DashboardDateRange(
            startOfTodayMillis = startOfToday.timeInMillis,
            startOfSevenDayPeriodMillis = startOfSevenDayPeriod.timeInMillis,
            startOfTomorrowMillis = startOfTomorrow.timeInMillis
        )
    }
}
