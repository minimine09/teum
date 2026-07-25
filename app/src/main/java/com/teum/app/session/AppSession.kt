package com.teum.app.session

import com.teum.app.overlay.IntentChoice

data class AppSession(
    val debugSessionId: Long,
    val packageName: String,
    val entryDetectedAtMillis: Long,
    val startedAtMillis: Long,
    val intentChoice: IntentChoice,
    val targetDurationMillis: Long,
    val currentLimitDurationMillis: Long = targetDurationMillis,
    val isFastReopen: Boolean = false,
    val reopenGapMillis: Long? = null,
    val modeAtStart: String? = null,
    val isVulnerableTimeAtStart: Boolean = false,
    val interventionAppliedAtStart: Boolean = false,
    val extensionCount: Int = 0,
    val totalExtensionDurationMillis: Long = 0L,
    val interventionVisibleMillis: Long = 0L,
    val currentInterventionStartedAtMillis: Long? = null,
    val outcomeType: OutcomeType? = null,
    val endedAtMillis: Long? = null
)
