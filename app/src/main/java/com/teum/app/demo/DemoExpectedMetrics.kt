package com.teum.app.demo

import java.util.Locale

object DemoExpectedMetrics {
    const val SESSION_COUNT = 17
    const val APP_OPEN_COUNT = 20
    const val EXTENSION_EVENT_COUNT = 6
    const val REOPEN_LOG_COUNT = 3
    const val SELF_CONTROL_EVENT_COUNT = 3

    const val TODAY_SESSION_COUNT = 3
    const val TODAY_USAGE_MINUTES = 15
    const val TODAY_TARGET_KEPT_COUNT = 2
    const val TODAY_INITIAL_GOAL_EXCEEDED_COUNT = 1
    const val TODAY_EXTENSION_COUNT = 1
    const val TODAY_FAST_REOPEN_COUNT = 1

    const val WEEKLY_TOTAL_SESSION_COUNT = 17
    const val WEEKLY_TOTAL_USAGE_MINUTES = 98
    const val INITIAL_GOAL_EXCEEDED_COUNT = 5
    const val INITIAL_GOAL_KEPT_COUNT = 12
    const val RAW_FINAL_OVERRUN_COUNT = 9
    const val RAW_FINAL_OVERRUN_MINUTES = 14
    const val WEEKLY_EXTENSION_COUNT = 6
    const val WEEKLY_FAST_REOPEN_COUNT = 3
    const val CLEAR_PURPOSE_SESSION_COUNT = 13
    const val OUTCOME_RESPONSE_COUNT = 12
    const val PURPOSE_DRIFT_COUNT = 5
    const val NECESSARY_USE_COUNT = 3
    const val NECESSARY_USE_EXCESS_MINUTES = 4
    const val CLOSED_AFTER_INTERVENTION_COUNT = 3
    const val INTERVENTION_APPLIED_SESSION_COUNT = 6
    const val VULNERABLE_TIME_SESSION_COUNT = 5

    const val YOUTUBE_USAGE_MINUTES = 44
    const val INSTAGRAM_USAGE_MINUTES = 41
    const val CHROME_USAGE_MINUTES = 13

    const val TOP_VULNERABLE_HOUR = 22
    const val VULNERABLE_22_SESSION_COUNT = 6
    const val VULNERABLE_22_OVERRUN_COUNT = 4
    const val VULNERABLE_22_FAST_REOPEN_COUNT = 2
    const val VULNERABLE_22_EXTENSION_COUNT = 5
    const val VULNERABLE_22_OPEN_COUNT = 7
    const val VULNERABLE_22_SCORE_TEXT = "0.6167"

    fun expectedValues(): Map<String, String> = linkedMapOf(
        "row.sessionLogs" to SESSION_COUNT.toString(),
        "row.appOpenEvents" to APP_OPEN_COUNT.toString(),
        "row.extensionEvents" to EXTENSION_EVENT_COUNT.toString(),
        "row.reopenLogs" to REOPEN_LOG_COUNT.toString(),
        "row.selfControlEvents" to SELF_CONTROL_EVENT_COUNT.toString(),
        "home.todaySessionCount" to TODAY_SESSION_COUNT.toString(),
        "home.todayUsageMillis" to minutes(TODAY_USAGE_MINUTES),
        "home.todayTargetKeptCount" to TODAY_TARGET_KEPT_COUNT.toString(),
        "home.todayOverrunCount" to TODAY_INITIAL_GOAL_EXCEEDED_COUNT.toString(),
        "home.todayExtensionCount" to TODAY_EXTENSION_COUNT.toString(),
        "home.todayFastReopenCount" to TODAY_FAST_REOPEN_COUNT.toString(),
        "weekly.totalSessionCount" to WEEKLY_TOTAL_SESSION_COUNT.toString(),
        "weekly.totalUsageMillis" to minutes(WEEKLY_TOTAL_USAGE_MINUTES),
        "weekly.overrunCount" to INITIAL_GOAL_EXCEEDED_COUNT.toString(),
        "weekly.extensionCount" to WEEKLY_EXTENSION_COUNT.toString(),
        "weekly.fastReopenCount" to WEEKLY_FAST_REOPEN_COUNT.toString(),
        "weekly.purposeDriftRate" to ratio(PURPOSE_DRIFT_COUNT, CLEAR_PURPOSE_SESSION_COUNT),
        "weekly.necessaryUseCount" to NECESSARY_USE_COUNT.toString(),
        "weekly.necessaryUseExcessMillis" to minutes(NECESSARY_USE_EXCESS_MINUTES),
        "weekly.outcomeResponseCount" to OUTCOME_RESPONSE_COUNT.toString(),
        "weekly.closedAfterInterventionCount" to CLOSED_AFTER_INTERVENTION_COUNT.toString(),
        "weekly.interventionAppliedSessionCount" to INTERVENTION_APPLIED_SESSION_COUNT.toString(),
        "weekly.vulnerableTimeSessionCount" to VULNERABLE_TIME_SESSION_COUNT.toString(),
        "app.youtubeUsageMillis" to minutes(YOUTUBE_USAGE_MINUTES),
        "app.instagramUsageMillis" to minutes(INSTAGRAM_USAGE_MINUTES),
        "app.chromeUsageMillis" to minutes(CHROME_USAGE_MINUTES),
        "internal.rawFinalOverrunCount" to RAW_FINAL_OVERRUN_COUNT.toString(),
        "internal.rawFinalOverrunMillis" to minutes(RAW_FINAL_OVERRUN_MINUTES),
        "vulnerable.topHour" to TOP_VULNERABLE_HOUR.toString(),
        "vulnerable.22.sessionCount" to VULNERABLE_22_SESSION_COUNT.toString(),
        "vulnerable.22.overrunCount" to VULNERABLE_22_OVERRUN_COUNT.toString(),
        "vulnerable.22.fastReopenCount" to VULNERABLE_22_FAST_REOPEN_COUNT.toString(),
        "vulnerable.22.extensionCount" to VULNERABLE_22_EXTENSION_COUNT.toString(),
        "vulnerable.22.openCount" to VULNERABLE_22_OPEN_COUNT.toString(),
        "vulnerable.22.score" to VULNERABLE_22_SCORE_TEXT
    )

    private fun minutes(value: Int): String = (value * MILLIS_PER_MINUTE).toString()

    private fun ratio(count: Int, total: Int): String =
        if (total == 0) {
            "0.0000"
        } else {
            String.format(Locale.US, "%.4f", count.toDouble() / total.toDouble())
        }

    private const val MILLIS_PER_MINUTE = 60_000L
}
