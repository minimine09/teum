package com.teum.app.dashboard

import com.teum.app.core.model.InterventionMode

object SessionPolicyDisplayText {
    fun status(
        modeAtStart: String?,
        interventionAppliedAtStart: Boolean,
        interventionEverApplied: Boolean = false
    ): String? {
        return when {
            interventionEverApplied || interventionAppliedAtStart -> "조심 모드 적용"
            modeAtStart == InterventionMode.INTERVENTION.name -> "조심 모드 대기"
            modeAtStart != null -> "일반 모드"
            else -> null
        }
    }
}
