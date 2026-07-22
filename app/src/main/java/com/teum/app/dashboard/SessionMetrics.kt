package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity

data class SessionMetrics(
    val usageMillis: Long,
    val targetMillis: Long,
    val overrunMillis: Long,
    val totalDurationMillis: Long,
    val interventionVisibleMillis: Long,
    val extensionCount: Int,
    val isLegacy: Boolean
) {
    val isOverrun: Boolean
        get() = overrunMillis > 0L
}

object SessionMetricsResolver {
    fun resolve(session: SessionLogEntity): SessionMetrics {
        val isLegacy = session.finalTargetDurationMillis == 0L &&
            session.targetDurationMillis > 0L
        val usageMillis = if (isLegacy) {
            session.durationMillis
        } else {
            session.effectiveUsageMillis
        }
        val targetMillis = if (isLegacy) {
            session.targetDurationMillis
        } else {
            session.finalTargetDurationMillis
        }
        val overrunMillis = if (isLegacy) {
            (usageMillis - targetMillis).coerceAtLeast(0L)
        } else {
            session.overrunMillis.coerceAtLeast(0L)
        }

        return SessionMetrics(
            usageMillis = usageMillis.coerceAtLeast(0L),
            targetMillis = targetMillis.coerceAtLeast(0L),
            overrunMillis = overrunMillis,
            totalDurationMillis = session.durationMillis.coerceAtLeast(0L),
            interventionVisibleMillis = session.interventionVisibleMillis.coerceAtLeast(0L),
            extensionCount = session.extensionCount.coerceAtLeast(0),
            isLegacy = isLegacy
        )
    }
}
