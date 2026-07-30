package com.teum.app.demo

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.teum.app.core.model.InterventionMode
import com.teum.app.dashboard.DashboardDataFilter
import com.teum.app.dashboard.DashboardDateRangeCalculator
import com.teum.app.dashboard.SessionGoalPolicy
import com.teum.app.dashboard.SessionMetricsResolver
import com.teum.app.dashboard.VulnerableTimeSelector
import com.teum.app.dashboard.VulnerabilityAnalyzer
import com.teum.app.dashboard.WeeklyReportAnalyzer
import com.teum.app.data.local.TeumDatabase
import com.teum.app.data.local.entity.ExtensionEventEntity
import com.teum.app.data.local.entity.ReopenLogEntity
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.data.repository.UserSettingsRepository
import java.util.Locale
import java.util.TimeZone

class RoomDemoDataSeeder(
    context: Context,
    private val timeZone: TimeZone = TimeZone.getDefault()
) : DemoDataSeeder {
    private val appContext = context.applicationContext
    private val database = TeumDatabase.getInstance(appContext)
    private val sessionLogDao = database.sessionLogDao()
    private val extensionEventDao = database.extensionEventDao()
    private val reopenLogDao = database.reopenLogDao()
    private val appOpenEventDao = database.appOpenEventDao()
    private val selfControlEventDao = database.selfControlEventDao()
    private val targetAppRepository = TargetAppRepository(appContext)
    private val userSettingsRepository = UserSettingsRepository(appContext)

    override suspend fun resetAndSeed(nowMillis: Long): DemoSeedResult =
        resetAndSeedInternal(nowMillis = nowMillis, failInsideTransaction = false)

    internal suspend fun resetAndSeedForTest(
        nowMillis: Long,
        failInsideTransaction: Boolean
    ): DemoSeedResult = resetAndSeedInternal(
        nowMillis = nowMillis,
        failInsideTransaction = failInsideTransaction
    )

    override suspend fun verifyCurrentSeed(nowMillis: Long): DemoSeedResult {
        return verifyFromDatabase(
            nowMillis = nowMillis,
            warnings = emptyList()
        )
    }

    private suspend fun resetAndSeedInternal(
        nowMillis: Long,
        failInsideTransaction: Boolean
    ): DemoSeedResult {
        if (!DemoDatasetFactory.canCreateTodayData(nowMillis, timeZone)) {
            return DemoSeedResult(
                success = false,
                generatedAtMillis = nowMillis,
                rowCounts = emptyMap(),
                expectedValues = DemoExpectedMetrics.expectedValues(),
                actualValues = emptyMap(),
                mismatches = listOf("seed.safeTime expected=enoughTodayWindow actual=tooEarly"),
                warnings = listOf(DemoDatasetFactory.safeStartWarning())
            )
        }

        val dataset = try {
            DemoDatasetFactory.create(nowMillis = nowMillis, timeZone = timeZone)
        } catch (exception: IllegalArgumentException) {
            return DemoSeedResult(
                success = false,
                generatedAtMillis = nowMillis,
                rowCounts = readRowCounts(),
                expectedValues = DemoExpectedMetrics.expectedValues(),
                actualValues = emptyMap(),
                mismatches = listOf("dataset.create failed=${exception.message.orEmpty()}"),
                warnings = listOf(DemoDatasetFactory.safeStartWarning())
            )
        }

        val warnings = mutableListOf<String>()
        try {
            database.withTransaction {
                extensionEventDao.deleteAllExtensionEventsForDebug()
                reopenLogDao.deleteAllReopenLogsForDebug()
                appOpenEventDao.deleteAllAppOpenEvents()
                selfControlEventDao.deleteAllSelfControlEvents()
                sessionLogDao.deleteAllSessionLogs()

                val idsByKey = linkedMapOf<String, Long>()
                dataset.sessions.forEach { seed ->
                    val sessionId = sessionLogDao.insertSessionLog(seed.entity)
                    idsByKey[seed.key] = sessionId
                    seed.extensionEvents.forEach { event ->
                        extensionEventDao.insertExtensionEvent(
                            ExtensionEventEntity(
                                sessionId = sessionId,
                                occurredAtMillis = event.occurredAtMillis,
                                extensionDurationMillis = event.extensionDurationMillis,
                                interventionActiveAtTime = event.interventionActiveAtTime
                            )
                        )
                    }
                }
                dataset.reopenLogs.forEach { seed ->
                    reopenLogDao.insertReopenLog(
                        ReopenLogEntity(
                            previousSessionId = idsByKey.getValue(seed.previousKey),
                            currentSessionId = idsByKey.getValue(seed.currentKey),
                            gapTimeMillis = seed.gapTimeMillis,
                            isFastReopen = seed.isFastReopen
                        )
                    )
                }
                dataset.appOpenEvents.forEach { event ->
                    appOpenEventDao.insertAppOpenEvent(event)
                }
                dataset.selfControlEvents.forEach { event ->
                    selfControlEventDao.insertSelfControlEvent(event)
                }

                if (failInsideTransaction) {
                    error("Forced demo seed rollback test")
                }
            }
        } catch (exception: RuntimeException) {
            Log.e(TAG, "demo seed transaction failed", exception)
            return verifyFromDatabase(
                nowMillis = nowMillis,
                warnings = listOf("Room transaction failed: ${exception.message.orEmpty()}")
            ).copy(success = false)
        }

        try {
            replaceTargetPackages()
            userSettingsRepository.setSetupCompleted(true)
            userSettingsRepository.setInterventionMode(InterventionMode.INTERVENTION)
            userSettingsRepository.setForceVulnerableNowForDebug(false)
        } catch (exception: RuntimeException) {
            warnings += "Room seed succeeded but settings update failed: ${exception.message.orEmpty()}"
        }

        return verifyFromDatabase(
            nowMillis = nowMillis,
            warnings = warnings
        )
    }

    private fun replaceTargetPackages() {
        val desired = DemoToolsContract.targetPackages
        val current = targetAppRepository.getTargetPackages()
        current.minus(desired).forEach(targetAppRepository::removeTargetPackage)
        desired.minus(current).forEach(targetAppRepository::addTargetPackage)
    }

    private suspend fun verifyFromDatabase(
        nowMillis: Long,
        warnings: List<String>
    ): DemoSeedResult {
        val sessions = sessionLogDao.getAllSessionLogsForDebug()
        val extensionEvents = extensionEventDao.getAllExtensionEventsForDebug()
        val reopenLogs = reopenLogDao.getAllReopenLogsForDebug()
        val openEvents = appOpenEventDao.getAllAppOpenEventsForDebug()
        val selfControlEvents = selfControlEventDao.getAllSelfControlEventsForDebug()
        val dateRange = DashboardDateRangeCalculator.calculate(
            nowMillis = nowMillis,
            timeZone = timeZone
        )
        val periodSessions = sessions.filter { session ->
            session.startedAtMillis < dateRange.startOfTomorrowMillis &&
                session.endedAtMillis > dateRange.startOfSevenDayPeriodMillis
        }
        val periodOpenEvents = openEvents.filter { event ->
            event.detectedAtMillis >= dateRange.startOfSevenDayPeriodMillis &&
                event.detectedAtMillis < dateRange.startOfTomorrowMillis
        }
        val periodSessionIds = periodSessions.mapTo(mutableSetOf()) { it.id }
        val periodExtensionEvents = extensionEvents.filter { it.sessionId in periodSessionIds }
        val periodReopenLogs = reopenLogs.filter { it.currentSessionId in periodSessionIds }
        val todayStats = DashboardDataFilter.todayStats(
            sessions = periodSessions,
            startOfTodayMillis = dateRange.startOfTodayMillis
        )
        val timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(
            sessions = periodSessions,
            openEvents = periodOpenEvents,
            extensionEvents = periodExtensionEvents,
            timeZone = timeZone,
            analysisStartMillis = dateRange.startOfSevenDayPeriodMillis,
            analysisEndMillis = dateRange.startOfTomorrowMillis
        )
        val weeklyStats = WeeklyReportAnalyzer.calculate(
            sessions = periodSessions,
            timeSlotStats = timeSlotStats,
            reopenLogs = periodReopenLogs,
            openEvents = periodOpenEvents
        )
        val topVulnerable = VulnerableTimeSelector.rankVulnerableSlots(timeSlotStats).firstOrNull()
        val hour22 = timeSlotStats.first { it.hourSlot == DemoExpectedMetrics.TOP_VULNERABLE_HOUR }
        val appUsageByPackage = weeklyStats.appUsageStats.associateBy { it.packageName }
        val rawFinalOverrunSessions = periodSessions.filter { it.rawOverrunMillis > 0L }

        val actualValues = linkedMapOf(
            "row.sessionLogs" to sessions.size.toString(),
            "row.appOpenEvents" to openEvents.size.toString(),
            "row.extensionEvents" to extensionEvents.size.toString(),
            "row.reopenLogs" to reopenLogs.size.toString(),
            "row.selfControlEvents" to selfControlEvents.size.toString(),
            "home.todaySessionCount" to todayStats.todaySessionCount.toString(),
            "home.todayUsageMillis" to todayStats.todayUsageMillis.toString(),
            "home.todayTargetKeptCount" to todayStats.todayTargetKeptCount.toString(),
            "home.todayOverrunCount" to todayStats.todayOverrunCount.toString(),
            "home.todayExtensionCount" to todayStats.todayExtensionCount.toString(),
            "home.todayFastReopenCount" to todayStats.todayFastReopenCount.toString(),
            "weekly.totalSessionCount" to weeklyStats.totalSessionCount.toString(),
            "weekly.totalUsageMillis" to periodSessions
                .sumOf { SessionMetricsResolver.resolve(it).usageMillis }
                .toString(),
            "weekly.overrunCount" to weeklyStats.overrunCount.toString(),
            "weekly.extensionCount" to weeklyStats.extensionCount.toString(),
            "weekly.fastReopenCount" to weeklyStats.fastReopenCount.toString(),
            "weekly.purposeDriftRate" to decimal(weeklyStats.purposeDriftRate),
            "weekly.necessaryUseCount" to weeklyStats.necessaryUseCount.toString(),
            "weekly.necessaryUseExcessMillis" to weeklyStats.necessaryUseExcessMillis.toString(),
            "weekly.outcomeResponseCount" to weeklyStats.outcomeResponseCount.toString(),
            "weekly.closedAfterInterventionCount" to weeklyStats.closedAfterInterventionCount.toString(),
            "weekly.interventionAppliedSessionCount" to weeklyStats.interventionAppliedSessionCount.toString(),
            "weekly.vulnerableTimeSessionCount" to weeklyStats.vulnerableTimeSessionCount.toString(),
            "app.youtubeUsageMillis" to appUsageByPackage
                .usageMillis(DemoToolsContract.YOUTUBE_PACKAGE),
            "app.instagramUsageMillis" to appUsageByPackage
                .usageMillis(DemoToolsContract.INSTAGRAM_PACKAGE),
            "app.chromeUsageMillis" to appUsageByPackage
                .usageMillis(DemoToolsContract.CHROME_PACKAGE),
            "internal.rawFinalOverrunCount" to rawFinalOverrunSessions.size.toString(),
            "internal.rawFinalOverrunMillis" to rawFinalOverrunSessions
                .sumOf { it.rawOverrunMillis }
                .toString(),
            "vulnerable.topHour" to (topVulnerable?.hourSlot?.toString() ?: "none"),
            "vulnerable.22.sessionCount" to hour22.sessionCount.toString(),
            "vulnerable.22.overrunCount" to hour22.overrunCount.toString(),
            "vulnerable.22.fastReopenCount" to hour22.fastReopenCount.toString(),
            "vulnerable.22.extensionCount" to hour22.extensionCount.toString(),
            "vulnerable.22.openCount" to hour22.openCount.toString(),
            "vulnerable.22.score" to decimal(hour22.vulnerabilityScore)
        )
        val expectedValues = DemoExpectedMetrics.expectedValues()
        val mismatches = expectedValues.mapNotNull { (key, expected) ->
            val actual = actualValues[key]
            if (actual == expected) {
                null
            } else {
                "$key expected=$expected actual=${actual ?: "missing"}"
            }
        }
        val rowCounts = readRowCounts()
        val result = DemoSeedResult(
            success = mismatches.isEmpty() && warnings.isEmpty(),
            generatedAtMillis = nowMillis,
            rowCounts = rowCounts,
            expectedValues = expectedValues,
            actualValues = actualValues,
            mismatches = mismatches,
            warnings = warnings
        )
        Log.i(TAG, "demo seed verification success=${result.success} mismatches=${mismatches.joinToString()}")
        return result
    }

    private suspend fun readRowCounts(): Map<String, Int> = linkedMapOf(
        "sessionLogs" to sessionLogDao.countSessionLogsForDebug(),
        "appOpenEvents" to appOpenEventDao.countAppOpenEventsForDebug(),
        "extensionEvents" to extensionEventDao.countExtensionEventsForDebug(),
        "reopenLogs" to reopenLogDao.countReopenLogsForDebug(),
        "selfControlEvents" to selfControlEventDao.countSelfControlEventsForDebug()
    )

    private fun Map<String, com.teum.app.dashboard.AppUsageStat>.usageMillis(
        packageName: String
    ): String = (this[packageName]?.usageMillis ?: 0L).toString()

    private fun decimal(value: Double): String = String.format(Locale.US, "%.4f", value)

    private companion object {
        const val TAG = "TeumDemoSeed"
    }
}
