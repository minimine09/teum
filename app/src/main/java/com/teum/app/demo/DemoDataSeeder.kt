package com.teum.app.demo

interface DemoDataSeeder {
    suspend fun resetAndSeed(
        nowMillis: Long = System.currentTimeMillis()
    ): DemoSeedResult

    suspend fun verifyCurrentSeed(
        nowMillis: Long = System.currentTimeMillis()
    ): DemoSeedResult
}
