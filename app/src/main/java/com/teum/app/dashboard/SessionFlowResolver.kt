package com.teum.app.dashboard

import com.teum.app.data.local.entity.SelfControlEventEntity
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.data.repository.SessionLogRepository

object SessionFlowResolver {
    fun resolve(session: SessionLogEntity): SessionFlowSummary {
        val metrics = SessionMetricsResolver.resolve(session)
        val rawOverrunMillis = maxOf(
            session.rawOverrunMillis,
            session.overrunMillis,
            metrics.overrunMillis,
            0L
        )
        val hasExtended = session.extensionCount > 0 ||
            session.totalExtensionDurationMillis > 0L
        val hasReachedAndClosed = session.closedAfterIntervention == true &&
            !hasExtended
        val hasEndedBeforeTarget = session.closedAfterIntervention != true &&
            !hasExtended &&
            metrics.usageMillis < metrics.targetMillis

        val primaryType = when {
            session.outcomeType == CONTINUED_SCROLLING ->
                SessionPrimaryFlowType.CONTINUED_SCROLLING
            session.outcomeType == PURPOSE_DRIFT || session.purposeDrifted == true ->
                SessionPrimaryFlowType.PURPOSE_DRIFTED
            hasExtended ->
                SessionPrimaryFlowType.EXTENDED_AFTER_BRAKE
            hasReachedAndClosed ->
                SessionPrimaryFlowType.REACHED_AND_CLOSED
            hasEndedBeforeTarget ->
                SessionPrimaryFlowType.ENDED_BEFORE_TARGET
            session.outcomeType == PURPOSE_ACHIEVED || session.outcomeType == NECESSARY_USE ->
                SessionPrimaryFlowType.COMPLETED
            isSavedSession(session) ->
                SessionPrimaryFlowType.COMPLETED
            else ->
                SessionPrimaryFlowType.UNKNOWN
        }

        val badges = buildSet {
            when (session.intentChoice) {
                CLEAR_PURPOSE -> add(SessionFlowBadge.CLEAR_PURPOSE)
                MINDFUL_REST -> add(SessionFlowBadge.MINDFUL_REST)
                UNCONSCIOUS_OPEN -> add(SessionFlowBadge.UNCONSCIOUS_OPEN)
            }
            when (session.outcomeType) {
                PURPOSE_ACHIEVED -> add(SessionFlowBadge.PURPOSE_KEPT)
                NECESSARY_USE -> add(SessionFlowBadge.NECESSARY_USE)
            }
            if (session.isFastReopen) add(SessionFlowBadge.FAST_REOPENED)
            if (rawOverrunMillis > 0L) add(SessionFlowBadge.RAW_OVER_TARGET)
            if (session.extensionCount >= MULTIPLE_EXTENSION_THRESHOLD) {
                add(SessionFlowBadge.MULTIPLE_EXTENSIONS)
            }
        }

        val tone = resolveTone(primaryType, badges)

        return SessionFlowSummary(
            primaryType = primaryType,
            tone = tone,
            badges = badges,
            isSelfControlPositive = primaryType == SessionPrimaryFlowType.REACHED_AND_CLOSED ||
                badges.any { it == SessionFlowBadge.PURPOSE_KEPT || it == SessionFlowBadge.NECESSARY_USE },
            isAttentionSignal = tone == SessionFlowTone.ATTENTION || tone == SessionFlowTone.WARNING,
            rawOverrunMillis = rawOverrunMillis.coerceAtLeast(0L),
            effectiveUsageMillis = metrics.usageMillis,
            finalTargetDurationMillis = metrics.targetMillis
        )
    }

