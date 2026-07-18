package com.teum.app.overlay

enum class TargetDurationChoice(
    val label: String,
    val durationMillis: Long
) {
    ONE_MINUTE("1\uBD84", 60_000L),
    THREE_MINUTES("3\uBD84", 180_000L),
    FIVE_MINUTES("5\uBD84", 300_000L),
    TEN_MINUTES("10\uBD84", 600_000L)
}
