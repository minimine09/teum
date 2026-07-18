package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity
import java.util.Calendar

object WeeklyReportAnalyzer {
    fun calculate(
        sessions: List<SessionLogEntity>,
        timeSlotStats: List<TimeSlotStat>
    ): WeeklyReportStats {
        val totalSessionCount = sessions.size
        val overrunCount = sessions.count { it.overrun }
        val purposeDriftCount = sessions.count { it.outcomeType == PURPOSE_DRIFT }
        val reopenGaps = sessions.mapNotNull { it.reopenGapMillis }
        val mostVulnerableHourSlot = timeSlotStats
            .filter { it.sessionCount > 0 }
            .maxWithOrNull(
                compareBy<TimeSlotStat> { it.vulnerabilityScore }
                    .thenBy { it.sessionCount }
            )
            ?.hourSlot

        return WeeklyReportStats(
            totalSessionCount = totalSessionCount,
            overrunCount = overrunCount,
            overrunRate = rate(overrunCount, totalSessionCount),
            extensionCount = sessions.sumOf { it.extensionCount },
            fastReopenCount = sessions.count { it.isFastReopen },
            purposeDriftRate = rate(purposeDriftCount, totalSessionCount),
            averageReopenGapMillis = if (reopenGaps.isEmpty()) null else reopenGaps.average().toLong(),
            mostVulnerableHourSlot = mostVulnerableHourSlot,
            dailyOverrunStats = calculateDailyOverrunStats(sessions)
        )
    }

    private fun calculateDailyOverrunStats(
        sessions: List<SessionLogEntity>
    ): List<DailyOverrunStat> {
        val sessionsByDay = sessions.groupBy { session ->
            Calendar.getInstance().apply {
                timeInMillis = session.startedAtMillis
            }.get(Calendar.DAY_OF_WEEK)
        }

        return DAYS.map { (dayOfWeek, label) ->
            val daySessions = sessionsByDay[dayOfWeek].orEmpty()
            DailyOverrunStat(
                dayOfWeek = dayOfWeek,
                label = label,
                sessionCount = daySessions.size,
                overrunCount = daySessions.count { it.overrun }
            )
        }
    }

    private fun rate(count: Int, total: Int): Double {
        return if (total == 0) 0.0 else count.toDouble() / total.toDouble()
    }

    private const val PURPOSE_DRIFT = "PURPOSE_DRIFT"
    private val DAYS = listOf(
        Calendar.MONDAY to "월",
        Calendar.TUESDAY to "화",
        Calendar.WEDNESDAY to "수",
        Calendar.THURSDAY to "목",
        Calendar.FRIDAY to "금",
        Calendar.SATURDAY to "토",
        Calendar.SUNDAY to "일"
    )
}
