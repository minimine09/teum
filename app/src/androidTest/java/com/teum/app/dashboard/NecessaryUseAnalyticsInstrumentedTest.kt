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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NecessaryUseAnalyticsInstrumentedTest {
    @Test
    fun necessaryUse_preservesTimeOverrunAndTracksSeparateExcess() = runBlocking {
        val repository = SessionLogRepository(ApplicationProvider.getApplicationContext())
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        val clearNecessaryId = repository.saveEndedSession(
            session = overrunSession(
                endedAtMillis = now - 3_000L,
                intentChoice = IntentChoice.CLEAR_PURPOSE,
                outcomeType = OutcomeType.NECESSARY_USE
            )
        )!!
        val restNecessaryId = repository.saveEndedSession(
            session = overrunSession(
                endedAtMillis = now - 2_000L,
                intentChoice = IntentChoice.MINDFUL_REST,
                outcomeType = OutcomeType.NECESSARY_USE
            )
        )!!
        val clearDriftId = repository.saveEndedSession(
            session = overrunSession(
                endedAtMillis = now - 1_000L,
                intentChoice = IntentChoice.CLEAR_PURPOSE,
                outcomeType = OutcomeType.PURPOSE_DRIFT
            )
        )!!

        val sessions = repository.observeSessionsForLastSevenDays().first()
        val clearNecessary = sessions.single { it.id == clearNecessaryId }
        val restNecessary = sessions.single { it.id == restNecessaryId }
        val clearDrift = sessions.single { it.id == clearDriftId }

        assertEquals(5_000L, clearNecessary.rawOverrunMillis)
        assertEquals(5_000L, clearNecessary.necessaryUseExcessMillis)
        assertEquals(5_000L, clearNecessary.overrunMillis)
        assertTrue(clearNecessary.overrun)
        assertEquals(OutcomeType.NECESSARY_USE.name, clearNecessary.outcomeType)
        assertTrue(clearNecessary.outcomeRespondedAtMillis != null)
        assertEquals(false, clearNecessary.outcomeAchieved)
        assertEquals(false, clearNecessary.purposeDrifted)

        assertEquals(5_000L, restNecessary.rawOverrunMillis)
        assertEquals(0L, restNecessary.necessaryUseExcessMillis)
        assertEquals(5_000L, restNecessary.overrunMillis)
        assertTrue(restNecessary.overrun)

        assertEquals(5_000L, clearDrift.rawOverrunMillis)
        assertEquals(0L, clearDrift.necessaryUseExcessMillis)
        assertEquals(5_000L, clearDrift.overrunMillis)
        assertTrue(clearDrift.overrun)
        assertTrue(clearDrift.outcomeRespondedAtMillis != null)
        assertEquals(true, clearDrift.purposeDrifted)

        val report = WeeklyReportAnalyzer.calculate(
            sessions = sessions,
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(sessions),
            reopenLogs = emptyList()
        )
        assertEquals(3, report.totalSessionCount)
        assertEquals(3, report.overrunCount)
        assertEquals(1.0, report.overrunRate, 0.0001)
        assertEquals(1, report.necessaryUseCount)
        assertEquals(5_000L, report.necessaryUseExcessMillis)
    }

    @Test
    fun updatingOutcomeToNecessaryUse_preservesStoredOverrun() = runBlocking {
        val repository = SessionLogRepository(ApplicationProvider.getApplicationContext())
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        val sessionId = repository.saveEndedSession(
            overrunSession(
                endedAtMillis = now,
                intentChoice = IntentChoice.CLEAR_PURPOSE,
                outcomeType = null
            )
        )!!

        assertTrue(
            repository.updateSessionOutcome(
                sessionId = sessionId,
                outcomeType = OutcomeType.NECESSARY_USE,
                respondedAtMillis = now + 1_000L
            )
        )

        val session = repository.observeSessionsForLastSevenDays().first().single()
        assertEquals(5_000L, session.rawOverrunMillis)
        assertEquals(5_000L, session.necessaryUseExcessMillis)
        assertEquals(5_000L, session.overrunMillis)
        assertTrue(session.overrun)
        assertEquals(false, session.outcomeAchieved)
        assertEquals(false, session.purposeDrifted)
    }

    private fun overrunSession(
        endedAtMillis: Long,
        intentChoice: IntentChoice,
        outcomeType: OutcomeType?
    ): AppSession {
        return AppSession(
            debugSessionId = endedAtMillis,
            packageName = "com.google.android.youtube",
            entryDetectedAtMillis = endedAtMillis - 20_000L,
            startedAtMillis = endedAtMillis - 10_000L,
            intentChoice = intentChoice,
            targetDurationMillis = 5_000L,
            outcomeType = outcomeType,
            endedAtMillis = endedAtMillis
        )
    }
}
