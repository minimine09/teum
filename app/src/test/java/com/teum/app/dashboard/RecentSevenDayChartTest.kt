package com.teum.app.dashboard

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentSevenDayChartTest {
    @Test
    fun fridayShowsSaturdayThroughFridayWithTodayAtEnd() {
        val items = RecentSevenDayChart.buildItems(
            dailyStats = statsForAllDays(),
            nowMillis = fixedNow(2026, Calendar.JULY, 31),
            timeZone = KST
        )

        assertEquals(listOf("토", "일", "월", "화", "수", "목", "금"), items.map { it.label })
        assertEquals(Calendar.FRIDAY, items.last().stat.dayOfWeek)
        assertTrue(items.last().isToday)
    }

    @Test
    fun mondayShowsTuesdayThroughMondayWithTodayAtEnd() {
        val items = RecentSevenDayChart.buildItems(
            dailyStats = statsForAllDays(),
            nowMillis = fixedNow(2026, Calendar.AUGUST, 3),
            timeZone = KST
        )

        assertEquals(listOf("화", "수", "목", "금", "토", "일", "월"), items.map { it.label })
        assertEquals(Calendar.MONDAY, items.last().stat.dayOfWeek)
        assertTrue(items.last().isToday)
    }

    @Test
    fun sundayShowsMondayThroughSundayWithTodayAtEnd() {
        val items = RecentSevenDayChart.buildItems(
            dailyStats = statsForAllDays(),
            nowMillis = fixedNow(2026, Calendar.AUGUST, 2),
            timeZone = KST
        )

        assertEquals(listOf("월", "화", "수", "목", "금", "토", "일"), items.map { it.label })
        assertEquals(Calendar.SUNDAY, items.last().stat.dayOfWeek)
        assertTrue(items.last().isToday)
    }

    @Test
    fun yearBoundaryKeepsCalendarDayOrder() {
        val nowMillis = fixedNow(2027, Calendar.JANUARY, 1)
        val items = RecentSevenDayChart.buildItems(
            dailyStats = statsForAllDays(),
            nowMillis = nowMillis,
            timeZone = KST
        )
        val expectedDays = (6 downTo 0).map { daysAgo ->
            Calendar.getInstance(KST).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.get(Calendar.DAY_OF_WEEK)
        }

        assertEquals(expectedDays, items.map { it.stat.dayOfWeek })
        assertTrue(items.last().isToday)
    }

    @Test
    fun emptyStatsStillReturnsSevenZeroItems() {
        val items = RecentSevenDayChart.buildItems(
            dailyStats = emptyList(),
            nowMillis = fixedNow(2026, Calendar.JULY, 31),
            timeZone = KST
        )

        assertEquals(7, items.size)
        assertTrue(items.all { it.stat.sessionCount == 0 })
        assertTrue(items.all { it.stat.overrunCount == 0 })
        assertTrue(items.all { it.stat.openCount == 0 })
        assertTrue(items.all { it.stat.extensionCount == 0 })
        assertTrue(items.all { it.stat.usageMillis == 0L })
        assertTrue(items.last().isToday)
    }

    private fun statsForAllDays(): List<DailyOverrunStat> = listOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    ).map { dayOfWeek ->
        DailyOverrunStat(
            dayOfWeek = dayOfWeek,
            label = "x",
            sessionCount = dayOfWeek,
            overrunCount = dayOfWeek,
            openCount = dayOfWeek,
            extensionCount = dayOfWeek,
            usageMillis = dayOfWeek.toLong()
        )
    }

    private fun fixedNow(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance(KST).apply {
            set(year, month, dayOfMonth, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private companion object {
        val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
    }
}
