package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity

data class DashboardUiState(
    val dashboardStats: DashboardStats = DashboardStats(),
    val recentSessions: List<SessionLogEntity> = emptyList(),
    val timeSlotStats: List<TimeSlotStat> = emptyList(),
    val weeklyReportStats: WeeklyReportStats = WeeklyReportStats(),
    val availablePackages: Set<String> = emptySet(),
    val selectedPackageName: String? = null
)
