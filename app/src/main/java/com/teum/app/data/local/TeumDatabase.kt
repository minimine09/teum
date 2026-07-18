package com.teum.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teum.app.data.local.dao.AppOpenEventDao
import com.teum.app.data.local.dao.SessionLogDao
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.SessionLogEntity

@Database(
    entities = [SessionLogEntity::class, AppOpenEventEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TeumDatabase : RoomDatabase() {
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun appOpenEventDao(): AppOpenEventDao

    companion object {
        @Volatile
        private var instance: TeumDatabase? = null

        fun getInstance(context: Context): TeumDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeumDatabase::class.java,
                    "teum.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { database ->
                    instance = database
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
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
    }
}
