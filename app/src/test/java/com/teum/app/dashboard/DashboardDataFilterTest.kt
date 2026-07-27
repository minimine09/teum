package com.teum.app.dashboard

import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.SessionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardDataFilterTest {
    @Test
    fun nullSelectionKeepsAllAppsAndPackageSelectionKeepsOnlyThatApp() {
        val sessions = listOf(session("youtube", 10), session("instagram", 20))
        val events = listOf(open("youtube"), open("instagram"), open("youtube"))

        assertEquals(2, DashboardDataFilter.sessions(sessions, null).size)
        assertEquals(3, DashboardDataFilter.openEvents(events, null).size)
        assertEquals(listOf("youtube"), DashboardDataFilter.sessions(sessions, "youtube").map { it.packageName })
        assertEquals(2, DashboardDataFilter.openEvents(events, "youtube").size)
    }

    @Test
    fun todayStatsExcludeOlderSessionsAndCountSelectedData() {
        val sessions = listOf(
            session(
                "youtube",
                99,
                overrun = true,
                fast = true,
                drifted = true,
                closedAfterIntervention = true
            ),
            session(
                "youtube",
                100,
                outcomeType = "PURPOSE_ACHIEVED",
                closedAfterIntervention = true
            ),
            session(
                "youtube",
                200,
                overrun = true,
                fast = true,
                drifted = true,
                outcomeType = "PURPOSE_DRIFT"
            )
        )

        val stats = DashboardDataFilter.todayStats(sessions, startOfTodayMillis = 100)

        assertEquals(2, stats.todaySessionCount)
        assertEquals(1, stats.todayOverrunCount)
        assertEquals(1, stats.todayFastReopenCount)
        assertEquals(1, stats.todayPurposeKeptCount)
        assertEquals(1, stats.todayPurposeDriftCount)
        assertEquals(1, stats.todayClosedAfterInterventionCount)
    }

    @Test
    fun todayPurposeKeptCountsOnlySuccessfulClearPurposeOutcomes() {
        val sessions = listOf(
            session("youtube", 100, outcomeType = "PURPOSE_ACHIEVED"),
            session("youtube", 101, outcomeType = "NECESSARY_USE"),
            session("youtube", 102, outcomeType = "PURPOSE_DRIFT", drifted = true),
            session("youtube", 103),
            session(
                "youtube",
                104,
                intentChoice = "MINDFUL_REST",
                outcomeType = "PURPOSE_ACHIEVED"
            ),
            session(
                "youtube",
                105,
                intentChoice = "UNCONSCIOUS_OPEN",
                outcomeType = "NECESSARY_USE"
            )
        )

        val stats = DashboardDataFilter.todayStats(sessions, startOfTodayMillis = 100)

        assertEquals(6, stats.todaySessionCount)
        assertEquals(2, stats.todayPurposeKeptCount)
    }

    private fun open(packageName: String) = AppOpenEventEntity(
        packageName = packageName,
        detectedAtMillis = 100
    )

    private fun session(
        packageName: String,
        startedAt: Long,
        overrun: Boolean = false,
        fast: Boolean = false,
        drifted: Boolean? = null,
        closedAfterIntervention: Boolean? = null,
        intentChoice: String = "CLEAR_PURPOSE",
        outcomeType: String? = null
    ) = SessionLogEntity(
        packageName = packageName,
        entryDetectedAtMillis = startedAt,
        startedAtMillis = startedAt,
        endedAtMillis = startedAt + 1,
        durationMillis = 1,
        targetDurationMillis = 1,
        intentChoice = intentChoice,
        outcomeType = outcomeType,
        purposeDrifted = drifted,
        closedAfterIntervention = closedAfterIntervention,
        overrun = overrun,
        extensionCount = 0,
        isFastReopen = fast,
        reopenGapMillis = null,
        createdAtMillis = startedAt
    )
}
