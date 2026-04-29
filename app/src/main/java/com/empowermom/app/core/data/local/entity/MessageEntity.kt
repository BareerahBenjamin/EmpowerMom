package com.empowermom.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val author: String,
    val category: String,          // MessageCategory.name
    val tagsJson: String = "[]",   // JSON array of tag strings
    val timestamp: Long = Date().time,
    val likes: Int = 0,
    val resonances: Int = 0,
    val isAnonymous: Boolean = true,
    val aiResponse: String = "",
    val isCrisis: Boolean = false,
    val isHidden: Boolean = false,
    val syncStatus: String = "local" // "local" | "synced" | "pending"
)

@Entity(
    tableName = "replies",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class ReplyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val content: String,
    val author: String,
    val timestamp: Long = Date().time,
    val isAnonymous: Boolean = true
)

// 用于记录用户对留言的点赞/共鸣状态（本地存储）
@Entity(
    tableName = "user_interactions",
    primaryKeys = ["messageId", "interactionType"]
)
data class UserInteractionEntity(
    val messageId: Long,
    val interactionType: String,   // "like" | "resonance"
    val timestamp: Long = Date().time
)
