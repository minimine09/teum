package com.teum.app.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.data.repository.SessionLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val selectedPackageName = MutableStateFlow<String?>(null)

    private val dateRange = flow {
        while (currentCoroutineContext().isActive) {
            val range = DashboardDateRangeCalculator.calculate()
            emit(range)
            delay((range.startOfTomorrowMillis - System.currentTimeMillis()).coerceAtLeast(1_000L))
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        dateRange,
        selectedPackageName
    ) { range, selectedPackage ->
        range to selectedPackage
    }.flatMapLatest { (range, selectedPackage) ->
        combine(
            repository.observeSessionsSince(range.startOfSevenDayPeriodMillis),
            repository.observeOpenEventsSince(range.startOfSevenDayPeriodMillis),
            repository.observeReopenLogsSince(
                sinceMillis = range.startOfSevenDayPeriodMillis,
                packageName = selectedPackage
            )
        ) { allSessions, allOpenEvents, reopenLogs ->
            val sessions = DashboardDataFilter.sessions(allSessions, selectedPackage)
            val openEvents = DashboardDataFilter.openEvents(allOpenEvents, selectedPackage)
            val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(
                sessions = sessions,
                openEvents = openEvents
            )

            DashboardUiState(
                dashboardStats = DashboardDataFilter.todayStats(sessions, range.startOfTodayMillis),
                recentSessions = sessions.sortedByDescending { it.endedAtMillis }.take(10),
                timeSlotStats = timeSlotStats,
                weeklyReportStats = WeeklyReportAnalyzer.calculate(
                    sessions = sessions,
                    timeSlotStats = timeSlotStats,
                    reopenLogs = reopenLogs
                ),
                availablePackages = (allSessions.map { it.packageName } +
                    allOpenEvents.map { it.packageName }).toSet(),
                selectedPackageName = selectedPackage
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = DashboardUiState()
    )

    fun selectPackage(packageName: String?) {
        selectedPackageName.value = packageName
    }

    fun deleteAllSessionLogs() {
        viewModelScope.launch {
            repository.deleteAllSessionLogs()
        }
    }

}
