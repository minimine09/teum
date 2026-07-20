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

    fun todayStats(
        sessions: List<SessionLogEntity>,
        startOfTodayMillis: Long
    ): DashboardStats {
        val todaySessions = sessions.filter { it.startedAtMillis >= startOfTodayMillis }
        return DashboardStats(
            todaySessionCount = todaySessions.size,
            todayOverrunCount = todaySessions.count { it.overrun },
            todayFastReopenCount = todaySessions.count { it.isFastReopen },
            todayPurposeDriftCount = todaySessions.count { it.purposeDrifted == true }
        )
    }
}
