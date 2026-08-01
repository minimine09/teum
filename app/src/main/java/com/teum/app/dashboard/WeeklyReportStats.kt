package com.teum.app.dashboard

data class DailyOverrunStat(
    val dayOfWeek: Int,
    val label: String,
    val sessionCount: Int,
    val overrunCount: Int,
    val openCount: Int = sessionCount,
    val extensionCount: Int = 0,
    val usageMillis: Long = 0L
)

data class AppUsageStat(
    val packageName: String,
    val usageMillis: Long,
    val appDisplayName: String? = null
)

data class IntentChoiceReportStat(
    val count: Int = 0,
    val rate: Double = 0.0
)

data class WeeklyReportStats(
    val totalSessionCount: Int = 0,
    val overrunCount: Int = 0,
    val overrunRate: Double = 0.0,
    val extensionCount: Int = 0,
    val fastReopenCount: Int = 0,
    val outcomeResponseCount: Int = 0,
    val purposeDriftRate: Double = 0.0,
    val purposeAchievedOutcomeCount: Int = 0,
    val necessaryUseCount: Int = 0,
    val necessaryUseOutcomeCount: Int = 0,
    val purposeDriftOutcomeCount: Int = 0,
    val unconsciousUseOutcomeCount: Int = 0,
    val necessaryUseExcessMillis: Long = 0L,
    val closedAfterInterventionCount: Int = 0,
    val averageReopenGapMillis: Long? = null,
    val mostVulnerableTimeSlotStat: TimeSlotStat? = null,
    val hasEnoughVulnerableTimeData: Boolean = false,
    val cautionModeSessionCount: Int = 0,
    val vulnerableTimeSessionCount: Int = 0,
    val interventionAppliedSessionCount: Int = 0,
    val clearPurposeIntentStat: IntentChoiceReportStat = IntentChoiceReportStat(),
    val mindfulRestIntentStat: IntentChoiceReportStat = IntentChoiceReportStat(),
    val unconsciousOpenIntentStat: IntentChoiceReportStat = IntentChoiceReportStat(),
    val dailyOverrunStats: List<DailyOverrunStat> = emptyList(),
    val appUsageStats: List<AppUsageStat> = emptyList()
) {
    val mostVulnerableHourSlot: Int?
        get() = mostVulnerableTimeSlotStat?.hourSlot
}
