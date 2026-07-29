package com.teum.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import com.teum.app.dashboard.DashboardDateRangeCalculator
import com.teum.app.dashboard.VulnerableTimeAnalysis
import com.teum.app.dashboard.VulnerableTimeSelector
import com.teum.app.dashboard.VulnerabilityAnalyzer
import com.teum.app.debug.TeumLogger
import java.util.TimeZone
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class VulnerableTimeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sessionLogRepository = SessionLogRepository(context)
    private val targetAppRepository = TargetAppRepository(context)
    private val userSettingsRepository = UserSettingsRepository(context)

    suspend fun analyzeRecentSevenDays(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): VulnerableTimeAnalysis {
        val dateRange = DashboardDateRangeCalculator.calculate(
            nowMillis = nowMillis,
            timeZone = timeZone
        )
        return combine(
            sessionLogRepository.observeSessionsSince(dateRange.startOfSevenDayPeriodMillis),
            sessionLogRepository.observeOpenEventsSince(dateRange.startOfSevenDayPeriodMillis)
        ) { sessions, openEvents ->
            val targetPackages = targetAppRepository.getTargetPackages()
            val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(
                sessions = VulnerableTimePolicyDataFilter.sessions(
                    sessions = sessions,
                    targetPackages = targetPackages
                ),
                openEvents = VulnerableTimePolicyDataFilter.openEvents(
                    openEvents = openEvents,
                    targetPackages = targetPackages
                ),
                timeZone = timeZone
            )
            VulnerableTimeSelector.select(
                timeSlotStats = timeSlotStats,
                analyzedAtMillis = nowMillis
            )
        }.first()
    }

    suspend fun isVulnerableNow(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (isDebuggableBuild() && userSettingsRepository.getForceVulnerableNowForDebug()) {
            TeumLogger.flow("[POLICY] VULNERABLE_DEBUG_OVERRIDE enabled=true")
            TeumLogger.flow("[POLICY] VULNERABLE_NOW result=true source=debug_override")
            return true
        }

        val result = analyzeRecentSevenDays(
            nowMillis = nowMillis,
            timeZone = timeZone
        ).isVulnerableAt(
            nowMillis = nowMillis,
            timeZone = timeZone
        )
        TeumLogger.flow("[POLICY] VULNERABLE_NOW result=$result source=analysis")
        return result
    }

    private fun isDebuggableBuild(): Boolean {
        return (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
