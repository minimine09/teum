package com.teum.app.dashboard

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VulnerableTimeSelectorTest {
    @Test
    fun noSlotWithTwoSessionsReturnsInsufficientData() {
        val analysis = VulnerableTimeSelector.select(
            timeSlotStats = listOf(
                stat(hourSlot = 9, sessionCount = 1, vulnerabilityScore = 1.0),
                stat(hourSlot = 22, sessionCount = 0, vulnerabilityScore = 1.0)
            ),
            analyzedAtMillis = 100L
        )

        assertFalse(analysis.hasEnoughData)
        assertTrue(analysis.vulnerableHourSlots.isEmpty())
    }

    @Test
    fun enoughDataWithoutRiskDoesNotInventVulnerableTime() {
        val analysis = VulnerableTimeSelector.select(
            timeSlotStats = listOf(
                stat(hourSlot = 9, sessionCount = 2, vulnerabilityScore = 0.0)
            ),
            analyzedAtMillis = 100L
        )

        assertTrue(analysis.hasEnoughData)
        assertTrue(analysis.vulnerableHourSlots.isEmpty())
    }

    @Test
    fun selectsAtMostTwoHighestScoringAnalyzableSlots() {
        val stats = listOf(
            stat(hourSlot = 9, sessionCount = 4, vulnerabilityScore = 0.30),
            stat(hourSlot = 18, sessionCount = 2, vulnerabilityScore = 0.80),
            stat(hourSlot = 22, sessionCount = 3, vulnerabilityScore = 0.80),
            stat(hourSlot = 23, sessionCount = 1, vulnerabilityScore = 1.0)
        )
        val analysis = VulnerableTimeSelector.select(
            timeSlotStats = stats,
            analyzedAtMillis = 100L
        )
        val reportTopSlot = VulnerableTimeSelector.rankVulnerableSlots(
            timeSlotStats = stats,
            maximumSlotCount = 1
        ).single()

        assertTrue(analysis.hasEnoughData)
        assertEquals(linkedSetOf(22, 18), analysis.vulnerableHourSlots)
        assertEquals(analysis.vulnerableHourSlots.first(), reportTopSlot.hourSlot)
    }

    @Test
    fun vulnerableCheckUsesHourSlotAndHandlesMidnightBoundary() {
        val utc = TimeZone.getTimeZone("UTC")
        val analysis = VulnerableTimeAnalysis(
            hasEnoughData = true,
            vulnerableHourSlots = setOf(23, 0),
            timeSlotStats = emptyList(),
            analyzedAtMillis = 100L
        )

        assertTrue(analysis.isVulnerableAt(timeAt(hour = 23, minute = 59, utc), utc))
        assertTrue(analysis.isVulnerableAt(timeAt(hour = 0, minute = 0, utc), utc))
        assertFalse(analysis.isVulnerableAt(timeAt(hour = 1, minute = 0, utc), utc))
    }

    @Test
    fun insufficientAnalysisNeverActivatesPolicy() {
        val utc = TimeZone.getTimeZone("UTC")
        val analysis = VulnerableTimeAnalysis(
            hasEnoughData = false,
            vulnerableHourSlots = setOf(22),
            timeSlotStats = emptyList(),
            analyzedAtMillis = 100L
        )

        assertFalse(analysis.isVulnerableAt(timeAt(hour = 22, minute = 0, utc), utc))
    }

    private fun stat(
        hourSlot: Int,
        sessionCount: Int,
        vulnerabilityScore: Double
    ) = TimeSlotStat(
        hourSlot = hourSlot,
        openCount = 0,
        sessionCount = sessionCount,
        overrunCount = 0,
        extensionCount = 0,
        fastReopenCount = 0,
        purposeDriftCount = 0,
        purposeOutcomeResponseCount = 0,
        overrunRate = 0.0,
        fastReopenRate = 0.0,
        extensionScore = 0.0,
        openScore = 0.0,
        purposeDriftRate = 0.0,
        vulnerabilityScore = vulnerabilityScore
    )

    private fun timeAt(
        hour: Int,
        minute: Int,
        timeZone: TimeZone
    ): Long = Calendar.getInstance(timeZone).apply {
        clear()
        set(2026, Calendar.JULY, 25, hour, minute)
    }.timeInMillis
}
