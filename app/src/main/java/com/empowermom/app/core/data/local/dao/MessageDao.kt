package com.empowermom.app.core.data.local.dao

import androidx.room.*
import com.empowermom.app.core.data.local.entity.MessageEntity
import com.empowermom.app.core.data.local.entity.ReplyEntity
import com.empowermom.app.core.data.local.entity.UserInteractionEntity
import kotlinx.coroutines.flow.Flow

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

    @Delete
    suspend fun deleteMessage(message: MessageEntity)
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
}
