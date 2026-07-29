package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFlowResolverTest {
    @Test
    fun purposeDriftTakesPriorityOverExtension() {
        val summary = SessionFlowResolver.resolve(
            session(
                outcomeType = "PURPOSE_DRIFT",
                purposeDrifted = true,
                extensionCount = 1,
                totalExtensionDurationMillis = 60_000L
            )
        )

        assertEquals(SessionPrimaryFlowType.PURPOSE_DRIFTED, summary.primaryType)
        assertEquals(SessionFlowTone.WARNING, summary.tone)
        assertTrue(summary.isAttentionSignal)
    }

    @Test
    fun extensionAfterBrakeIsNotRawOverrunPrimaryType() {
        val summary = SessionFlowResolver.resolve(
            session(
                extensionCount = 1,
                totalExtensionDurationMillis = 60_000L,
                rawOverrunMillis = 1L,
                overrunMillis = 1L
            )
        )

        assertEquals(SessionPrimaryFlowType.EXTENDED_AFTER_BRAKE, summary.primaryType)
        assertEquals(SessionFlowTone.NEUTRAL, summary.tone)
        assertTrue(SessionFlowBadge.RAW_OVER_TARGET in summary.badges)
    }

    @Test
    fun repeatedExtensionBecomesAttentionSignal() {
        val summary = SessionFlowResolver.resolve(
            session(
                extensionCount = 2,
                totalExtensionDurationMillis = 120_000L
            )
        )

        assertEquals(SessionPrimaryFlowType.EXTENDED_AFTER_BRAKE, summary.primaryType)
        assertTrue(SessionFlowBadge.MULTIPLE_EXTENSIONS in summary.badges)
        assertEquals(SessionFlowTone.ATTENTION, summary.tone)
    }

    @Test
    fun reachedAndClosedIsPositiveWhenNoExtensionExists() {
        val summary = SessionFlowResolver.resolve(
            session(
                closedAfterIntervention = true,
                effectiveUsageMillis = 60_000L,
                finalTargetDurationMillis = 60_000L
            )
        )

        assertEquals(SessionPrimaryFlowType.REACHED_AND_CLOSED, summary.primaryType)
        assertEquals(SessionFlowTone.POSITIVE, summary.tone)
        assertTrue(summary.isSelfControlPositive)
        assertFalse(summary.isAttentionSignal)
    }

    @Test
    fun endedBeforeTargetUsesEffectiveUsageAndFinalTarget() {
        val summary = SessionFlowResolver.resolve(
            session(
                effectiveUsageMillis = 40_000L,
                finalTargetDurationMillis = 60_000L
            )
        )

        assertEquals(SessionPrimaryFlowType.ENDED_BEFORE_TARGET, summary.primaryType)
        assertEquals(SessionFlowTone.NEUTRAL, summary.tone)
    }

    @Test
    fun fastReopenAndUnconsciousOpenAreBadges() {
        val summary = SessionFlowResolver.resolve(
            session(
                intentChoice = "UNCONSCIOUS_OPEN",
                isFastReopen = true,
                effectiveUsageMillis = 40_000L,
                finalTargetDurationMillis = 60_000L
            )
        )

        assertEquals(SessionPrimaryFlowType.ENDED_BEFORE_TARGET, summary.primaryType)
        assertTrue(SessionFlowBadge.UNCONSCIOUS_OPEN in summary.badges)
        assertTrue(SessionFlowBadge.FAST_REOPENED in summary.badges)
        assertEquals(SessionFlowTone.ATTENTION, summary.tone)
    }

    private fun session(
        intentChoice: String = "CLEAR_PURPOSE",
        outcomeType: String? = null,
        purposeDrifted: Boolean? = null,
        closedAfterIntervention: Boolean? = null,
        durationMillis: Long = 60_000L,
        targetDurationMillis: Long = 60_000L,
        effectiveUsageMillis: Long = 60_000L,
        finalTargetDurationMillis: Long = 60_000L,
        totalExtensionDurationMillis: Long = 0L,
        rawOverrunMillis: Long = 0L,
        overrunMillis: Long = 0L,
        extensionCount: Int = 0,
        isFastReopen: Boolean = false
    ) = SessionLogEntity(
        packageName = "com.google.android.youtube",
        entryDetectedAtMillis = 1_000L,
        startedAtMillis = 2_000L,
        endedAtMillis = 2_000L + durationMillis,
        durationMillis = durationMillis,
        targetDurationMillis = targetDurationMillis,
        effectiveUsageMillis = effectiveUsageMillis,
        finalTargetDurationMillis = finalTargetDurationMillis,
        totalExtensionDurationMillis = totalExtensionDurationMillis,
        rawOverrunMillis = rawOverrunMillis,
        overrunMillis = overrunMillis,
        intentChoice = intentChoice,
        outcomeType = outcomeType,
        purposeDrifted = purposeDrifted,
        closedAfterIntervention = closedAfterIntervention,
        overrun = overrunMillis > 0L,
        extensionCount = extensionCount,
        isFastReopen = isFastReopen,
        reopenGapMillis = null,
        createdAtMillis = 2_000L + durationMillis
    )
}
