package com.teum.app.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoExpectedMetricsTest {
    @Test
    fun expectedValuesKeepUserGoalAndRawOverrunSeparate() {
        val expected = DemoExpectedMetrics.expectedValues()

        assertEquals("5", expected.getValue("weekly.overrunCount"))
        assertEquals("9", expected.getValue("internal.rawFinalOverrunCount"))
        assertEquals("0.3846", expected.getValue("weekly.purposeDriftRate"))
        assertEquals("22", expected.getValue("vulnerable.topHour"))
        assertEquals("0.6167", expected.getValue("vulnerable.22.score"))
    }
}
