package com.teum.app.dashboard

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teum.app.data.local.entity.SessionLogEntity
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
class BrakeExitOverrunPolicyInstrumentedTest {
    @Test
    fun todayInitialGoalOverrunCount_usesPersistedExtensionCount() = runBlocking {
        val repository = SessionLogRepository(ApplicationProvider.getApplicationContext())
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        repository.saveEndedSession(
            overrunSession(now - 1_000L, IntentChoice.CLEAR_PURPOSE)
        )
        repository.saveEndedSession(
            overrunSession(now, IntentChoice.CLEAR_PURPOSE).copy(
                extensionCount = 1,
                totalExtensionDurationMillis = 5_000L
            )
        )

        val sessions = repository.observeSessionsForLastSevenDays().first()
        val extendedSession = sessions.single { it.extensionCount == 1 }
        assertEquals(1, extendedSession.extensionCount)
        assertEquals(1, repository.observeTodayOverrunCount().first())

        val report = WeeklyReportAnalyzer.calculate(
            sessions = sessions,
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(sessions),
            reopenLogs = emptyList()
        )
        assertEquals(1, report.overrunCount)
        assertEquals(0.5, report.overrunRate, 0.0)
    }

    @Test
    fun confirmedBrakeExit_preservesRawOverrunButInitialGoalStatsRequireExtension() = runBlocking {
        val repository = SessionLogRepository(ApplicationProvider.getApplicationContext())
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        val achievedAndConfirmedId = repository.saveEndedSession(
            overrunSession(now - 5_000L, IntentChoice.CLEAR_PURPOSE)
        )!!
        val necessaryAndConfirmedId = repository.saveEndedSession(
            overrunSession(now - 4_000L, IntentChoice.CLEAR_PURPOSE)
        )!!
        val achievedWithoutConfirmationId = repository.saveEndedSession(
            overrunSession(now - 3_000L, IntentChoice.CLEAR_PURPOSE)
        )!!
        val nonClearAndConfirmedId = repository.saveEndedSession(
            overrunSession(now - 2_000L, IntentChoice.MINDFUL_REST)
        )!!
        val unansweredAndConfirmedId = repository.saveEndedSession(
            overrunSession(now - 1_000L, IntentChoice.CLEAR_PURPOSE)
        )!!

        repository.updateSessionOutcome(
            achievedAndConfirmedId,
            OutcomeType.PURPOSE_ACHIEVED
        )
        repository.updateSessionOutcome(
            necessaryAndConfirmedId,
            OutcomeType.NECESSARY_USE
        )
        repository.updateSessionOutcome(
            achievedWithoutConfirmationId,
            OutcomeType.PURPOSE_ACHIEVED
        )
        repository.updateSessionOutcome(
            nonClearAndConfirmedId,
            OutcomeType.PURPOSE_ACHIEVED
        )

        assertTrue(repository.confirmExitAfterIntervention(achievedAndConfirmedId, now))
        assertTrue(repository.confirmExitAfterIntervention(necessaryAndConfirmedId, now))
        assertTrue(repository.confirmExitAfterIntervention(nonClearAndConfirmedId, now))
        assertTrue(repository.confirmExitAfterIntervention(unansweredAndConfirmedId, now))

        val sessionsById = repository.observeSessionsForLastSevenDays()
            .first()
            .associateBy { it.id }
        val achievedAndConfirmed = sessionsById.getValue(achievedAndConfirmedId)
        val necessaryAndConfirmed = sessionsById.getValue(necessaryAndConfirmedId)
        val achievedWithoutConfirmation = sessionsById.getValue(achievedWithoutConfirmationId)
        val nonClearAndConfirmed = sessionsById.getValue(nonClearAndConfirmedId)
        val unansweredAndConfirmed = sessionsById.getValue(unansweredAndConfirmedId)

        assertEquals(15_000L, achievedAndConfirmed.rawOverrunMillis)
        assertEquals(0L, achievedAndConfirmed.overrunMillis)
        assertFalse(achievedAndConfirmed.overrun)
        assertEquals(true, achievedAndConfirmed.closedAfterIntervention)

        assertOverrunIsRetained(necessaryAndConfirmed)
        assertOverrunIsRetained(achievedWithoutConfirmation)
        assertEquals(null, achievedWithoutConfirmation.closedAfterIntervention)
        assertOverrunIsRetained(nonClearAndConfirmed)
        assertOverrunIsRetained(unansweredAndConfirmed)

        val sessions = sessionsById.values.toList()
        val report = WeeklyReportAnalyzer.calculate(
            sessions = sessions,
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(sessions),
            reopenLogs = emptyList()
        )
        assertEquals(0, report.overrunCount)
        assertEquals(0.0, report.overrunRate, 0.0)
        assertEquals(0, repository.observeTodayOverrunCount().first())
    }

    private fun assertOverrunIsRetained(session: SessionLogEntity) {
        assertEquals(15_000L, session.rawOverrunMillis)
        assertEquals(15_000L, session.overrunMillis)
        assertTrue(session.overrun)
    }

    private fun overrunSession(
        endedAtMillis: Long,
        intentChoice: IntentChoice
    ): AppSession {
        return AppSession(
            debugSessionId = endedAtMillis,
            packageName = "com.google.android.youtube",
            entryDetectedAtMillis = endedAtMillis - 21_000L,
            startedAtMillis = endedAtMillis - 20_000L,
            intentChoice = intentChoice,
            targetDurationMillis = 5_000L,
            endedAtMillis = endedAtMillis
        )
    }
}
