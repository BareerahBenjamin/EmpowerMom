package com.empowermom.app.core.data.local.dao

import androidx.room.*
import com.empowermom.app.core.data.local.entity.MessageEntity
import com.empowermom.app.core.data.local.entity.ReplyEntity
import com.empowermom.app.core.data.local.entity.UserInteractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // ── 查询 ──────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM messages WHERE isHidden = 0 AND isPrivateOnly = 0 ORDER BY timestamp DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE category = :category AND isHidden = 0 AND isPrivateOnly = 0 ORDER BY timestamp DESC")
    fun observeMessagesByCategory(category: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE userId = :userId AND (isPrivateOnly = 1 OR isHidden = 1) ORDER BY timestamp DESC")
    fun observePrivateMessages(userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE id = :id")
    fun observeMessageById(id: Long): Flow<MessageEntity?>

    @Query("SELECT COUNT(*) FROM messages WHERE isHidden = 0 AND isPrivateOnly = 0")
    suspend fun getMessageCount(): Int

    // ── 写入 ──────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET likes = likes + 1 WHERE id = :id")
    suspend fun incrementLikes(id: Long)

    @Query("UPDATE messages SET likes = likes - 1 WHERE id = :id")
    suspend fun decrementLikes(id: Long)

    @Query("UPDATE messages SET resonances = resonances + 1 WHERE id = :id")
    suspend fun incrementResonances(id: Long)

    @Query("UPDATE messages SET resonances = resonances - 1 WHERE id = :id")
    suspend fun decrementResonances(id: Long)

    @Query("UPDATE messages SET aiResponse = :response WHERE id = :id")
    suspend fun updateAiResponse(id: Long, response: String)

    @Query("UPDATE messages SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: MessageEntity)

    // ── 同步相关 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM messages WHERE syncStatus = 'local'")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getMessageByRemoteId(remoteId: Long): MessageEntity?

    /**
     * 查询当前用户名下「AI 回应缺失或为兜底句」的留言，用于后台补生成。
     * 既覆盖失败留空（aiResponse = ''），也覆盖历史上被写入兜底句的脏数据。
     */
    @Query("SELECT * FROM messages WHERE userId = :userId AND (aiResponse = '' OR aiResponse = :fallback)")
    suspend fun getMessagesNeedingAiResponse(userId: String, fallback: String): List<MessageEntity>

    @Query("SELECT * FROM messages")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("UPDATE messages SET remoteId = :remoteId, syncStatus = 'synced' WHERE id = :localId")
    suspend fun markSynced(localId: Long, remoteId: Long)
}

@Dao
interface ReplyDao {

    @Query("SELECT * FROM replies WHERE messageId = :messageId ORDER BY timestamp ASC")
    fun observeRepliesByMessageId(messageId: Long): Flow<List<ReplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: ReplyEntity): Long

    @Delete
    suspend fun deleteReply(reply: ReplyEntity)

    @Query("DELETE FROM replies WHERE messageId = :messageId")
    suspend fun deleteRepliesByMessageId(messageId: Long)

    // ── 同步相关 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM replies WHERE syncStatus = 'local'")
    suspend fun getUnsyncedReplies(): List<ReplyEntity>

    @Query("SELECT * FROM replies WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReplyEntity?

    @Query("SELECT * FROM replies WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getReplyByRemoteId(remoteId: Long): ReplyEntity?

    @Query("UPDATE replies SET remoteId = :remoteId, syncStatus = 'synced' WHERE id = :localId")
    suspend fun markSynced(localId: Long, remoteId: Long)
}

@Dao
interface UserInteractionDao {

    @Query("SELECT * FROM user_interactions WHERE messageId = :messageId AND interactionType = :type")
    suspend fun getInteraction(messageId: Long, type: String): UserInteractionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: UserInteractionEntity)

    @Query("DELETE FROM user_interactions WHERE messageId = :messageId AND interactionType = :type")
    suspend fun deleteInteraction(messageId: Long, type: String)

    @Query("SELECT messageId FROM user_interactions WHERE interactionType = 'like'")
    suspend fun getLikedMessageIds(): List<Long>

    @Query("SELECT messageId FROM user_interactions WHERE interactionType = 'resonance'")
    suspend fun getResonatedMessageIds(): List<Long>

    // ── 同步相关 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM user_interactions WHERE syncStatus = 'local'")
    suspend fun getUnsyncedInteractions(): List<UserInteractionEntity>

    @Query("UPDATE user_interactions SET syncStatus = 'synced' WHERE messageId = :messageId AND interactionType = :type")
    suspend fun markInteractionSynced(messageId: Long, type: String)
}

/*
==================== 原有内容（保留，勿删）====================

@Dao
interface MessageDao {

    // ── 查询 ──────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM messages WHERE isHidden = 0 ORDER BY timestamp DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE category = :category AND isHidden = 0 ORDER BY timestamp DESC")
    fun observeMessagesByCategory(category: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE isHidden = 0")
    suspend fun getMessageCount(): Int
}
*/
