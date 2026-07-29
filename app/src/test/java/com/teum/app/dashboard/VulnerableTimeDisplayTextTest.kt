package com.teum.app.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class VulnerableTimeDisplayTextTest {
    @Test fun formatsOneHourSlotsAcrossMidnight() {
        assertEquals("22:00 - 23:00", VulnerableTimeDisplayText.hourRange(22))
        assertEquals("23:00 - 00:00", VulnerableTimeDisplayText.hourRange(23))
        assertEquals("00:00 - 01:00", VulnerableTimeDisplayText.hourRange(0))
    }
}
