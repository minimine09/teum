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
        minimumSessionCount: Int = DEFAULT_MINIMUM_SESSION_COUNT,
        maximumSlotCount: Int = DEFAULT_MAXIMUM_SLOT_COUNT
    ): VulnerableTimeAnalysis {
        val rankedSlots = rankVulnerableSlots(
            timeSlotStats = timeSlotStats,
            minimumSessionCount = minimumSessionCount,
            maximumSlotCount = maximumSlotCount
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
        minimumSessionCount: Int = DEFAULT_MINIMUM_SESSION_COUNT,
        maximumSlotCount: Int = DEFAULT_MAXIMUM_SLOT_COUNT
    ): List<TimeSlotStat> {
        require(minimumSessionCount > 0) {
            "minimumSessionCount must be greater than zero"
        }
        require(maximumSlotCount > 0) {
            "maximumSlotCount must be greater than zero"
        }

        return timeSlotStats
            .filter { stat ->
                stat.sessionCount >= minimumSessionCount &&
                    stat.vulnerabilityScore > 0.0
            }
            .sortedWith(
                compareByDescending<TimeSlotStat> { it.vulnerabilityScore }
                    .thenByDescending { it.sessionCount }
                    .thenBy { it.hourSlot }
            )
            .take(maximumSlotCount)
    }

    const val DEFAULT_MINIMUM_SESSION_COUNT = 2
    const val DEFAULT_MAXIMUM_SLOT_COUNT = 2
}
