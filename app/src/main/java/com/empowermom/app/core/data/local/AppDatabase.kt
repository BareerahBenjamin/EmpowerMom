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
    version = 4,
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

/*
==================== 原有内容（保留，勿删）====================

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
*/

/*
==================== 原有内容（保留，勿删）- version 3 ====================

@Database(
    entities = [
        MessageEntity::class,
        ReplyEntity::class,
        UserInteractionEntity::class,
        DailyLogEntity::class,
    ],
    version = 3,
    exportSchema = true
)
*/
