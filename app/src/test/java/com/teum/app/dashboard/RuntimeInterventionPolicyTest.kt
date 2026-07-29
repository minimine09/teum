package com.teum.app.session

import com.teum.app.overlay.IntentChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeInterventionPolicyTest {
    @Test
    fun `normal mode stays normal even during a vulnerable hour`() {
        assertFalse(
            RuntimeInterventionPolicy.isActive(
                interventionModeEnabled = false,
                isVulnerableTime = true
            )
        )
    }

    @Test
    fun `intervention mode activates only during a vulnerable hour`() {
        assertFalse(
            RuntimeInterventionPolicy.isActive(
                interventionModeEnabled = true,
                isVulnerableTime = false
            )
        )
        assertTrue(
            RuntimeInterventionPolicy.isActive(
                interventionModeEnabled = true,
                isVulnerableTime = true
            )
        )
    }

    @Test
    fun `entering caution starts its extension count at zero`() {
        val updated = RuntimeInterventionPolicy.updateSession(
            session = session(),
            interventionActive = true
        )

        assertTrue(updated.currentInterventionActive)
        assertTrue(updated.interventionEverApplied)
        assertEquals(0, updated.cautionExtensionCount)
    }

    @Test
    fun `caution extension count survives normal time and resumes on reentry`() {
        var session = session()

        session = RuntimeInterventionPolicy.extendSession(
            session = session,
            elapsedMillis = 1_000L,
            extraMillis = 10_000L,
            nowMillis = 11_000L
        )
        assertEquals(1, session.extensionCount)
        assertEquals(0, session.cautionExtensionCount)
        assertEquals(11_000L, session.extensionEvents.single().occurredAtMillis)
        assertFalse(session.extensionEvents.single().interventionActiveAtTime)

        session = RuntimeInterventionPolicy.updateSession(session, interventionActive = true)
        repeat(2) { index ->
            session = RuntimeInterventionPolicy.extendSession(
                session = session,
                elapsedMillis = 2_000L,
                extraMillis = 10_000L,
                nowMillis = 12_000L + index
            )
        }
        assertEquals(3, session.extensionCount)
        assertEquals(2, session.cautionExtensionCount)
        assertEquals(listOf(11_000L, 12_000L, 12_001L), session.extensionEvents.map { it.occurredAtMillis })
        assertEquals(listOf(false, true, true), session.extensionEvents.map { it.interventionActiveAtTime })
        assertFalse(RuntimeInterventionPolicy.isCautionExtensionLimitReached(session))

        session = RuntimeInterventionPolicy.updateSession(session, interventionActive = false)
        session = RuntimeInterventionPolicy.extendSession(
            session = session,
            elapsedMillis = 3_000L,
            extraMillis = 10_000L,
            nowMillis = 13_000L
        )
        assertEquals(4, session.extensionCount)
        assertEquals(2, session.cautionExtensionCount)
        assertFalse(RuntimeInterventionPolicy.isCautionExtensionLimitReached(session))

        session = RuntimeInterventionPolicy.updateSession(session, interventionActive = true)
        session = RuntimeInterventionPolicy.extendSession(
            session = session,
            elapsedMillis = 4_000L,
            extraMillis = 10_000L,
            nowMillis = 14_000L
        )
        assertEquals(5, session.extensionCount)
        assertEquals(3, session.cautionExtensionCount)
        assertTrue(RuntimeInterventionPolicy.isCautionExtensionLimitReached(session))
        assertTrue(session.interventionEverApplied)
        assertEquals(
            listOf(false, true, true, false, true),
            session.extensionEvents.map { it.interventionActiveAtTime }
        )
    }

    @Test
    fun `records overrun onset from effective usage only after target is exceeded`() {
        val unchanged = RuntimeInterventionPolicy.recordOverrunCandidate(
            session = session(),
            effectiveUsageMillis = 60_000L,
            finalTargetDurationMillis = 60_000L,
            nowMillis = 100_000L
        )
        val detected = RuntimeInterventionPolicy.recordOverrunCandidate(
            session = unchanged,
            effectiveUsageMillis = 65_000L,
            finalTargetDurationMillis = 60_000L,
            nowMillis = 100_000L
        )

        assertEquals(null, unchanged.overrunDetectedAtMillis)
        assertEquals(95_000L, detected.overrunDetectedAtMillis)
    }

    @Test
    fun `extension clears previous overrun candidate after final target moves`() {
        val extended = RuntimeInterventionPolicy.extendSession(
            session = session().copy(overrunDetectedAtMillis = 95_000L),
            elapsedMillis = 65_000L,
            extraMillis = 10_000L,
            nowMillis = 100_000L
        )

        assertEquals(null, extended.overrunDetectedAtMillis)
    }

    private fun session(): AppSession {
        return AppSession(
            debugSessionId = 1L,
            packageName = "com.example.target",
            entryDetectedAtMillis = 0L,
            startedAtMillis = 0L,
            intentChoice = IntentChoice.CLEAR_PURPOSE,
            targetDurationMillis = 60_000L
        )
    }
}
