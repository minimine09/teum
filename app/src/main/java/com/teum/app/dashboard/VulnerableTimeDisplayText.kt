package com.teum.app.dashboard

object VulnerableTimeDisplayText {
    fun hourRange(hourSlot: Int): String {
        val start = hourSlot.mod(24).toString().padStart(2, '0')
        val end = (hourSlot + 1).mod(24).toString().padStart(2, '0')
        return "$start:00 - $end:00"
    }
}
