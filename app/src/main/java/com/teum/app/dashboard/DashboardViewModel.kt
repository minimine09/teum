package com.teum.app.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teum.app.data.repository.SessionLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SessionLogRepository(application)

    private fun observeTodayStats(startOfTodayMillis: Long): Flow<DashboardStats> = combine(
        repository.observeTodaySessionCount(startOfTodayMillis),
        repository.observeTodayOverrunCount(startOfTodayMillis),
        repository.observeTodayFastReopenCount(startOfTodayMillis),
        repository.observeTodayPurposeDriftCount(startOfTodayMillis)
    ) { sessionCount, overrunCount, fastReopenCount, purposeDriftCount ->
        DashboardStats(
            todaySessionCount = sessionCount,
            todayOverrunCount = overrunCount,
            todayFastReopenCount = fastReopenCount,
            todayPurposeDriftCount = purposeDriftCount
        )
    }

    private val dateRange = flow {
        while (currentCoroutineContext().isActive) {
            val range = DashboardDateRangeCalculator.calculate()
            emit(range)
            delay((range.startOfTomorrowMillis - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }

    val uiState: StateFlow<DashboardUiState> = dateRange.flatMapLatest { range ->
        combine(
            observeTodayStats(range.startOfTodayMillis),
            repository.observeRecentSessions(),
            repository.observeSessionsSince(range.startOfSevenDayPeriodMillis),
            repository.observeOpenEventsSince(range.startOfSevenDayPeriodMillis)
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
        }
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
