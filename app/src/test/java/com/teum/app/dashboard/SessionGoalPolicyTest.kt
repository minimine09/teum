package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGoalPolicyTest {
    @Test
    fun noExtensionKeepsInitialGoalEvenWhenFinalAllowanceHasTinyRawOverrun() {
        val session = session(extensionCount = 0, overrun = true, overrunMillis = 37L)

        assertTrue(SessionGoalPolicy.keptInitialGoal(session))
        assertFalse(SessionGoalPolicy.exceededInitialGoal(session))
    }

    @Test
    fun anyExtensionExceedsInitialGoalEvenWhenFinalAllowanceWasKept() {
        val session = session(extensionCount = 1, overrun = false, overrunMillis = 0L)

        assertFalse(SessionGoalPolicy.keptInitialGoal(session))
        assertTrue(SessionGoalPolicy.exceededInitialGoal(session))
    }

    private fun session(
        extensionCount: Int,
        overrun: Boolean,
        overrunMillis: Long
    ) = SessionLogEntity(
        packageName = "target",
        entryDetectedAtMillis = 1_000L,
        startedAtMillis = 1_000L,
        endedAtMillis = 16_000L,
        durationMillis = 15_000L,
        targetDurationMillis = 5_000L,
        effectiveUsageMillis = 15_000L,
        totalExtensionDurationMillis = 10_000L,
        finalTargetDurationMillis = 15_000L,
        overrunMillis = overrunMillis,
        intentChoice = "CLEAR_PURPOSE",
        outcomeType = null,
        overrun = overrun,
        extensionCount = extensionCount,
        isFastReopen = false,
        reopenGapMillis = null,
        createdAtMillis = 16_000L
    )
}
