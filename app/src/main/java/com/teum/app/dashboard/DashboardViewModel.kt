package com.teum.app.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teum.app.data.repository.SessionLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionLogRepository(application)

    private val todayStats = combine(
        repository.observeTodaySessionCount(),
        repository.observeTodayOverrunCount(),
        repository.observeTodayFastReopenCount(),
        repository.observeTodayPurposeDriftCount()
    ) { sessionCount, overrunCount, fastReopenCount, purposeDriftCount ->
        DashboardStats(
            todaySessionCount = sessionCount,
            todayOverrunCount = overrunCount,
            todayFastReopenCount = fastReopenCount,
            todayPurposeDriftCount = purposeDriftCount
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        todayStats,
        repository.observeRecentSessions(),
        repository.observeSessionsForLastSevenDays(),
        repository.observeOpenEventsForLastSevenDays()
    ) { dashboardStats, recentSessions, lastSevenDaysSessions, openEvents ->
        val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(
            sessions = lastSevenDaysSessions,
            openEvents = openEvents
        )
        DashboardUiState(
            dashboardStats = dashboardStats,
            recentSessions = recentSessions,
            timeSlotStats = timeSlotStats,
            weeklyReportStats = WeeklyReportAnalyzer.calculate(
                sessions = lastSevenDaysSessions,
                timeSlotStats = timeSlotStats
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = DashboardUiState()
    )

    fun deleteAllSessionLogs() {
        viewModelScope.launch {
            repository.deleteAllSessionLogs()
        }
    }
}
