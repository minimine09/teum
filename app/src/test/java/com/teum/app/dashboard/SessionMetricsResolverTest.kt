package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMetricsResolverTest {
    @Test
    fun versionFourSessionUsesEffectiveFinalAndStoredOverrunMetrics() {
        val metrics = SessionMetricsResolver.resolve(
            session(
                durationMillis = 19_654L,
                targetDurationMillis = 5_000L,
                interventionVisibleMillis = 9_617L,
                effectiveUsageMillis = 10_037L,
                totalExtensionDurationMillis = 5_000L,
                finalTargetDurationMillis = 10_000L,
                overrunMillis = 37L,
                extensionCount = 1
            )
        )

        assertEquals(10_037L, metrics.usageMillis)
        assertEquals(10_000L, metrics.targetMillis)
        assertEquals(37L, metrics.overrunMillis)
        assertEquals(19_654L, metrics.totalDurationMillis)
        assertEquals(9_617L, metrics.interventionVisibleMillis)
        assertEquals(1, metrics.extensionCount)
        assertTrue(metrics.isOverrun)
        assertFalse(metrics.isLegacy)
    }

    @Test
    fun migratedSessionFallsBackToLegacyDurationAndTargetMetrics() {
        val metrics = SessionMetricsResolver.resolve(
            session(
                durationMillis = 70_000L,
                targetDurationMillis = 60_000L,
                effectiveUsageMillis = 0L,
                finalTargetDurationMillis = 0L,
                overrunMillis = 0L
            )
        )

        assertEquals(70_000L, metrics.usageMillis)
        assertEquals(60_000L, metrics.targetMillis)
        assertEquals(10_000L, metrics.overrunMillis)
        assertTrue(metrics.isOverrun)
        assertTrue(metrics.isLegacy)
    }

    private fun session(
        durationMillis: Long,
        targetDurationMillis: Long,
        interventionVisibleMillis: Long = 0L,
        effectiveUsageMillis: Long = 0L,
        totalExtensionDurationMillis: Long = 0L,
        finalTargetDurationMillis: Long = 0L,
        overrunMillis: Long = 0L,
        extensionCount: Int = 0
    ) = SessionLogEntity(
        packageName = "com.google.android.youtube",
        entryDetectedAtMillis = 1_000L,
        startedAtMillis = 2_000L,
        endedAtMillis = 2_000L + durationMillis,
        durationMillis = durationMillis,
        targetDurationMillis = targetDurationMillis,
        interventionVisibleMillis = interventionVisibleMillis,
        effectiveUsageMillis = effectiveUsageMillis,
        totalExtensionDurationMillis = totalExtensionDurationMillis,
        finalTargetDurationMillis = finalTargetDurationMillis,
        overrunMillis = overrunMillis,
        intentChoice = "CLEAR_PURPOSE",
        outcomeType = null,
        overrun = overrunMillis > 0L,
        extensionCount = extensionCount,
        isFastReopen = false,
        reopenGapMillis = null,
        createdAtMillis = 2_000L + durationMillis
    )
}
