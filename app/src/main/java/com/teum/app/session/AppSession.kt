package com.teum.app.session

import com.teum.app.overlay.IntentChoice

data class SessionExtensionEvent(
    val occurredAtMillis: Long,
    val extensionDurationMillis: Long,
    val interventionActiveAtTime: Boolean
)

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
    val currentInterventionActive: Boolean = interventionAppliedAtStart,
    val interventionEverApplied: Boolean = interventionAppliedAtStart,
    val extensionCount: Int = 0,
    val cautionExtensionCount: Int = 0,
    val totalExtensionDurationMillis: Long = 0L,
    val extensionEvents: List<SessionExtensionEvent> = emptyList(),
    val interventionVisibleMillis: Long = 0L,
    val currentInterventionStartedAtMillis: Long? = null,
    val outcomeType: OutcomeType? = null,
    val endedAtMillis: Long? = null
)
