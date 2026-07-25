package com.teum.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionPolicyDisplayTextTest {
    @Test
    fun appliedPolicyTakesPriority() {
        assertEquals(
            "조심 모드 적용",
            SessionPolicyDisplayText.status(
                modeAtStart = "CAUTION",
                interventionAppliedAtStart = true
            )
        )
    }

    @Test
    fun cautionOutsideVulnerableTimeIsWaiting() {
        assertEquals(
            "조심 모드 대기",
            SessionPolicyDisplayText.status(
                modeAtStart = "CAUTION",
                interventionAppliedAtStart = false
            )
        )
    }

    @Test
    fun normalModeIsShownAsNormal() {
        assertEquals(
            "일반 모드",
            SessionPolicyDisplayText.status(
                modeAtStart = "NORMAL",
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
