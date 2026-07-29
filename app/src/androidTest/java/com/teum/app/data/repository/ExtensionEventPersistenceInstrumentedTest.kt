package com.teum.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import com.teum.app.session.SessionExtensionEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtensionEventPersistenceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = SessionLogRepository(context)

    @After
    fun clearDatabase() = runBlocking {
        repository.deleteAllSessionLogs()
    }

    @Test
    fun extensionEventsPersistAndCanBeReadByTimeAndSession() = runBlocking {
        repository.deleteAllSessionLogs()
        val sessionId = requireNotNull(
            repository.saveEndedSession(
                AppSession(
                    debugSessionId = 1L,
                    packageName = "com.example.target",
                    entryDetectedAtMillis = 1_000L,
                    startedAtMillis = 2_000L,
                    intentChoice = IntentChoice.CLEAR_PURPOSE,
                    targetDurationMillis = 60_000L,
                    extensionCount = 2,
                    cautionExtensionCount = 1,
                    interventionEverApplied = true,
                    totalExtensionDurationMillis = 240_000L,
                    extensionEvents = listOf(
                        SessionExtensionEvent(
                            occurredAtMillis = 10_000L,
                            extensionDurationMillis = 60_000L,
                            interventionActiveAtTime = false
                        ),
                        SessionExtensionEvent(
                            occurredAtMillis = 20_000L,
                            extensionDurationMillis = 180_000L,
                            interventionActiveAtTime = true
                        )
                    ),
                    endedAtMillis = 62_000L
                )
            )
        )

        val savedSession = repository.observeRecentSessions(limit = 1).first().single()
        assertEquals(1, savedSession.cautionExtensionCount)
        assertTrue(savedSession.interventionEverApplied)

        val recentEvents = repository.observeExtensionEventsSince(15_000L).first()
        assertEquals(1, recentEvents.size)
        assertEquals(180_000L, recentEvents.single().extensionDurationMillis)
        assertTrue(recentEvents.single().interventionActiveAtTime)

        val sessionEvents = repository.observeExtensionEventsForSession(sessionId).first()
        assertEquals(2, sessionEvents.size)
        assertEquals(listOf(10_000L, 20_000L), sessionEvents.map { it.occurredAtMillis })
        assertEquals(listOf(false, true), sessionEvents.map { it.interventionActiveAtTime })

        repository.deleteAllSessionLogs()
        assertTrue(repository.observeExtensionEventsForSession(sessionId).first().isEmpty())
    }
}
