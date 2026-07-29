package com.teum.app.dashboard

import com.teum.app.core.model.InterventionMode
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.ReopenLogEntity
import com.teum.app.data.local.entity.SessionLogEntity
import java.util.Calendar

object WeeklyReportAnalyzer {
    fun calculate(
        sessions: List<SessionLogEntity>,
        timeSlotStats: List<TimeSlotStat>,
        reopenLogs: List<ReopenLogEntity>,
        openEvents: List<AppOpenEventEntity> = emptyList()
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
        val totalReopenGapMillis = reopenLogs.sumOf { it.gapTimeMillis }
        val mostVulnerableTimeSlotStat = VulnerableTimeSelector.rankVulnerableSlots(
            timeSlotStats = timeSlotStats
        ).firstOrNull()

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
            averageReopenGapMillis = if (totalSessionCount == 0) {
                null
            } else {
                totalReopenGapMillis / totalSessionCount
            },
            mostVulnerableTimeSlotStat = mostVulnerableTimeSlotStat,
            hasEnoughVulnerableTimeData = timeSlotStats.any { stat ->
                stat.sessionCount >= VulnerableTimeSelector.DEFAULT_MINIMUM_SESSION_COUNT
            },
            cautionModeSessionCount = sessions.count {
                it.modeAtStart == InterventionMode.INTERVENTION.name
            },
            vulnerableTimeSessionCount = sessions.count { it.isVulnerableTimeAtStart },
            interventionAppliedSessionCount = sessions.count { it.interventionAppliedAtStart },
            dailyOverrunStats = calculateDailyOverrunStats(
                sessions = sessions,
                openEvents = openEvents
            ),
            appUsageStats = calculateAppUsageStats(sessions)
        )
    }

    private fun calculateDailyOverrunStats(
        sessions: List<SessionLogEntity>,
        openEvents: List<AppOpenEventEntity>
    ): List<DailyOverrunStat> {
        val sessionsByDay = sessions.groupBy { session ->
            Calendar.getInstance().apply {
                timeInMillis = session.startedAtMillis
            }.get(Calendar.DAY_OF_WEEK)
        }
        val openEventsByDay = openEvents.groupBy { event ->
            Calendar.getInstance().apply {
                timeInMillis = event.detectedAtMillis
            }.get(Calendar.DAY_OF_WEEK)
        }

        return DAYS.map { (dayOfWeek, label) ->
            val daySessions = sessionsByDay[dayOfWeek].orEmpty()
            DailyOverrunStat(
                dayOfWeek = dayOfWeek,
                label = label,
                sessionCount = daySessions.size,
                overrunCount = daySessions.count { it.overrun },
                openCount = openEventsByDay[dayOfWeek].orEmpty().size,
                extensionCount = daySessions.sumOf { it.extensionCount },
                usageMillis = daySessions.sumOf { SessionMetricsResolver.resolve(it).usageMillis }
            )
        }
    }

    private fun calculateAppUsageStats(
        sessions: List<SessionLogEntity>
    ): List<AppUsageStat> {
        return sessions
            .groupBy { it.packageName }
            .map { (packageName, appSessions) ->
                AppUsageStat(
                    packageName = packageName,
                    usageMillis = appSessions.sumOf {
                        SessionMetricsResolver.resolve(it).usageMillis
                    },
                    appDisplayName = appSessions
                        .asSequence()
                        .sortedByDescending { it.endedAtMillis }
                        .mapNotNull { it.appDisplayName?.takeIf(String::isNotBlank) }
                        .firstOrNull()
                )
            }
            .filter { it.usageMillis > 0L }
            .sortedByDescending { it.usageMillis }
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
