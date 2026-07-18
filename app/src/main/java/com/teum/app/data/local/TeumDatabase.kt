package com.teum.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.teum.app.data.local.dao.SessionLogDao
import com.teum.app.data.local.entity.SessionLogEntity

@Database(
    entities = [SessionLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TeumDatabase : RoomDatabase() {
    abstract fun sessionLogDao(): SessionLogDao

    companion object {
        @Volatile
        private var instance: TeumDatabase? = null

        fun getInstance(context: Context): TeumDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TeumDatabase::class.java,
                    "teum.db"
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
