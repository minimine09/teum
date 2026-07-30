package com.teum.app.demo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoSeedInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val seeder = RoomDemoDataSeeder(context, KST)

    @Test
    fun resetAndSeedVerifiesProductionAnalyzerValuesAndIsIdempotent() = runBlocking {
        val first = seeder.resetAndSeed(NOW_AFTER_SAFE_WINDOW)
        val second = seeder.resetAndSeed(NOW_AFTER_SAFE_WINDOW)

        assertTrue(first.mismatches.joinToString(), first.success)
        assertTrue(second.mismatches.joinToString(), second.success)
        assertEquals(first.rowCounts, second.rowCounts)
        assertEquals(first.actualValues, second.actualValues)
        assertEquals("2", second.actualValues.getValue("home.todayTargetKeptCount"))
        assertEquals("5", second.actualValues.getValue("weekly.overrunCount"))
        assertEquals("9", second.actualValues.getValue("internal.rawFinalOverrunCount"))
        assertEquals("22", second.actualValues.getValue("vulnerable.topHour"))
    }

    @Test
    fun beforeCalculatedSafeWindowSeedFailsWithSafeWarning() = runBlocking {
        val result = seeder.resetAndSeed(NOW_BEFORE_SAFE_WINDOW)

        assertFalse(result.success)
        assertTrue(result.warnings.any { it.contains("필요한 시간이 아직 부족") })
    }

    @Test
    fun calculatedSafeWindowAndFourteenOClockCanSeed() = runBlocking {
        val atMinimum = seeder.resetAndSeed(NOW_AT_SAFE_WINDOW)
        val afterMinimum = seeder.resetAndSeed(NOW_AFTER_SAFE_WINDOW_BY_ONE_SECOND)
        val afternoon = seeder.resetAndSeed(NOW_AT_DEMO_TIME)

        assertTrue(atMinimum.mismatches.joinToString(), atMinimum.success)
        assertTrue(afterMinimum.mismatches.joinToString(), afterMinimum.success)
        assertTrue(afternoon.mismatches.joinToString(), afternoon.success)
    }

    @Test
    fun forcedFailureRollsBackRoomTransaction() = runBlocking {
        val baseline = seeder.resetAndSeed(NOW_AFTER_SAFE_WINDOW)
        assertTrue(baseline.success)

        val failed = seeder.resetAndSeedForTest(
            nowMillis = NOW_AFTER_SAFE_WINDOW,
            failInsideTransaction = true
        )
        val afterRollback = seeder.verifyCurrentSeed(NOW_AFTER_SAFE_WINDOW)

        assertFalse(failed.success)
        assertTrue(afterRollback.mismatches.joinToString(), afterRollback.success)
        assertEquals(baseline.rowCounts, afterRollback.rowCounts)
        assertEquals(baseline.actualValues, afterRollback.actualValues)
    }

    private companion object {
        val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
        val START_OF_DAY: Long = fixedNow(hour = 0)
        val SAFE_WINDOW: Long = DemoDatasetFactory.minimumRequiredTodayElapsedMillis()
        val NOW_BEFORE_SAFE_WINDOW: Long = START_OF_DAY + SAFE_WINDOW - 1_000L
        val NOW_AT_SAFE_WINDOW: Long = START_OF_DAY + SAFE_WINDOW
        val NOW_AFTER_SAFE_WINDOW_BY_ONE_SECOND: Long = START_OF_DAY + SAFE_WINDOW + 1_000L
        val NOW_AFTER_SAFE_WINDOW: Long = fixedNow(hour = 2)
        val NOW_AT_DEMO_TIME: Long = fixedNow(hour = 14)

        fun fixedNow(hour: Int): Long = Calendar.getInstance(KST).apply {
            set(2026, Calendar.JULY, 31, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
