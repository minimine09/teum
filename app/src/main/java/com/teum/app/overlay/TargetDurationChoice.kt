package com.teum.app.overlay

enum class TargetDurationChoice(
    val label: String,
    val durationMillis: Long
) {
    // Demo-only shortcut for quickly testing Session Brake timing.
    TEST_FIVE_SECONDS("5\uCD08", 5_000L),
    ONE_MINUTE("1\uBD84", 60_000L),
    THREE_MINUTES("3\uBD84", 180_000L),
    FIVE_MINUTES("5\uBD84", 300_000L),
    TEN_MINUTES("10\uBD84", 600_000L),
    FIFTEEN_MINUTES("15\uBD84", 900_000L),
    THIRTY_MINUTES("30\uBD84", 1_800_000L),
    ONE_HOUR("1\uC2DC\uAC04", 3_600_000L)
}
