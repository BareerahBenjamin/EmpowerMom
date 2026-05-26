package com.empowermom.app.core.data.repository

import android.util.Log
import com.empowermom.app.core.data.local.dao.MessageDao
import com.empowermom.app.core.data.local.dao.ReplyDao
import com.empowermom.app.core.data.local.dao.UserInteractionDao
import com.empowermom.app.core.data.local.entity.MessageEntity
import com.empowermom.app.core.data.local.entity.ReplyEntity
import com.empowermom.app.core.data.local.entity.UserInteractionEntity
import com.empowermom.app.core.data.remote.dto.InteractionDto
import com.empowermom.app.core.data.remote.dto.MessageDto
import com.empowermom.app.core.data.remote.dto.ReplyDto
import com.empowermom.app.feature.messageboard.model.CrisisKeywords
import com.empowermom.app.core.network.PromptTemplates
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.messageboard.model.MessageCategory
import com.empowermom.app.feature.messageboard.model.MediaAttachment
import com.empowermom.app.feature.messageboard.model.Reply
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val replyDao: ReplyDao,
    private val userInteractionDao: UserInteractionDao,
    private val gson: Gson,
    private val deepSeekApiService: com.empowermom.app.core.network.DeepSeekApiService,
    private val supabaseRepository: SupabaseRepository,
    private val authRepository: AuthRepository
) {

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "MessageRepository"

    // ── 查询留言列表（响应式，带分类过滤）──────────────────────────────────────

    fun observeMessages(
        category: MessageCategory? = null,
        privateOnly: Boolean = false
    ): Flow<List<Message>> {
        val entitiesFlow = when {
            privateOnly -> messageDao.observePrivateMessages()
            category == null -> messageDao.observeAllMessages()
            else -> messageDao.observeMessagesByCategory(category.name)
        }

        return entitiesFlow.map { entities ->
            val likedIds = userInteractionDao.getLikedMessageIds().toSet()
            val resonatedIds = userInteractionDao.getResonatedMessageIds().toSet()

            entities.map { entity ->
                entity.toMessage(
                    replies = emptyList(),
                    isLiked = entity.id in likedIds,
                    isResonated = entity.id in resonatedIds,
                    gson = gson
                )
            }
        }
    }

    // ── 发布新留言 ────────────────────────────────────────────────────────────

    suspend fun postMessage(
        content: String,
        category: MessageCategory,
        tags: List<String>,
        attachments: List<MediaAttachment>,
        author: String,
        isAnonymous: Boolean,
        isPrivateOnly: Boolean
    ): Long {
        val isCrisis = CrisisKeywords.detect(content)
        val userId = authRepository.currentUserId()
        val entity = MessageEntity(
            content = content,
            author = author,
            userId = userId,
            category = category.name,
            tagsJson = gson.toJson(tags),
            attachmentsJson = gson.toJson(attachments),
            timestamp = Date().time,
            isAnonymous = isAnonymous,
            isCrisis = isCrisis,
            isHidden = isCrisis,
            isPrivateOnly = isPrivateOnly,
            syncStatus = "local"
        )
        val localId = messageDao.insertMessage(entity)

        // 后台同步到 Supabase
        syncScope.launch {
            try {
                val dto = MessageDto(
                    userId = userId,
                    content = content,
                    author = author,
                    category = category.name,
                    tagsJson = gson.toJson(tags),
                    attachmentsJson = gson.toJson(attachments),
                    timestamp = entity.timestamp,
                    isAnonymous = isAnonymous,
                    aiResponse = "",
                    isCrisis = isCrisis,
                    isHidden = isCrisis,
                    isPrivateOnly = isPrivateOnly
                )
                val remoteDto = supabaseRepository.insertMessage(dto)
                messageDao.markSynced(localId, remoteDto.id)
                Log.d(TAG, "帖子同步成功 localId=$localId → remoteId=${remoteDto.id}")
            } catch (e: Exception) {
                Log.e(TAG, "帖子同步失败 localId=$localId: ${e.message}", e)
            }
        }

        return localId
    }

    // ── 更新 AI 回应 ──────────────────────────────────────────────────────────

    suspend fun updateAiResponse(messageId: Long, response: String) {
        messageDao.updateAiResponse(messageId, response)

        // 同步 AI 回应到 Supabase
        syncScope.launch {
            try {
                val entity = messageDao.getMessageById(messageId) ?: return@launch
                val remoteId = entity.remoteId ?: return@launch
                supabaseRepository.updateAiResponse(remoteId, response)
                Log.d(TAG, "AI 回应同步成功 messageId=$messageId")
            } catch (e: Exception) {
                Log.e(TAG, "AI 回应同步失败: ${e.message}", e)
            }
        }
    }

    // ── 生成 AI 回应 ──────────────────────────────────────────────────────────

    suspend fun generateAiResponse(content: String, category: MessageCategory): String {
        val systemPrompt = PromptTemplates.forCategory(category)

        val request = com.empowermom.app.core.network.DeepSeekRequest(
            messages = listOf(
                com.empowermom.app.core.network.DeepSeekMessage(
                    role = "system",
                    content = systemPrompt
                ),
                com.empowermom.app.core.network.DeepSeekMessage(
                    role = "user",
                    content = content
                )
            )
        )

        val response = deepSeekApiService.chatCompletion(
            authorization = "Bearer ${com.empowermom.app.BuildConfig.DEEPSEEK_API_KEY}",
            request = request
        )

        return response.choices.firstOrNull()?.message?.content
            ?: "谢谢你愿意分享这些。"
    }

    // ── 点赞 / 取消点赞 ───────────────────────────────────────────────────────

    suspend fun toggleLike(messageId: Long): Boolean {
        val existing = userInteractionDao.getInteraction(messageId, "like")
        val liked: Boolean
        if (existing == null) {
            userInteractionDao.insertInteraction(
                UserInteractionEntity(messageId, "like")
            )
            messageDao.incrementLikes(messageId)
            liked = true
        } else {
            userInteractionDao.deleteInteraction(messageId, "like")
            messageDao.decrementLikes(messageId)
            liked = false
        }

        // 同步到 Supabase
        syncInteractionToSupabase(messageId, "like", liked)

        return liked
    }

    // ── 共鸣 / 取消共鸣 ──────────────────────────────────────────────────────

    suspend fun toggleResonance(messageId: Long): Boolean {
        val existing = userInteractionDao.getInteraction(messageId, "resonance")
        val resonated: Boolean
        if (existing == null) {
            userInteractionDao.insertInteraction(
                UserInteractionEntity(messageId, "resonance")
            )
            messageDao.incrementResonances(messageId)
            resonated = true
        } else {
            userInteractionDao.deleteInteraction(messageId, "resonance")
            messageDao.decrementResonances(messageId)
            resonated = false
        }

        // 同步到 Supabase
        syncInteractionToSupabase(messageId, "resonance", resonated)

        return resonated
    }

    private fun syncInteractionToSupabase(messageId: Long, type: String, add: Boolean) {
        syncScope.launch {
            try {
                val userId = authRepository.currentUserId() ?: return@launch
                if (add) {
                    supabaseRepository.upsertInteraction(
                        InteractionDto(
                            userId = userId,
                            messageId = messageId,
                            interactionType = type,
                            timestamp = Date().time
                        )
                    )
                } else {
                    supabaseRepository.deleteInteraction(userId, messageId, type)
                }
                userInteractionDao.markInteractionSynced(messageId, type)
                Log.d(TAG, "互动同步成功 messageId=$messageId type=$type add=$add")
            } catch (e: Exception) {
                Log.e(TAG, "互动同步失败: ${e.message}", e)
            }
        }
    }

    // ── 发布回复 ──────────────────────────────────────────────────────────────

    suspend fun postReply(
        messageId: Long,
        content: String,
        author: String,
        isAnonymous: Boolean
    ): Long {
        val userId = authRepository.currentUserId()
        val entity = ReplyEntity(
            messageId = messageId,
            content = content,
            author = author,
            isAnonymous = isAnonymous,
            userId = userId,
            syncStatus = "local"
        )
        val localId = replyDao.insertReply(entity)

        // 后台同步到 Supabase
        syncScope.launch {
            try {
                // 需要远程 messageId（remoteId）
                val messageEntity = messageDao.getMessageById(messageId)
                val remoteMessageId = messageEntity?.remoteId
                if (remoteMessageId == null) {
                    Log.w(TAG, "回复同步跳过：帖子尚未同步到远程 messageId=$messageId")
                    return@launch
                }

                val dto = ReplyDto(
                    messageId = remoteMessageId,
                    userId = userId,
                    content = content,
                    author = author,
                    timestamp = entity.timestamp,
                    isAnonymous = isAnonymous
                )
                val remoteReply = supabaseRepository.insertReply(dto)
                replyDao.markSynced(localId, remoteReply.id)
                Log.d(TAG, "回复同步成功 localId=$localId → remoteId=${remoteReply.id}")
            } catch (e: Exception) {
                Log.e(TAG, "回复同步失败 localId=$localId: ${e.message}", e)
            }
        }

        return localId
    }

    // ── 删除留言（含所有回复）────────────────────────────────────────────────

    suspend fun deleteMessage(messageId: Long): Boolean {
        val entity = messageDao.getMessageById(messageId) ?: return false
        val currentUserId = authRepository.currentUserId()
        if (entity.userId != null && entity.userId != currentUserId) {
            Log.w(TAG, "无权删除他人留言 messageId=$messageId")
            return false
        }

        // 删除本地回复 + 留言
        replyDao.deleteRepliesByMessageId(messageId)
        messageDao.deleteById(messageId)
        Log.d(TAG, "本地删除留言成功 messageId=$messageId")

        // 后台同步删除远程数据
        val remoteId = entity.remoteId
        if (remoteId != null) {
            syncScope.launch {
                try {
                    supabaseRepository.deleteRepliesByMessage(remoteId)
                    supabaseRepository.deleteMessage(remoteId)
                    Log.d(TAG, "远程删除留言成功 remoteId=$remoteId")
                } catch (e: Exception) {
                    Log.e(TAG, "远程删除留言失败: ${e.message}", e)
                }
            }
        }

        return true
    }

    // ── 删除回复 ────────────────────────────────────────────────────────────

    suspend fun deleteReply(replyId: Long): Boolean {
        val entity = replyDao.getById(replyId) ?: return false
        val currentUserId = authRepository.currentUserId()
        if (entity.userId != null && entity.userId != currentUserId) {
            Log.w(TAG, "无权删除他人回复 replyId=$replyId")
            return false
        }

        // 删除本地回复
        replyDao.deleteReply(entity)
        Log.d(TAG, "本地删除回复成功 replyId=$replyId")

        // 后台同步删除远程数据
        val remoteId = entity.remoteId
        if (remoteId != null) {
            syncScope.launch {
                try {
                    supabaseRepository.deleteReply(remoteId)
                    Log.d(TAG, "远程删除回复成功 remoteId=$remoteId")
                } catch (e: Exception) {
                    Log.e(TAG, "远程删除回复失败: ${e.message}", e)
                }
            }
        }

        return true
    }

    // ── 查询单条留言（含回复，响应式）──────────────────────────────────────────

    fun observeMessageWithReplies(messageId: Long): Flow<Message?> {
        val messageFlow = messageDao.observeMessageById(messageId)
        val repliesFlow = replyDao.observeRepliesByMessageId(messageId)

        return combine(messageFlow, repliesFlow) { messageEntity, replyEntities ->
            if (messageEntity == null) return@combine null
            val likedIds = userInteractionDao.getLikedMessageIds().toSet()
            val resonatedIds = userInteractionDao.getResonatedMessageIds().toSet()

            val replies = replyEntities.map { entity ->
                Reply(
                    id = entity.id,
                    userId = entity.userId,
                    messageId = entity.messageId,
                    content = entity.content,
                    author = entity.author,
                    timestamp = Date(entity.timestamp),
                    isAnonymous = entity.isAnonymous
                )
            }

            messageEntity.toMessage(
                replies = replies,
                isLiked = messageEntity.id in likedIds,
                isResonated = messageEntity.id in resonatedIds,
                gson = gson
            )
        }
    }

    // ── 从远程拉取最新数据 ────────────────────────────────────────────────────

    suspend fun refreshFromRemote() {
        try {
            val remoteMessages = supabaseRepository.fetchMessages()
            Log.d(TAG, "拉取到 ${remoteMessages.size} 条远程帖子")

            for (dto in remoteMessages) {
                val existing = messageDao.getMessageByRemoteId(dto.id)
                if (existing != null) {
                    // 更新已有帖子的计数和 AI 回应
                    val updated = existing.copy(
                        likes = dto.likes,
                        resonances = dto.resonances,
                        aiResponse = dto.aiResponse,
                        syncStatus = "synced"
                    )
                    messageDao.upsertMessage(updated)
                } else {
                    // 插入远程帖子到本地
                    val entity = MessageEntity(
                        remoteId = dto.id,
                        userId = dto.userId,
                        content = dto.content,
                        author = dto.author,
                        category = dto.category,
                        tagsJson = dto.tagsJson,
                        attachmentsJson = dto.attachmentsJson,
                        timestamp = dto.timestamp,
                        likes = dto.likes,
                        resonances = dto.resonances,
                        isAnonymous = dto.isAnonymous,
                        aiResponse = dto.aiResponse,
                        isCrisis = dto.isCrisis,
                        isHidden = dto.isHidden,
                        isPrivateOnly = dto.isPrivateOnly,
                        syncStatus = "synced"
                    )
                    messageDao.upsertMessage(entity)
                }
            }

            // 拉取所有本地帖子的远程回复
            val allLocalMessages = messageDao.getAllMessages()
            for (entity in allLocalMessages) {
                val remoteId = entity.remoteId ?: continue
                syncRepliesForMessage(entity.id, remoteId)
            }

            Log.d(TAG, "远程数据同步完成")
        } catch (e: Exception) {
            Log.e(TAG, "拉取远程数据失败: ${e.message}", e)
            throw e
        }
    }

    private suspend fun syncRepliesForMessage(localMessageId: Long, remoteMessageId: Long) {
        try {
            val remoteReplies = supabaseRepository.fetchReplies(remoteMessageId)
            for (dto in remoteReplies) {
                val existing = replyDao.getReplyByRemoteId(dto.id)
                if (existing == null) {
                    val entity = ReplyEntity(
                        remoteId = dto.id,
                        userId = dto.userId,
                        messageId = localMessageId,
                        content = dto.content,
                        author = dto.author,
                        timestamp = dto.timestamp,
                        isAnonymous = dto.isAnonymous,
                        syncStatus = "synced"
                    )
                    replyDao.insertReply(entity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "拉取回复失败 remoteMessageId=$remoteMessageId: ${e.message}", e)
        }
    }

    // ── Entity → Domain Model 映射 ────────────────────────────────────────────

    private fun MessageEntity.toMessage(
        replies: List<Reply>,
        isLiked: Boolean,
        isResonated: Boolean,
        gson: Gson
    ): Message {
        val tagType = object : TypeToken<List<String>>() {}.type
        val tagList: List<String> = try {
            gson.fromJson(tagsJson, tagType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val attachmentType = object : TypeToken<List<MediaAttachment>>() {}.type
        val attachments: List<MediaAttachment> = try {
            gson.fromJson(attachmentsJson, attachmentType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return Message(
            id = id,
            userId = userId,
            content = content,
            author = author,
            category = MessageCategory.valueOf(category),
            tags = tagList,
            attachments = attachments,
            timestamp = Date(timestamp),
            likes = likes,
            resonances = resonances,
            replies = replies,
            isAnonymous = isAnonymous,
            aiResponse = aiResponse,
            isCrisis = isCrisis,
            isHidden = isHidden,
            isPrivateOnly = isPrivateOnly,
            isLiked = isLiked,
            isResonated = isResonated
        )
    }
}
