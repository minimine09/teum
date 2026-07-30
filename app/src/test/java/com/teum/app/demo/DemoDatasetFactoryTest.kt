package com.teum.app.demo

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDatasetFactoryTest {
    @Test
    fun createBuildsExpectedRowsAfterEnoughTodayWindow() {
        val dataset = DemoDatasetFactory.create(
            nowMillis = fixedNow(hour = 2),
            timeZone = KST
        )

        assertEquals(DemoExpectedMetrics.SESSION_COUNT, dataset.sessions.size)
        assertEquals(
            DemoExpectedMetrics.EXTENSION_EVENT_COUNT,
            dataset.sessions.sumOf { it.extensionEvents.size }
        )
        assertEquals(DemoExpectedMetrics.REOPEN_LOG_COUNT, dataset.reopenLogs.size)
        assertEquals(DemoExpectedMetrics.APP_OPEN_COUNT, dataset.appOpenEvents.size)
        assertEquals(DemoExpectedMetrics.SELF_CONTROL_EVENT_COUNT, dataset.selfControlEvents.size)
        assertEquals(
            DemoExpectedMetrics.WEEKLY_TOTAL_USAGE_MINUTES * MILLIS_PER_MINUTE,
            dataset.sessions.sumOf { it.entity.effectiveUsageMillis }
        )
        assertEquals(
            DemoExpectedMetrics.RAW_FINAL_OVERRUN_COUNT,
            dataset.sessions.count { it.entity.rawOverrunMillis > 0L }
        )
        assertEquals(
            DemoExpectedMetrics.INITIAL_GOAL_EXCEEDED_COUNT,
            dataset.sessions.count { it.entity.extensionCount > 0 }
        )
    }

    @Test
    fun todayRowsUseSafeFixedTimesAndFastReopenGap() {
        val nowMillis = fixedNow(hour = 2)
        val dataset = DemoDatasetFactory.create(
            nowMillis = nowMillis,
            timeZone = KST
        )
        val sessionsByKey = dataset.sessions.associateBy { it.key }
        val s15 = sessionsByKey.getValue("S15").entity
        val s16 = sessionsByKey.getValue("S16").entity
        val s17 = sessionsByKey.getValue("S17").entity

        assertEquals(3 * MILLIS_PER_MINUTE, s16.entryDetectedAtMillis - s15.endedAtMillis)
        assertEquals(2 * MILLIS_PER_MINUTE, s17.entryDetectedAtMillis - s16.endedAtMillis)
        assertEquals(
            nowMillis - 2 * MILLIS_PER_MINUTE,
            dataset.selfControlEvents.single { it.packageName == DemoToolsContract.CHROME_PACKAGE }
                .occurredAtMillis
        )
        assertEquals(3, dataset.sessions.count { it.key in setOf("S15", "S16", "S17") })
        assertEquals(
            DemoExpectedMetrics.TODAY_USAGE_MINUTES * MILLIS_PER_MINUTE,
            listOf("S15", "S16", "S17").sumOf {
                sessionsByKey.getValue(it).entity.effectiveUsageMillis
            }
        )
    }

    @Test
    fun safeWindowUsesCalculatedMinimumInsteadOfFixedHour() {
        val startOfToday = startOfDay()
        val minimumElapsedMillis = DemoDatasetFactory.minimumRequiredTodayElapsedMillis()

        assertFalse(
            DemoDatasetFactory.canCreateTodayData(
                nowMillis = startOfToday + minimumElapsedMillis - 1_000L,
                timeZone = KST
            )
        )
        assertTrue(
            DemoDatasetFactory.canCreateTodayData(
                nowMillis = startOfToday + minimumElapsedMillis,
                timeZone = KST
            )
        )
        assertTrue(
            DemoDatasetFactory.canCreateTodayData(
                nowMillis = startOfToday + minimumElapsedMillis + 1_000L,
                timeZone = KST
            )
        )
    }

    @Test
    fun representativeEarlyTimesMatchSafeWindow() {
        assertFalse(
            DemoDatasetFactory.canCreateTodayData(
                nowMillis = fixedNow(hour = 0, minute = 10),
                timeZone = KST
            )
        )
        assertTrue(
            DemoDatasetFactory.canCreateTodayData(
                nowMillis = fixedNow(hour = 0, minute = 30),
                timeZone = KST
            )
        )
        assertTrue(
            DemoDatasetFactory.canCreateTodayData(
                nowMillis = fixedNow(hour = 14),
                timeZone = KST
            )
        )
    }

    private fun fixedNow(hour: Int, minute: Int = 0): Long = Calendar.getInstance(KST).apply {
        set(2026, Calendar.JULY, 31, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfDay(): Long = fixedNow(hour = 0)

    private companion object {
        val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
