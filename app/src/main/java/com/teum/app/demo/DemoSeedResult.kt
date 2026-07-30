package com.teum.app.demo

data class DemoSeedResult(
    val success: Boolean,
    val generatedAtMillis: Long,
    val rowCounts: Map<String, Int>,
    val expectedValues: Map<String, String>,
    val actualValues: Map<String, String>,
    val mismatches: List<String>,
    val warnings: List<String>
)
