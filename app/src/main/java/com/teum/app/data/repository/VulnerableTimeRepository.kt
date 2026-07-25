package com.teum.app.data.repository

import android.content.Context
import com.teum.app.dashboard.DashboardDateRangeCalculator
import com.teum.app.dashboard.VulnerableTimeAnalysis
import com.teum.app.dashboard.VulnerableTimeSelector
import com.teum.app.dashboard.VulnerabilityAnalyzer
import java.util.TimeZone
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class VulnerableTimeRepository(context: Context) {
    private val sessionLogRepository = SessionLogRepository(context)

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
            val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(
                sessions = sessions,
                openEvents = openEvents,
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
        return analyzeRecentSevenDays(
            nowMillis = nowMillis,
            timeZone = timeZone
        ).isVulnerableAt(
            nowMillis = nowMillis,
            timeZone = timeZone
        )
    }
}
