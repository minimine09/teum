package com.teum.app.dashboard

object SessionPolicyDisplayText {
    fun status(
        modeAtStart: String?,
        interventionAppliedAtStart: Boolean
    ): String? {
        return when {
            interventionAppliedAtStart -> "조심 모드 적용"
            modeAtStart == CAUTION_MODE -> "조심 모드 대기"
            modeAtStart != null -> "일반 모드"
            else -> null
        }
    }

    private const val CAUTION_MODE = "CAUTION"
}
