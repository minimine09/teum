package com.teum.app.dashboard

import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.data.local.entity.ReopenLogEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class WeeklyReportAnalyzerTest {
    @Test fun emptyDataReturnsSafeDefaultsAndSevenDays() {
        val report = report(emptyList())
        assertEquals(0, report.totalSessionCount); assertEquals(0.0, report.overrunRate, 0.0)
        assertEquals(0.0, report.purposeDriftRate, 0.0); assertNull(report.averageReopenGapMillis)
        assertNull(report.mostVulnerableHourSlot); assertEquals(7, report.dailyOverrunStats.size)
    }

    @Test fun calculatesTotalsAverageGapAndWeekdayOverruns() {
        val report = report(listOf(
            session(Calendar.MONDAY, 9, overrun=true, extensions=2, gap=20_000),
            session(Calendar.MONDAY, 9, gap=40_000),
            session(Calendar.SATURDAY, 21, overrun=true, fast=true, gap=30_000)
        ))
        assertEquals(3, report.totalSessionCount); assertEquals(2, report.overrunCount)
        assertEquals(2.0/3, report.overrunRate, 1e-6); assertEquals(2, report.extensionCount)
        assertEquals(1, report.fastReopenCount); assertEquals(30_000L, report.averageReopenGapMillis)
        assertEquals(1, report.dailyOverrunStats.first { it.dayOfWeek==Calendar.MONDAY }.overrunCount)
        assertEquals(1, report.dailyOverrunStats.first { it.dayOfWeek==Calendar.SATURDAY }.overrunCount)
    }

    @Test fun purposeDriftRateUsesAllClearPurposeSessionsWhileResponsesStaySeparate() {
        val report = report(listOf(
            session(Calendar.TUESDAY,13,answered=true,drifted=true,closed=true),
            session(Calendar.TUESDAY,13,answered=true,drifted=false), session(Calendar.TUESDAY,13),
            session(Calendar.TUESDAY,13,intent="RECOGNIZED_BREAK",answered=true,drifted=true)
        ))
        assertEquals(2, report.outcomeResponseCount); assertEquals(1.0/3.0, report.purposeDriftRate, 0.0)
        assertEquals(1, report.closedAfterInterventionCount)
    }

    @Test fun vulnerableHourUsesScoreThenSessionCount() {
        val scoreWinner = report(listOf(session(Calendar.WEDNESDAY,8,overrun=true), session(Calendar.WEDNESDAY,20)))
        assertEquals(8, scoreWinner.mostVulnerableHourSlot)
        val countWinner = report(listOf(session(Calendar.WEDNESDAY,8),session(Calendar.WEDNESDAY,20),session(Calendar.THURSDAY,20)))
        assertEquals(20, countWinner.mostVulnerableHourSlot)
    }

    @Test fun necessaryUseSummaryCountsOnlyClearPurposeOutcomeExceptions() {
        val report = report(listOf(
            session(
                Calendar.THURSDAY,
                15,
                outcome = "NECESSARY_USE",
                necessaryUseExcessMillis = 30_000L
            ),
            session(
                Calendar.THURSDAY,
                15,
                intent = "MINDFUL_REST",
                outcome = "NECESSARY_USE",
                necessaryUseExcessMillis = 0L
            ),
            session(Calendar.THURSDAY, 15, outcome = "PURPOSE_DRIFT")
        ))
        assertEquals(1, report.necessaryUseCount)
        assertEquals(30_000L, report.necessaryUseExcessMillis)
    }

    @Test fun calculatesWeeklyChartBreakdowns() {
        val report = report(listOf(
            session(
                Calendar.MONDAY,
                9,
                extensions = 2,
                packageName = "youtube",
                durationMillis = 120_000L
            ),
            session(
                Calendar.MONDAY,
                10,
                extensions = 1,
                packageName = "youtube",
                durationMillis = 60_000L
            ),
            session(
                Calendar.TUESDAY,
                9,
                packageName = "chrome",
                durationMillis = 30_000L
            )
        ))

        val monday = report.dailyOverrunStats.first { it.dayOfWeek == Calendar.MONDAY }
        val tuesday = report.dailyOverrunStats.first { it.dayOfWeek == Calendar.TUESDAY }
        assertEquals(2, monday.openCount)
        assertEquals(3, monday.extensionCount)
        assertEquals(180_000L, monday.usageMillis)
        assertEquals(1, tuesday.openCount)
        assertEquals(30_000L, tuesday.usageMillis)
        assertEquals(listOf("youtube", "chrome"), report.appUsageStats.map { it.packageName })
        assertEquals(180_000L, report.appUsageStats.first { it.packageName == "youtube" }.usageMillis)
        assertEquals(30_000L, report.appUsageStats.first { it.packageName == "chrome" }.usageMillis)
    }

    @Test fun summarizesSavedInterventionPolicyState() {
        val report = report(listOf(
            session(
                Calendar.MONDAY,
                22,
                modeAtStart = "CAUTION",
                isVulnerableTimeAtStart = true,
                interventionAppliedAtStart = true
            ),
            session(
                Calendar.TUESDAY,
                12,
                modeAtStart = "CAUTION"
            ),
            session(
                Calendar.WEDNESDAY,
                22,
                modeAtStart = "NORMAL",
                isVulnerableTimeAtStart = true
            )
        ))

        assertEquals(2, report.cautionModeSessionCount)
        assertEquals(2, report.vulnerableTimeSessionCount)
        assertEquals(1, report.interventionAppliedSessionCount)
    }

    private fun report(s: List<SessionLogEntity>) = WeeklyReportAnalyzer.calculate(
        sessions = s,
        timeSlotStats = VulnerabilityAnalyzer.calculateTimeSlotStats(s),
        reopenLogs = s.mapIndexedNotNull { index, session ->
            session.reopenGapMillis?.let { gapTimeMillis ->
                ReopenLogEntity(
                    id = index + 1L,
                    previousSessionId = index + 1L,
                    currentSessionId = index + 2L,
                    gapTimeMillis = gapTimeMillis,
                    isFastReopen = session.isFastReopen
                )
            }
        }
    )
    private fun session(day:Int,hour:Int,overrun:Boolean=false,extensions:Int=0,fast:Boolean=false,
        gap:Long?=null,intent:String="CLEAR_PURPOSE",answered:Boolean=false,drifted:Boolean?=null,
        closed:Boolean?=null,outcome:String?=null,necessaryUseExcessMillis:Long=0L,
        packageName:String="target",durationMillis:Long=60_000L,
        modeAtStart:String?=null,isVulnerableTimeAtStart:Boolean=false,
        interventionAppliedAtStart:Boolean=false): SessionLogEntity {
        val start=time(day,hour)
        return SessionLogEntity(packageName=packageName,entryDetectedAtMillis=start,startedAtMillis=start,
            endedAtMillis=start+durationMillis,durationMillis=durationMillis,targetDurationMillis=60_000,intentChoice=intent,
            modeAtStart=modeAtStart,isVulnerableTimeAtStart=isVulnerableTimeAtStart,
            interventionAppliedAtStart=interventionAppliedAtStart,
            outcomeType=outcome,outcomeRespondedAtMillis=if(answered) start+61_000 else null,purposeDrifted=drifted,
            closedAfterIntervention=closed,overrun=overrun,extensionCount=extensions,isFastReopen=fast,
            reopenGapMillis=gap,necessaryUseExcessMillis=necessaryUseExcessMillis,createdAtMillis=start)
    }
    private fun time(day:Int,hour:Int)=Calendar.getInstance().apply {
        clear(); set(2026,Calendar.MAY,11 + ((day - Calendar.MONDAY + 7) % 7),hour,0)
    }.timeInMillis
}


