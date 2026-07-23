package com.teum.app.dashboard

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import com.teum.app.session.OutcomeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OutcomeAnalyticsInstrumentedTest {
    @Test
    fun outcomeResponses_driveWeeklyDriftAndConfirmedExitStats() = runBlocking {
        val repository = SessionLogRepository(ApplicationProvider.getApplicationContext())
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        val unansweredId = repository.saveEndedSession(sessionEndingAt(now - 4_000L))!!
        val achievedId = repository.saveEndedSession(sessionEndingAt(now - 3_000L))!!
        val driftedId = repository.saveEndedSession(sessionEndingAt(now - 2_000L))!!
        val scrollingId = repository.saveEndedSession(sessionEndingAt(now - 1_000L))!!

        assertTrue(
            repository.updateSessionOutcome(
                sessionId = achievedId,
                outcomeType = OutcomeType.PURPOSE_ACHIEVED,
                respondedAtMillis = now - 2_500L
            )
        )
        assertTrue(
            repository.updateSessionOutcome(
                sessionId = driftedId,
                outcomeType = OutcomeType.PURPOSE_DRIFT,
                respondedAtMillis = now - 1_500L
            )
        )
        assertTrue(
            repository.updateSessionOutcome(
                sessionId = scrollingId,
                outcomeType = OutcomeType.CONTINUED_SCROLLING,
                respondedAtMillis = now - 500L
            )
        )
        assertTrue(
            repository.confirmExitAfterIntervention(
                sessionId = driftedId,
                confirmedAtMillis = now
            )
        )
        assertFalse(
            repository.updateSessionOutcome(
                sessionId = Long.MAX_VALUE,
                outcomeType = OutcomeType.PURPOSE_ACHIEVED
            )
        )

        val sessions = repository.observeSessionsForLastSevenDays().first()
        val unanswered = sessions.single { it.id == unansweredId }
        val achieved = sessions.single { it.id == achievedId }
        val drifted = sessions.single { it.id == driftedId }
        val scrolling = sessions.single { it.id == scrollingId }

        assertEquals(null, unanswered.outcomeRespondedAtMillis)
        assertEquals(true, achieved.outcomeAchieved)
        assertEquals(false, achieved.purposeDrifted)
        assertEquals(false, drifted.outcomeAchieved)
        assertEquals(true, drifted.purposeDrifted)
        assertEquals(OutcomeType.CONTINUED_SCROLLING.name, scrolling.outcomeType)
        assertEquals(false, scrolling.outcomeAchieved)
        assertEquals(true, scrolling.purposeDrifted)
        assertEquals(true, drifted.closedAfterIntervention)
        assertEquals(now, drifted.interventionExitConfirmedAtMillis)

        val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(sessions)
        val report = WeeklyReportAnalyzer.calculate(sessions, timeSlotStats, emptyList())

        assertEquals(4, report.totalSessionCount)
        assertEquals(3, report.outcomeResponseCount)
        assertEquals(2.0 / 4.0, report.purposeDriftRate, 0.0001)
        assertEquals(1, report.closedAfterInterventionCount)
        assertEquals(2, repository.observeTodayPurposeDriftCount().first())
    }

    private fun sessionEndingAt(endedAtMillis: Long): AppSession {
        return AppSession(
            debugSessionId = endedAtMillis,
            packageName = "com.google.android.youtube",
            entryDetectedAtMillis = endedAtMillis - 20_000L,
            startedAtMillis = endedAtMillis - 10_000L,
            intentChoice = IntentChoice.CLEAR_PURPOSE,
            targetDurationMillis = 60_000L,
            endedAtMillis = endedAtMillis
        )
    }
}
