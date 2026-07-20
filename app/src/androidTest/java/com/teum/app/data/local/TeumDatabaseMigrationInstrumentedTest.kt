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
    fun migrate1To3_preservesSessionsAndAddsAnalyticsSchema() {
        createVersionOneDatabase()

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            TeumDatabase.MIGRATION_1_2,
            TeumDatabase.MIGRATION_2_3
        ).use { database ->
            assertPreservedVersionOneSession(database)
            assertOutcomeColumnsDefaultToNull(database)
            assertAppOpenEventsTableAcceptsRows(database)
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

    private companion object {
        const val TEST_DATABASE = "teum-migration-test.db"
        const val SESSION_ID = 7L
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
