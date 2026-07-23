package com.teum.app.dashboard

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.overlay.BrakeChoice
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NecessaryUseAnalyticsInstrumentedTest {
    @Test
    fun onlyClearPurposeNecessaryUse_isExcludedFromOverrunStats() = runBlocking {
        val repository = SessionLogRepository(ApplicationProvider.getApplicationContext())
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        val clearNecessaryId = repository.saveEndedSession(
            session = overrunSession(
                endedAtMillis = now - 3_000L,
                intentChoice = IntentChoice.CLEAR_PURPOSE
            ),
            brakeChoice = BrakeChoice.NECESSARY_USE
        )!!
        val restNecessaryId = repository.saveEndedSession(
            session = overrunSession(
                endedAtMillis = now - 2_000L,
                intentChoice = IntentChoice.MINDFUL_REST
            ),
            brakeChoice = BrakeChoice.NECESSARY_USE
        )!!
        val clearDriftId = repository.saveEndedSession(
            session = overrunSession(
                endedAtMillis = now - 1_000L,
                intentChoice = IntentChoice.CLEAR_PURPOSE
            ),
            brakeChoice = BrakeChoice.PURPOSE_DRIFT
        )!!

        val sessions = repository.observeSessionsForLastSevenDays().first()
        val clearNecessary = sessions.single { it.id == clearNecessaryId }
        val restNecessary = sessions.single { it.id == restNecessaryId }
        val clearDrift = sessions.single { it.id == clearDriftId }

        assertEquals(5_000L, clearNecessary.rawOverrunMillis)
        assertEquals(5_000L, clearNecessary.necessaryUseExcessMillis)
        assertEquals(0L, clearNecessary.overrunMillis)
        assertFalse(clearNecessary.overrun)
        assertEquals(BrakeChoice.NECESSARY_USE.name, clearNecessary.brakeChoice)

        assertEquals(5_000L, restNecessary.rawOverrunMillis)
        assertEquals(0L, restNecessary.necessaryUseExcessMillis)
        assertEquals(5_000L, restNecessary.overrunMillis)
        assertTrue(restNecessary.overrun)

        assertEquals(5_000L, clearDrift.rawOverrunMillis)
        assertEquals(0L, clearDrift.necessaryUseExcessMillis)
        assertEquals(5_000L, clearDrift.overrunMillis)
        assertTrue(clearDrift.overrun)

        val report = WeeklyReportAnalyzer.calculate(
            sessions = sessions,
            timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(sessions)
        )
        assertEquals(3, report.totalSessionCount)
        assertEquals(2, report.overrunCount)
        assertEquals(2.0 / 3.0, report.overrunRate, 0.0001)
        assertEquals(1, report.necessaryUseCount)
        assertEquals(5_000L, report.necessaryUseExcessMillis)
    }

    private fun overrunSession(
        endedAtMillis: Long,
        intentChoice: IntentChoice
    ): AppSession {
        return AppSession(
            debugSessionId = endedAtMillis,
            packageName = "com.google.android.youtube",
            entryDetectedAtMillis = endedAtMillis - 20_000L,
            startedAtMillis = endedAtMillis - 10_000L,
            intentChoice = intentChoice,
            targetDurationMillis = 5_000L,
            endedAtMillis = endedAtMillis
        )
    }
}
