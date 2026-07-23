package com.teum.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teum.app.data.local.dao.AppOpenEventDao
import com.teum.app.data.local.dao.ReopenLogDao
import com.teum.app.data.local.dao.SessionLogDao
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.ReopenLogEntity
import com.teum.app.data.local.entity.SessionLogEntity

@Database(
    entities = [
        SessionLogEntity::class,
        AppOpenEventEntity::class,
        ReopenLogEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class TeumDatabase : RoomDatabase() {
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun appOpenEventDao(): AppOpenEventDao
    abstract fun reopenLogDao(): ReopenLogDao

    companion object {
        @Volatile
        private var instance: TeumDatabase? = null

        fun getInstance(context: Context): TeumDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeumDatabase::class.java,
                    "teum.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                    .build()
                    .also { database ->
                    instance = database
                }
            }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_open_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        detectedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN outcomeRespondedAtMillis INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN outcomeAchieved INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN purposeDrifted INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN closedAfterIntervention INTEGER"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN interventionExitConfirmedAtMillis INTEGER"
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN interventionVisibleMillis INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN effectiveUsageMillis INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN totalExtensionDurationMillis INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN finalTargetDurationMillis INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN overrunMillis INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN rawOverrunMillis INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE session_logs ADD COLUMN necessaryUseExcessMillis INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE session_logs SET rawOverrunMillis = overrunMillis"
                )
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reopen_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        previousSessionId INTEGER NOT NULL,
                        currentSessionId INTEGER NOT NULL,
                        gapTimeMillis INTEGER NOT NULL,
                        isFastReopen INTEGER NOT NULL,
                        FOREIGN KEY(previousSessionId) REFERENCES session_logs(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(currentSessionId) REFERENCES session_logs(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reopen_logs_previousSessionId " +
                        "ON reopen_logs(previousSessionId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reopen_logs_currentSessionId " +
                        "ON reopen_logs(currentSessionId)"
                )
                database.execSQL(
                    """
                    INSERT INTO reopen_logs (
                        previousSessionId,
                        currentSessionId,
                        gapTimeMillis,
                        isFastReopen
                    )
                    SELECT
                        (
                            SELECT previous.id
                            FROM session_logs AS previous
                            WHERE previous.packageName = current.packageName
                                AND previous.id != current.id
                                AND previous.endedAtMillis <= current.entryDetectedAtMillis
                            ORDER BY previous.endedAtMillis DESC
                            LIMIT 1
                        ),
                        current.id,
                        current.reopenGapMillis,
                        current.isFastReopen
                    FROM session_logs AS current
                    WHERE current.reopenGapMillis IS NOT NULL
                        AND EXISTS (
                            SELECT 1
                            FROM session_logs AS previous
                            WHERE previous.packageName = current.packageName
                                AND previous.id != current.id
                                AND previous.endedAtMillis <= current.entryDetectedAtMillis
                        )
                    """.trimIndent()
                )
            }
        }
    }
}
