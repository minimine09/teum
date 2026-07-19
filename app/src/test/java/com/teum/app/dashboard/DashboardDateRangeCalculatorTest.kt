package com.teum.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DashboardDateRangeCalculatorTest {
    @Test
    fun rangeStartsAtTodayAndIncludesTodayPlusPreviousSixDays() {
        val zone = TimeZone.getTimeZone("Asia/Seoul")
        val now = time(zone, 2026, Calendar.JULY, 19, 15, 30)

        val range = DashboardDateRangeCalculator.calculate(now, zone)

        assertEquals(time(zone, 2026, Calendar.JULY, 19, 0, 0), range.startOfTodayMillis)
        assertEquals(time(zone, 2026, Calendar.JULY, 13, 0, 0), range.startOfSevenDayPeriodMillis)
        assertEquals(time(zone, 2026, Calendar.JULY, 20, 0, 0), range.startOfTomorrowMillis)
    }

    @Test
    fun tomorrowUsesCalendarBoundaryAcrossDaylightSavingChange() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val now = time(zone, 2026, Calendar.MARCH, 8, 12, 0)

        val range = DashboardDateRangeCalculator.calculate(now, zone)

        assertEquals(time(zone, 2026, Calendar.MARCH, 8, 0, 0), range.startOfTodayMillis)
        assertEquals(time(zone, 2026, Calendar.MARCH, 9, 0, 0), range.startOfTomorrowMillis)
        assertEquals(23L * 60L * 60L * 1_000L, range.startOfTomorrowMillis - range.startOfTodayMillis)
    }

    private fun time(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, day, hour, minute)
    }.timeInMillis
}
