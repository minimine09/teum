package com.teum.app.dashboard

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReopenPersistenceInstrumentedTest {
    @Test
    fun reopenCheck_usesPersistedSameAppSessionAndLinksBothSessionIds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SessionLogRepository(context)
        repository.deleteAllSessionLogs()

        val now = System.currentTimeMillis()
        val previousEndTime = now - 2L * 60L * 1_000L
        val previousId = repository.saveEndedSession(
            endedSession(
                packageName = YOUTUBE,
                entryDetectedAtMillis = previousEndTime - 20_000L,
                startedAtMillis = previousEndTime - 10_000L,
                endedAtMillis = previousEndTime
            )
        )!!
        repository.saveEndedSession(
            endedSession(
                packageName = INSTAGRAM,
                entryDetectedAtMillis = now - 30_000L,
                startedAtMillis = now - 20_000L,
                endedAtMillis = now - 10_000L
            )
        )

        val reopened = SessionLogRepository(context).checkReopen(
            packageName = YOUTUBE,
            currentEntryTimeMillis = now
        )
        assertEquals(previousId, reopened.previousSessionId)
        assertEquals(previousEndTime, reopened.previousEndTimeMillis)
        assertEquals(2L * 60L * 1_000L, reopened.gapTimeMillis)
        assertTrue(reopened.isFastReopen)

        val currentEndTime = now + 10_000L
        val currentId = repository.saveEndedSession(
            session = endedSession(
                packageName = YOUTUBE,
                entryDetectedAtMillis = now,
                startedAtMillis = now,
                endedAtMillis = currentEndTime,
                reopenGapMillis = reopened.gapTimeMillis,
                isFastReopen = reopened.isFastReopen
            )
        )!!

        val reopenLogs = repository.observeReopenLogsSince(now - 60_000L).first()
        val reopenLog = reopenLogs.single()
        assertEquals(previousId, reopenLog.previousSessionId)
        assertEquals(currentId, reopenLog.currentSessionId)
        assertEquals(2L * 60L * 1_000L, reopenLog.gapTimeMillis)
        assertTrue(reopenLog.isFastReopen)

        val afterRestart = SessionLogRepository(context).checkReopen(
            packageName = YOUTUBE,
            currentEntryTimeMillis = currentEndTime + 6L * 60L * 1_000L
        )
        assertEquals(currentId, afterRestart.previousSessionId)
        assertEquals(6L * 60L * 1_000L, afterRestart.gapTimeMillis)
        assertFalse(afterRestart.isFastReopen)

        val noPreviousSession = repository.checkReopen(
            packageName = "com.example.no.sessions",
            currentEntryTimeMillis = now
        )
        assertNull(noPreviousSession.previousSessionId)
        assertNull(noPreviousSession.gapTimeMillis)
        assertFalse(noPreviousSession.isFastReopen)
    }

    private fun endedSession(
        packageName: String,
        entryDetectedAtMillis: Long,
        startedAtMillis: Long,
        endedAtMillis: Long,
        reopenGapMillis: Long? = null,
        isFastReopen: Boolean = false
    ): AppSession {
        return AppSession(
            debugSessionId = endedAtMillis,
            packageName = packageName,
            entryDetectedAtMillis = entryDetectedAtMillis,
            startedAtMillis = startedAtMillis,
            intentChoice = IntentChoice.CLEAR_PURPOSE,
            targetDurationMillis = 60_000L,
            isFastReopen = isFastReopen,
            reopenGapMillis = reopenGapMillis,
            endedAtMillis = endedAtMillis
        )
    }

    private companion object {
        const val YOUTUBE = "com.google.android.youtube"
        const val INSTAGRAM = "com.instagram.android"
    }
}