    fun resolveSelfControl(event: SelfControlEventEntity): SessionFlowSummary? {
        if (event.eventType != SessionLogRepository.EVENT_CLOSE_NOW_BEFORE_SESSION) return null

        // CLOSE_NOW_BEFORE_SESSION is the pre-use self-control path. It is not a SessionLog.
        return SessionFlowSummary(
            primaryType = SessionPrimaryFlowType.COMPLETED,
            tone = SessionFlowTone.POSITIVE,
            badges = emptySet(),
            isSelfControlPositive = true,
            isAttentionSignal = false,
            rawOverrunMillis = 0L,
            effectiveUsageMillis = 0L,
            finalTargetDurationMillis = 0L
        )
    }

    fun primaryLabel(type: SessionPrimaryFlowType): String = when (type) {
        SessionPrimaryFlowType.CONTINUED_SCROLLING -> "계속 봄"
        SessionPrimaryFlowType.PURPOSE_DRIFTED -> "목적 이탈"
        SessionPrimaryFlowType.EXTENDED_AFTER_BRAKE -> "연장 사용"
        SessionPrimaryFlowType.REACHED_AND_CLOSED -> "점검 후 종료"
        SessionPrimaryFlowType.ENDED_BEFORE_TARGET -> "일반 종료"
        SessionPrimaryFlowType.COMPLETED -> "완료"
        SessionPrimaryFlowType.UNKNOWN -> "기록됨"
    }

    fun badgeLabel(badge: SessionFlowBadge): String = when (badge) {
        SessionFlowBadge.FAST_REOPENED -> "다시 열림"
        SessionFlowBadge.UNCONSCIOUS_OPEN -> "무의식 실행"
        SessionFlowBadge.MINDFUL_REST -> "인지된 휴식"
        SessionFlowBadge.CLEAR_PURPOSE -> "명확한 목적"
        SessionFlowBadge.PURPOSE_KEPT -> "목적 달성"
        SessionFlowBadge.NECESSARY_USE -> "필요한 사용"
        SessionFlowBadge.RAW_OVER_TARGET -> "시간 차이 있음"
        SessionFlowBadge.MULTIPLE_EXTENSIONS -> "반복 연장"
    }

    private fun resolveTone(
        primaryType: SessionPrimaryFlowType,
        badges: Set<SessionFlowBadge>
    ): SessionFlowTone {
        return when {
            primaryType == SessionPrimaryFlowType.CONTINUED_SCROLLING ||
                primaryType == SessionPrimaryFlowType.PURPOSE_DRIFTED ->
                SessionFlowTone.WARNING
            primaryType == SessionPrimaryFlowType.EXTENDED_AFTER_BRAKE &&
                SessionFlowBadge.MULTIPLE_EXTENSIONS in badges ->
                SessionFlowTone.ATTENTION
            SessionFlowBadge.UNCONSCIOUS_OPEN in badges ||
                SessionFlowBadge.FAST_REOPENED in badges ->
                SessionFlowTone.ATTENTION
            primaryType == SessionPrimaryFlowType.REACHED_AND_CLOSED ||
                SessionFlowBadge.PURPOSE_KEPT in badges ||
                SessionFlowBadge.NECESSARY_USE in badges ->
                SessionFlowTone.POSITIVE
            primaryType == SessionPrimaryFlowType.UNKNOWN ->
                SessionFlowTone.NEUTRAL
            else ->
                SessionFlowTone.NEUTRAL
        }
    }

    private fun isSavedSession(session: SessionLogEntity): Boolean {
        return session.startedAtMillis > 0L && session.endedAtMillis >= session.startedAtMillis
    }

    private const val CLEAR_PURPOSE = "CLEAR_PURPOSE"
    private const val MINDFUL_REST = "MINDFUL_REST"
    private const val UNCONSCIOUS_OPEN = "UNCONSCIOUS_OPEN"
    private const val PURPOSE_ACHIEVED = "PURPOSE_ACHIEVED"
    private const val NECESSARY_USE = "NECESSARY_USE"
    private const val PURPOSE_DRIFT = "PURPOSE_DRIFT"
    private const val CONTINUED_SCROLLING = "CONTINUED_SCROLLING"
    private const val MULTIPLE_EXTENSION_THRESHOLD = 2
}
