package com.teum.app.dashboard

data class TimeSlotStat(
    val hourSlot: Int,
    val openCount: Int,
    val sessionCount: Int,
    val overrunCount: Int,
    val extensionCount: Int,
    val fastReopenCount: Int,
    val purposeDriftCount: Int,
    val purposeOutcomeResponseCount: Int,
    val overrunRate: Double,
    val fastReopenRate: Double,
    val purposeDriftRate: Double,
    val vulnerabilityScore: Double
) {
    val hasLowData: Boolean
        get() = sessionCount == 1
}
