package com.teum.app.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.data.repository.VulnerableTimePolicyDataFilter
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
    private val targetAppRepository = TargetAppRepository(application)
    private val selectedPackageName = MutableStateFlow<String?>(null)

    val sessionHistorySessions: StateFlow<List<SessionLogEntity>> =
        repository.observeRecentSessions(Int.MAX_VALUE)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
                initialValue = emptyList()
            )

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
            repository.observeSessionsOverlappingPeriod(
                sinceMillis = range.startOfSevenDayPeriodMillis,
                untilMillis = range.startOfTomorrowMillis
            ),
            repository.observeOpenEventsSince(range.startOfSevenDayPeriodMillis),
            repository.observeReopenLogsSince(
                sinceMillis = range.startOfSevenDayPeriodMillis,
                packageName = null
            ),
            repository.observeExtensionEventsSince(range.startOfSevenDayPeriodMillis)
        ) { allSessions, policySessionCandidates, allOpenEvents, reopenLogs, extensionEvents ->
            val targetPackages = targetAppRepository.getTargetPackages()
            val policySessions = VulnerableTimePolicyDataFilter.sessions(
                sessions = policySessionCandidates,
                targetPackages = targetPackages
            )
            val policyOpenEvents = VulnerableTimePolicyDataFilter.openEvents(
                openEvents = allOpenEvents,
                targetPackages = targetPackages
            )
            val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(
                sessions = policySessions,
                openEvents = policyOpenEvents,
                extensionEvents = extensionEvents,
                analysisStartMillis = range.startOfSevenDayPeriodMillis,
                analysisEndMillis = range.startOfTomorrowMillis
            )

            DashboardUiState(
                dashboardStats = DashboardDataFilter.todayStats(
                    allSessions,
                    range.startOfTodayMillis
                ),
                recentSessions = DashboardDataFilter.recentSessions(
                    allSessions = allSessions,
                    selectedPackageName = null
                ),
                sessionRecentSessions = DashboardDataFilter.recentSessions(
                    allSessions = allSessions,
                    selectedPackageName = selectedPackage
                ),
                timeSlotStats = timeSlotStats,
                weeklyReportStats = WeeklyReportAnalyzer.calculate(
                    sessions = allSessions,
                    timeSlotStats = timeSlotStats,
                    reopenLogs = reopenLogs,
                    openEvents = allOpenEvents
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
