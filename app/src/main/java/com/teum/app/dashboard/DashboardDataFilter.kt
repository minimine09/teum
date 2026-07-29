package com.teum.app.dashboard

import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.SessionLogEntity

object DashboardDataFilter {
    fun sessions(
        sessions: List<SessionLogEntity>,
        selectedPackageName: String?
    ): List<SessionLogEntity> = selectedPackageName?.let { selected ->
        sessions.filter { it.packageName == selected }
    } ?: sessions

    fun openEvents(
        openEvents: List<AppOpenEventEntity>,
        selectedPackageName: String?
    ): List<AppOpenEventEntity> = selectedPackageName?.let { selected ->
        openEvents.filter { it.packageName == selected }
    } ?: openEvents

    fun recentSessions(
        allSessions: List<SessionLogEntity>,
        selectedPackageName: String?,
        limit: Int = 10
    ): List<SessionLogEntity> = sessions(allSessions, selectedPackageName)
        .sortedByDescending { it.endedAtMillis }
        .take(limit)

    fun todayStats(
        sessions: List<SessionLogEntity>,
        startOfTodayMillis: Long
    ): DashboardStats {
        val todaySessions = sessions.filter { it.startedAtMillis >= startOfTodayMillis }
        val todaySessionMetrics = todaySessions.associateWith(SessionMetricsResolver::resolve)
        val todayAppUsageStats = todaySessions
            .groupBy { it.packageName }
            .map { (packageName, appSessions) ->
                AppUsageStat(
                    packageName = packageName,
                    usageMillis = appSessions.sumOf { session ->
                        todaySessionMetrics.getValue(session).usageMillis
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
            .take(5)

        return DashboardStats(
            todaySessionCount = todaySessions.size,
            todayOverrunCount = todaySessions.count { it.overrun },
            todayFastReopenCount = todaySessions.count { it.isFastReopen },
            todayTargetKeptCount = todaySessions.count { session ->
                todaySessionMetrics.getValue(session).isOverrun.not()
            },
            todayExtensionCount = todaySessions.sumOf { it.extensionCount },
            todayUsageMillis = todaySessionMetrics.values.sumOf { it.usageMillis },
            todayAppUsageStats = todayAppUsageStats,
            todayPurposeKeptCount = todaySessions.count { session ->
                session.intentChoice == CLEAR_PURPOSE &&
                    session.outcomeType in PURPOSE_KEPT_OUTCOMES
            },
            todayPurposeDriftCount = todaySessions.count { it.purposeDrifted == true },
            todayClosedAfterInterventionCount =
                todaySessions.count { it.closedAfterIntervention == true }
        )
    }

    private const val CLEAR_PURPOSE = "CLEAR_PURPOSE"
    private val PURPOSE_KEPT_OUTCOMES = setOf(
        "PURPOSE_ACHIEVED",
        "NECESSARY_USE"
    )
}
