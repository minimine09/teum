package com.teum.app.dashboard

data class SessionFlowSummary(
    val primaryType: SessionPrimaryFlowType,
    val tone: SessionFlowTone,
    val badges: Set<SessionFlowBadge>,
    val isSelfControlPositive: Boolean,
    val isAttentionSignal: Boolean,
    val rawOverrunMillis: Long,
    val effectiveUsageMillis: Long,
    val finalTargetDurationMillis: Long
)
