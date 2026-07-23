package com.teum.app.dashboard

import com.teum.app.data.local.entity.ReopenLogEntity
import com.teum.app.data.local.entity.SessionLogEntity
import java.util.Calendar

object WeeklyReportAnalyzer {
    fun calculate(
        sessions: List<SessionLogEntity>,
        timeSlotStats: List<TimeSlotStat>,
        reopenLogs: List<ReopenLogEntity>
    ): WeeklyReportStats {
        val totalSessionCount = sessions.size
        val overrunCount = sessions.count { it.overrun }
        val clearPurposeSessions = sessions.filter { session ->
            session.intentChoice == CLEAR_PURPOSE
        }
        val purposeOutcomeSessions = clearPurposeSessions.filter { session ->
            session.outcomeRespondedAtMillis != null
        }
        val purposeDriftCount = clearPurposeSessions.count { it.purposeDrifted == true }
        val necessaryUseSessions = clearPurposeSessions.filter { session ->
            session.outcomeType == NECESSARY_USE
        }
        val reopenGaps = reopenLogs.map { it.gapTimeMillis }
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
            fastReopenCount = reopenLogs.count { it.isFastReopen },
            outcomeResponseCount = purposeOutcomeSessions.size,
            purposeDriftRate = rate(purposeDriftCount, clearPurposeSessions.size),
            necessaryUseCount = necessaryUseSessions.size,
            necessaryUseExcessMillis = necessaryUseSessions.sumOf {
                it.necessaryUseExcessMillis
            },
            closedAfterInterventionCount = sessions.count { it.closedAfterIntervention == true },
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

    private const val CLEAR_PURPOSE = "CLEAR_PURPOSE"
    private const val NECESSARY_USE = "NECESSARY_USE"
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
