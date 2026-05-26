package com.empowermom.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.empowermom.app.core.data.local.dao.DailyLogDao
import com.empowermom.app.core.data.local.dao.MessageDao
import com.empowermom.app.core.data.local.dao.ReplyDao
import com.empowermom.app.core.data.local.dao.UserInteractionDao
import com.empowermom.app.core.data.local.entity.DailyLogEntity
import com.empowermom.app.core.data.local.entity.MessageEntity
import com.empowermom.app.core.data.local.entity.ReplyEntity
import com.empowermom.app.core.data.local.entity.UserInteractionEntity

@Database(
    entities = [
        MessageEntity::class,
        ReplyEntity::class,
        UserInteractionEntity::class,
        DailyLogEntity::class,
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun replyDao(): ReplyDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun dailyLogDao(): DailyLogDao

    companion object {
        const val DATABASE_NAME = "empowermom.db"

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN remoteId INTEGER")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'local'")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN userId TEXT")
            }
        }
    }
}
