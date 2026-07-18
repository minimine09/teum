package com.teum.app.session

data class ReopenCheckResult(
    val isFastReopen: Boolean,
    val gapMillis: Long?,
    val previousEndTimeMillis: Long?
)
