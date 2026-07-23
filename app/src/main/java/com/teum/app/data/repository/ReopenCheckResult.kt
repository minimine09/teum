package com.teum.app.data.repository

data class ReopenCheckResult(
    val previousSessionId: Long?,
    val previousEndTimeMillis: Long?,
    val gapTimeMillis: Long?,
    val isFastReopen: Boolean
)
