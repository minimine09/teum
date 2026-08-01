package com.teum.app.dashboard

import java.util.Calendar
import java.util.TimeZone

data class RecentSevenDayChartItem(
    val label: String,
    val stat: DailyOverrunStat,
    val isToday: Boolean
)

object RecentSevenDayChart {
    fun buildItems(
        dailyStats: List<DailyOverrunStat>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<RecentSevenDayChartItem> {
        val statsByDay = dailyStats.associateBy { it.dayOfWeek }
        return (6 downTo 0).map { daysAgo ->
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val label = weekdayLabel(dayOfWeek)
            RecentSevenDayChartItem(
                label = label,
                stat = statsByDay[dayOfWeek] ?: DailyOverrunStat(
                    dayOfWeek = dayOfWeek,
                    label = label,
                    sessionCount = 0,
                    overrunCount = 0,
                    openCount = 0,
                    extensionCount = 0,
                    usageMillis = 0L
                ),
                isToday = daysAgo == 0
            )
        }
    }

    private fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SUNDAY -> "일"
        Calendar.MONDAY -> "월"
        Calendar.TUESDAY -> "화"
        Calendar.WEDNESDAY -> "수"
        Calendar.THURSDAY -> "목"
        Calendar.FRIDAY -> "금"
        Calendar.SATURDAY -> "토"
        else -> ""
    }
}
