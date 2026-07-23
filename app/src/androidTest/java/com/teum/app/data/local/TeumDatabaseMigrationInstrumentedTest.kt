package com.teum.app.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeumDatabaseMigrationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TeumDatabase::class.java
    )

    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrate1To6_preservesSessionsAndAddsAnalyticsSchema() {
        createVersionOneDatabase()

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            TeumDatabase.MIGRATION_1_2,
            TeumDatabase.MIGRATION_2_3,
            TeumDatabase.MIGRATION_3_4,
            TeumDatabase.MIGRATION_4_5,
            TeumDatabase.MIGRATION_5_6
        ).use { database ->
            assertPreservedVersionOneSession(database)
            assertOutcomeColumnsDefaultToNull(database)
            assertSessionMetricColumnsDefaultToZero(database)
            assertNecessaryUseColumns(database)
            assertAppOpenEventsTableAcceptsRows(database)
            assertReopenLogsTableAcceptsRows(database)
        }
    }

    @Test
    fun migrate3To6_preservesSessionsAndAddsSessionMetrics() {
        migrationHelper.createDatabase(TEST_DATABASE, 3).use { database ->
            insertVersionThreeSession(database)
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            TeumDatabase.MIGRATION_3_4,
            TeumDatabase.MIGRATION_4_5,
            TeumDatabase.MIGRATION_5_6
        ).use { database ->
            assertPreservedVersionOneSession(database)
            assertOutcomeColumnsDefaultToNull(database)
            assertSessionMetricColumnsDefaultToZero(database)
            assertNecessaryUseColumns(database)
            assertReopenLogsTableAcceptsRows(database)
        }
    }

    @Test
    fun migrate4To6_preservesExistingOverrunAsRawOverrun() {
        migrationHelper.createDatabase(TEST_DATABASE, 4).use { database ->
            insertVersionFourSession(database)
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            TeumDatabase.MIGRATION_4_5,
            TeumDatabase.MIGRATION_5_6
        ).use { database ->
            assertNecessaryUseColumns(database, expectedRawOverrunMillis = 5_000L)
            assertReopenLogsTableAcceptsRows(database)
        }
    }

    @Test
    fun migrate5To6_addsReopenLogRelationships() {
        migrationHelper.createDatabase(TEST_DATABASE, 5).use { database ->
            insertVersionFiveSession(database)
            insertVersionFiveReopenedSession(database)
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            TeumDatabase.MIGRATION_5_6
        ).use { database ->
            assertVersionFiveReopenWasMigrated(database)
        }
    }

    private fun createVersionOneDatabase() {
        context.deleteDatabase(TEST_DATABASE)
        context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(CREATE_VERSION_ONE_SESSION_LOGS)
            database.execSQL(
                """
                INSERT INTO session_logs (
                    id,
                    packageName,
                    entryDetectedAtMillis,
                    startedAtMillis,
                    endedAtMillis,
                    durationMillis,
                    targetDurationMillis,
                    intentChoice,
                    outcomeType,
                    overrun,
                    extensionCount,
                    isFastReopen,
                    reopenGapMillis,
                    createdAtMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    SESSION_ID,
                    PACKAGE_NAME,
                    1_000L,
                    2_000L,
                    12_000L,
                    10_000L,
                    60_000L,
                    "CLEAR_PURPOSE",
                    "ENDED",
                    0,
                    1,
                    1,
                    30_000L,
                    12_500L
                )
            )
            database.version = 1
        }
    }

    private fun assertPreservedVersionOneSession(database: SupportSQLiteDatabase) {
        database.query("SELECT * FROM session_logs WHERE id = $SESSION_ID").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(PACKAGE_NAME, cursor.getString(cursor.getColumnIndexOrThrow("packageName")))
            assertEquals(10_000L, cursor.getLong(cursor.getColumnIndexOrThrow("durationMillis")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("extensionCount")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isFastReopen")))
            assertEquals(30_000L, cursor.getLong(cursor.getColumnIndexOrThrow("reopenGapMillis")))
        }
    }

    private fun assertOutcomeColumnsDefaultToNull(database: SupportSQLiteDatabase) {
        database.query(
            """
            SELECT
                outcomeRespondedAtMillis,
                outcomeAchieved,
                purposeDrifted,
                closedAfterIntervention,
                interventionExitConfirmedAtMillis
            FROM session_logs
            WHERE id = $SESSION_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            repeat(cursor.columnCount) { columnIndex ->
                assertNull(cursor.getString(columnIndex))
            }
        }
    }

    private fun assertSessionMetricColumnsDefaultToZero(database: SupportSQLiteDatabase) {
        database.query(
            """
            SELECT
                interventionVisibleMillis,
                effectiveUsageMillis,
                totalExtensionDurationMillis,
                finalTargetDurationMillis,
                overrunMillis
            FROM session_logs
            WHERE id = $SESSION_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            repeat(cursor.columnCount) { columnIndex ->
                assertEquals(0L, cursor.getLong(columnIndex))
            }
        }
    }

    private fun assertNecessaryUseColumns(
        database: SupportSQLiteDatabase,
        expectedRawOverrunMillis: Long = 0L
    ) {
        database.query(
            """
            SELECT
                rawOverrunMillis,
                necessaryUseExcessMillis
            FROM session_logs
            WHERE id = $SESSION_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(
                expectedRawOverrunMillis,
                cursor.getLong(cursor.getColumnIndexOrThrow("rawOverrunMillis"))
            )
            assertEquals(
                0L,
                cursor.getLong(cursor.getColumnIndexOrThrow("necessaryUseExcessMillis"))
            )
        }
    }

    private fun insertVersionThreeSession(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO session_logs (
                id,
                packageName,
                entryDetectedAtMillis,
                startedAtMillis,
                endedAtMillis,
                durationMillis,
                targetDurationMillis,
                intentChoice,
                outcomeType,
                outcomeRespondedAtMillis,
                outcomeAchieved,
                purposeDrifted,
                closedAfterIntervention,
                interventionExitConfirmedAtMillis,
                overrun,
                extensionCount,
                isFastReopen,
                reopenGapMillis,
                createdAtMillis
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                SESSION_ID,
                PACKAGE_NAME,
                1_000L,
                2_000L,
                12_000L,
                10_000L,
                60_000L,
                "CLEAR_PURPOSE",
                "ENDED",
                null,
                null,
                null,
                null,
                null,
                0,
                1,
                1,
                30_000L,
                12_500L
            )
        )
    }

    private fun insertVersionFourSession(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO session_logs (
                id,
                packageName,
                entryDetectedAtMillis,
                startedAtMillis,
                endedAtMillis,
                durationMillis,
                targetDurationMillis,
                interventionVisibleMillis,
                effectiveUsageMillis,
                totalExtensionDurationMillis,
                finalTargetDurationMillis,
                overrunMillis,
                intentChoice,
                outcomeType,
                outcomeRespondedAtMillis,
                outcomeAchieved,
                purposeDrifted,
                closedAfterIntervention,
                interventionExitConfirmedAtMillis,
                overrun,
                extensionCount,
                isFastReopen,
                reopenGapMillis,
                createdAtMillis
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                SESSION_ID,
                PACKAGE_NAME,
                1_000L,
                2_000L,
                12_000L,
                10_000L,
                5_000L,
                0L,
                10_000L,
                0L,
                5_000L,
                5_000L,
                "CLEAR_PURPOSE",
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                0,
                null,
                12_500L
            )
        )
    }

    private fun insertVersionFiveSession(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO session_logs (
                id,
                packageName,
                entryDetectedAtMillis,
                startedAtMillis,
                endedAtMillis,
                durationMillis,
                targetDurationMillis,
                interventionVisibleMillis,
                effectiveUsageMillis,
                totalExtensionDurationMillis,
                finalTargetDurationMillis,
                rawOverrunMillis,
                overrunMillis,
                necessaryUseExcessMillis,
                intentChoice,
                outcomeType,
                outcomeRespondedAtMillis,
                outcomeAchieved,
                purposeDrifted,
                closedAfterIntervention,
                interventionExitConfirmedAtMillis,
                overrun,
                extensionCount,
                isFastReopen,
                reopenGapMillis,
                createdAtMillis
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                SESSION_ID,
                PACKAGE_NAME,
                1_000L,
                2_000L,
                12_000L,
                10_000L,
                5_000L,
                0L,
                10_000L,
                0L,
                5_000L,
                5_000L,
                5_000L,
                0L,
                "CLEAR_PURPOSE",
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                0,
                0,
                null,
                12_500L
            )
        )
    }

    private fun insertVersionFiveReopenedSession(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO session_logs (
                id,
                packageName,
                entryDetectedAtMillis,
                startedAtMillis,
                endedAtMillis,
                durationMillis,
                targetDurationMillis,
                interventionVisibleMillis,
                effectiveUsageMillis,
                totalExtensionDurationMillis,
                finalTargetDurationMillis,
                rawOverrunMillis,
                overrunMillis,
                necessaryUseExcessMillis,
                intentChoice,
                outcomeType,
                outcomeRespondedAtMillis,
                outcomeAchieved,
                purposeDrifted,
                closedAfterIntervention,
                interventionExitConfirmedAtMillis,
                overrun,
                extensionCount,
                isFastReopen,
                reopenGapMillis,
                createdAtMillis
            )
            SELECT
                ?,
                packageName,
                ?,
                ?,
                ?,
                durationMillis,
                targetDurationMillis,
                interventionVisibleMillis,
                effectiveUsageMillis,
                totalExtensionDurationMillis,
                finalTargetDurationMillis,
                rawOverrunMillis,
                overrunMillis,
                necessaryUseExcessMillis,
                intentChoice,
                outcomeType,
                outcomeRespondedAtMillis,
                outcomeAchieved,
                purposeDrifted,
                closedAfterIntervention,
                interventionExitConfirmedAtMillis,
                overrun,
                extensionCount,
                ?,
                ?,
                ?
            FROM session_logs
            WHERE id = ?
            """.trimIndent(),
            arrayOf(
                CURRENT_SESSION_ID,
                42_000L,
                42_000L,
                52_000L,
                1,
                30_000L,
                52_500L,
                SESSION_ID
            )
        )
    }

    private fun assertAppOpenEventsTableAcceptsRows(database: SupportSQLiteDatabase) {
        database.execSQL(
            "INSERT INTO app_open_events (packageName, detectedAtMillis) VALUES (?, ?)",
            arrayOf(PACKAGE_NAME, 20_000L)
        )

        database.query("SELECT packageName, detectedAtMillis FROM app_open_events").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(PACKAGE_NAME, cursor.getString(0))
            assertEquals(20_000L, cursor.getLong(1))
        }
    }

    private fun assertReopenLogsTableAcceptsRows(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            INSERT INTO reopen_logs (
                previousSessionId,
                currentSessionId,
                gapTimeMillis,
                isFastReopen
            ) VALUES (?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(SESSION_ID, SESSION_ID, 30_000L, 1)
        )

        database.query(
            """
            SELECT previousSessionId, currentSessionId, gapTimeMillis, isFastReopen
            FROM reopen_logs
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(SESSION_ID, cursor.getLong(0))
            assertEquals(SESSION_ID, cursor.getLong(1))
            assertEquals(30_000L, cursor.getLong(2))
            assertEquals(1, cursor.getInt(3))
        }
    }

    private fun assertVersionFiveReopenWasMigrated(database: SupportSQLiteDatabase) {
        database.query(
            """
            SELECT previousSessionId, currentSessionId, gapTimeMillis, isFastReopen
            FROM reopen_logs
            WHERE currentSessionId = $CURRENT_SESSION_ID
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(SESSION_ID, cursor.getLong(0))
            assertEquals(CURRENT_SESSION_ID, cursor.getLong(1))
            assertEquals(30_000L, cursor.getLong(2))
            assertEquals(1, cursor.getInt(3))
        }
    }

    private companion object {
        const val TEST_DATABASE = "teum-migration-test.db"
        const val SESSION_ID = 7L
        const val CURRENT_SESSION_ID = 8L
        const val PACKAGE_NAME = "com.google.android.youtube"

        val CREATE_VERSION_ONE_SESSION_LOGS =
            """
            CREATE TABLE IF NOT EXISTS session_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                packageName TEXT NOT NULL,
                entryDetectedAtMillis INTEGER NOT NULL,
                startedAtMillis INTEGER NOT NULL,
                endedAtMillis INTEGER NOT NULL,
                durationMillis INTEGER NOT NULL,
                targetDurationMillis INTEGER NOT NULL,
                intentChoice TEXT NOT NULL,
                outcomeType TEXT,
                overrun INTEGER NOT NULL,
                extensionCount INTEGER NOT NULL,
                isFastReopen INTEGER NOT NULL,
                reopenGapMillis INTEGER,
                createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
    }
}
