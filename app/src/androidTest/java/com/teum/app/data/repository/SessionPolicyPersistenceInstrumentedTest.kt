package com.teum.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teum.app.data.local.TeumDatabase
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionPolicyPersistenceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = TeumDatabase.getInstance(context)
    private val repository = SessionLogRepository(context)

    @After
    fun clearDatabase() = runBlocking {
        repository.deleteAllSessionLogs()
    }

    @Test
    fun saveEndedSession_persistsPolicyStateAtSessionStart() = runBlocking {
        val startedAtMillis = 10_000L
        val packageName = "com.google.android.youtube"

        val sessionId = repository.saveEndedSession(
            AppSession(
                debugSessionId = 1L,
                packageName = packageName,
                entryDetectedAtMillis = startedAtMillis,
                startedAtMillis = startedAtMillis,
                intentChoice = IntentChoice.CLEAR_PURPOSE,
                targetDurationMillis = 60_000L,
                modeAtStart = "CAUTION",
                isVulnerableTimeAtStart = true,
                interventionAppliedAtStart = true,
                endedAtMillis = startedAtMillis + 30_000L
            )
        )

        assertNotNull(sessionId)
        val saved = database.sessionLogDao().findLatestEndedSession(
            packageName = packageName,
            beforeMillis = startedAtMillis + 30_001L
        )
        assertNotNull(saved)
        assertEquals("CAUTION", saved?.modeAtStart)
        assertTrue(saved?.isVulnerableTimeAtStart == true)
        assertTrue(saved?.interventionAppliedAtStart == true)
    }
}
