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
        assertEquals(1, stats.todayTargetKeptCount)
        assertEquals(0, stats.todayExtensionCount)
        assertEquals(2L, stats.todayUsageMillis)
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

    @Test
    fun todayStatsSummarizesUsageExtensionsFastReopenAndTopApps() {
        val sessions = listOf(
            session(
                "youtube",
                100,
                durationMillis = 10_000,
                extensionCount = 2,
                appDisplayName = "YouTube"
            ),
            session(
                "instagram",
                110,
                durationMillis = 30_000,
                overrun = true,
                fast = true,
                appDisplayName = "Instagram"
            ),
            session(
                "youtube",
                120,
                durationMillis = 20_000,
                appDisplayName = "YouTube"
            ),
            session(
                "older",
                99,
                durationMillis = 1_000,
                extensionCount = 5,
                fast = true
            )
        )

        val stats = DashboardDataFilter.todayStats(sessions, startOfTodayMillis = 100)

        assertEquals(3, stats.todaySessionCount)
        assertEquals(60_000L, stats.todayUsageMillis)
        assertEquals(2, stats.todayTargetKeptCount)
        assertEquals(2, stats.todayExtensionCount)
        assertEquals(1, stats.todayFastReopenCount)
        assertEquals(listOf("youtube", "instagram"), stats.todayAppUsageStats.map { it.packageName })
        assertEquals(30_000L, stats.todayAppUsageStats[0].usageMillis)
        assertEquals("YouTube", stats.todayAppUsageStats[0].appDisplayName)
    }

    @Test
    fun todayAppUsageKeepsOnlyTopFiveByUsage() {
        val sessions = (1..6).map { index ->
            session(
                packageName = "app$index",
                startedAt = 100L + index,
                durationMillis = index * 1_000L
            )
        }

        val stats = DashboardDataFilter.todayStats(sessions, startOfTodayMillis = 100)

        assertEquals(5, stats.todayAppUsageStats.size)
        assertEquals(listOf("app6", "app5", "app4", "app3", "app2"), stats.todayAppUsageStats.map { it.packageName })
    }

    @Test
    fun recentSessionsFiltersBeforeApplyingLimit() {
        val sessions = (1L..11L).map { index ->
            session("instagram", startedAt = 100L + index)
        } + session("youtube", startedAt = 1L)

        val homeSessions = DashboardDataFilter.recentSessions(
            allSessions = sessions,
            selectedPackageName = null
        )
        val youtubeSessions = DashboardDataFilter.recentSessions(
            allSessions = sessions,
            selectedPackageName = "youtube"
        )

        assertEquals(10, homeSessions.size)
        assertEquals(setOf("instagram"), homeSessions.map { it.packageName }.toSet())
        assertEquals(listOf("youtube"), youtubeSessions.map { it.packageName })
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
        outcomeType: String? = null,
        durationMillis: Long = 1,
        extensionCount: Int = 0,
        appDisplayName: String? = null
    ): SessionLogEntity {
        val targetMillis = if (overrun) {
            (durationMillis - 1L).coerceAtLeast(0L)
        } else {
            durationMillis
        }
        val overrunMillis = (durationMillis - targetMillis).coerceAtLeast(0L)
        return SessionLogEntity(
            packageName = packageName,
            appDisplayName = appDisplayName,
            entryDetectedAtMillis = startedAt,
            startedAtMillis = startedAt,
            endedAtMillis = startedAt + durationMillis,
            durationMillis = durationMillis,
            targetDurationMillis = targetMillis,
            effectiveUsageMillis = durationMillis,
            finalTargetDurationMillis = targetMillis,
            overrunMillis = overrunMillis,
            intentChoice = intentChoice,
            outcomeType = outcomeType,
            purposeDrifted = drifted,
            closedAfterIntervention = closedAfterIntervention,
            overrun = overrunMillis > 0L,
            extensionCount = extensionCount,
            isFastReopen = fast,
            reopenGapMillis = null,
            createdAtMillis = startedAt
        )
    }
}
