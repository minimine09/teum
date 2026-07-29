package com.teum.app.data.repository

import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.dashboard.VulnerabilityAnalyzer
import com.teum.app.dashboard.VulnerableTimeSelector
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VulnerableTimePolicyDataFilterTest {
    @Test fun keepsOnlyCurrentlyManagedAppData() {
        val sessions = listOf(
            session(packageName = "managed.youtube"),
            session(packageName = "managed.instagram"),
            session(packageName = "removed.app")
        )
        val openEvents = listOf(
            AppOpenEventEntity(packageName = "managed.youtube", detectedAtMillis = 1L),
            AppOpenEventEntity(packageName = "managed.instagram", detectedAtMillis = 2L),
            AppOpenEventEntity(packageName = "removed.app", detectedAtMillis = 3L)
        )
        val targetPackages = setOf("managed.youtube", "managed.instagram")

        assertEquals(
            listOf("managed.youtube", "managed.instagram"),
            VulnerableTimePolicyDataFilter.sessions(sessions, targetPackages)
                .map { it.packageName }
        )
        assertEquals(
            listOf("managed.youtube", "managed.instagram"),
            VulnerableTimePolicyDataFilter.openEvents(openEvents, targetPackages)
                .map { it.packageName }
        )
    }

    @Test fun emptyManagedAppSelectionProducesNoPolicyData() {
        assertTrue(
            VulnerableTimePolicyDataFilter.sessions(
                sessions = listOf(session(packageName = "removed.app")),
                targetPackages = emptySet()
            ).isEmpty()
        )
        assertTrue(
            VulnerableTimePolicyDataFilter.openEvents(
                openEvents = listOf(
                    AppOpenEventEntity(packageName = "removed.app", detectedAtMillis = 1L)
                ),
                targetPackages = emptySet()
            ).isEmpty()
        )
    }

    @Test fun removedAppCannotBecomePolicyOrReportRepresentative() {
        val utc = TimeZone.getTimeZone("UTC")
        val sessions = listOf(
            session(
                packageName = "managed.youtube",
                startedAtMillis = time(hour = 9, utc),
                overrun = true,
                fastReopen = true
            ),
            session(
                packageName = "managed.youtube",
                startedAtMillis = time(hour = 9, utc),
                overrun = true
            ),
            session(
                packageName = "removed.app",
                startedAtMillis = time(hour = 22, utc),
                overrun = true,
                fastReopen = true
            ),
            session(
                packageName = "removed.app",
                startedAtMillis = time(hour = 22, utc),
                overrun = true,
                fastReopen = true
            )
        )
        val managedSessions = VulnerableTimePolicyDataFilter.sessions(
            sessions = sessions,
            targetPackages = setOf("managed.youtube")
        )
        val stats = VulnerabilityAnalyzer.calculateTimeSlotStats(
            sessions = managedSessions,
            openEvents = emptyList(),
            timeZone = utc
        )
        val rankedCandidates = VulnerableTimeSelector.rankVulnerableSlots(stats)

        assertEquals(listOf(9), rankedCandidates.map { it.hourSlot })
    }

    private fun session(
        packageName: String,
        startedAtMillis: Long = 1L,
        overrun: Boolean = false,
        fastReopen: Boolean = false
    ) = SessionLogEntity(
        packageName = packageName,
        entryDetectedAtMillis = startedAtMillis,
        startedAtMillis = startedAtMillis,
        endedAtMillis = startedAtMillis + 1L,
        durationMillis = 1L,
        targetDurationMillis = 1L,
        intentChoice = "CLEAR_PURPOSE",
        outcomeType = null,
        overrun = overrun,
        extensionCount = 0,
        isFastReopen = fastReopen,
        reopenGapMillis = if (fastReopen) 30_000L else null,
        createdAtMillis = startedAtMillis + 1L
    )

    private fun time(hour: Int, timeZone: TimeZone): Long {
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(2026, Calendar.JULY, 27, hour, 0)
        }.timeInMillis
    }
}
