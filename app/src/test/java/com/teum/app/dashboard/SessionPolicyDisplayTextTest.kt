package com.teum.app.dashboard

import com.teum.app.core.model.InterventionMode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionPolicyDisplayTextTest {
    @Test
    fun appliedPolicyTakesPriority() {
        assertEquals(
            "조심 모드 적용",
            SessionPolicyDisplayText.status(
                modeAtStart = InterventionMode.INTERVENTION.name,
                interventionAppliedAtStart = true
            )
        )
    }

    @Test
    fun cautionOutsideVulnerableTimeIsWaiting() {
        assertEquals(
            "조심 모드 대기",
            SessionPolicyDisplayText.status(
                modeAtStart = InterventionMode.INTERVENTION.name,
                interventionAppliedAtStart = false
            )
        )
    }

    @Test
    fun normalModeIsShownAsNormal() {
        assertEquals(
            "일반 모드",
            SessionPolicyDisplayText.status(
                modeAtStart = InterventionMode.NORMAL.name,
                interventionAppliedAtStart = false
            )
        )
    }

    @Test
    fun legacySessionWithoutModeDoesNotInventStatus() {
        assertNull(
            SessionPolicyDisplayText.status(
                modeAtStart = null,
                interventionAppliedAtStart = false
            )
        )
    }
}
