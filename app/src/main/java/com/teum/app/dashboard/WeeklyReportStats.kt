package com.teum.app.dashboard

data class DailyOverrunStat(
    val dayOfWeek: Int,
    val label: String,
    val sessionCount: Int,
    val overrunCount: Int
)

data class WeeklyReportStats(
    val totalSessionCount: Int = 0,
    val overrunCount: Int = 0,
    val overrunRate: Double = 0.0,
    val extensionCount: Int = 0,
    val fastReopenCount: Int = 0,
    val outcomeResponseCount: Int = 0,
    val purposeDriftRate: Double = 0.0,
    val closedAfterInterventionCount: Int = 0,
    val averageReopenGapMillis: Long? = null,
    val mostVulnerableHourSlot: Int? = null,
    val dailyOverrunStats: List<DailyOverrunStat> = emptyList()
)
