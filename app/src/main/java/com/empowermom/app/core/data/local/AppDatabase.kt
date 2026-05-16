package com.empowermom.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
        DailyLogEntity::class,          // ← 新增
    ],
    version = 2,                        // ← 从 1 升到 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun replyDao(): ReplyDao
    abstract fun userInteractionDao(): UserInteractionDao
    abstract fun dailyLogDao(): DailyLogDao   // ← 新增

    companion object {
        const val DATABASE_NAME = "empowermom.db"
    }
}
