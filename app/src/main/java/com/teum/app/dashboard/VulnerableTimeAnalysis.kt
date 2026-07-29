package com.teum.app.dashboard

import java.util.Calendar
import java.util.TimeZone

data class VulnerableTimeAnalysis(
    val hasEnoughData: Boolean,
    val vulnerableHourSlots: Set<Int>,
    val timeSlotStats: List<TimeSlotStat>,
    val analyzedAtMillis: Long
) {
    fun isVulnerableAt(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (!hasEnoughData || vulnerableHourSlots.isEmpty()) return false
        val hourSlot = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }.get(Calendar.HOUR_OF_DAY)
        return hourSlot in vulnerableHourSlots
    }
}

object VulnerableTimeSelector {
    fun select(
        timeSlotStats: List<TimeSlotStat>,
        analyzedAtMillis: Long,
        minimumSessionCount: Int = DEFAULT_MINIMUM_SESSION_COUNT
    ): VulnerableTimeAnalysis {
        val rankedSlots = rankVulnerableSlots(
            timeSlotStats = timeSlotStats,
            minimumSessionCount = minimumSessionCount
        )
        val hasEnoughData = timeSlotStats.any { stat ->
            stat.sessionCount >= minimumSessionCount
        }
        val vulnerableHourSlots = rankedSlots
            .mapTo(linkedSetOf()) { it.hourSlot }

        return VulnerableTimeAnalysis(
            hasEnoughData = hasEnoughData,
            vulnerableHourSlots = vulnerableHourSlots,
            timeSlotStats = timeSlotStats,
            analyzedAtMillis = analyzedAtMillis
        )
    }

    fun rankVulnerableSlots(
        timeSlotStats: List<TimeSlotStat>,
        minimumSessionCount: Int = DEFAULT_MINIMUM_SESSION_COUNT
    ): List<TimeSlotStat> {
        require(minimumSessionCount > 0) {
            "minimumSessionCount must be greater than zero"
        }

        return timeSlotStats
            .filter { stat ->
                stat.sessionCount >= minimumSessionCount &&
                    stat.vulnerabilityScore + SCORE_COMPARISON_EPSILON >=
                    VULNERABILITY_SCORE_THRESHOLD
            }
            .sortedWith(
                compareByDescending<TimeSlotStat> { it.vulnerabilityScore }
                    .thenByDescending { it.sessionCount }
                    .thenBy { it.hourSlot }
            )
    }

    const val DEFAULT_MINIMUM_SESSION_COUNT = 2
    const val VULNERABILITY_SCORE_THRESHOLD = 0.5
    private const val SCORE_COMPARISON_EPSILON = 1e-9
}
