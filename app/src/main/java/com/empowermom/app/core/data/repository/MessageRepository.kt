package com.empowermom.app.core.data.repository

import com.empowermom.app.core.data.local.dao.MessageDao
import com.empowermom.app.core.data.local.dao.ReplyDao
import com.empowermom.app.core.data.local.dao.UserInteractionDao
import com.empowermom.app.core.data.local.entity.MessageEntity
import com.empowermom.app.core.data.local.entity.ReplyEntity
import com.empowermom.app.core.data.local.entity.UserInteractionEntity
import com.empowermom.app.feature.messageboard.model.CrisisKeywords
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.messageboard.model.MessageCategory
import com.empowermom.app.feature.messageboard.model.Reply
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val replyDao: ReplyDao,
    private val userInteractionDao: UserInteractionDao,
    private val gson: Gson,
    private val deepSeekApiService: com.empowermom.app.core.network.DeepSeekApiService
) {

    // ── 查询留言列表（响应式，带分类过滤）──────────────────────────────────────

    fun observeMessages(category: MessageCategory? = null): Flow<List<Message>> {
        val entitiesFlow = if (category == null) {
            messageDao.observeAllMessages()
        } else {
            messageDao.observeMessagesByCategory(category.name)
        }

        return entitiesFlow.map { entities ->
            val likedIds = userInteractionDao.getLikedMessageIds().toSet()
            val resonatedIds = userInteractionDao.getResonatedMessageIds().toSet()

            // 危机帖（isHidden = true）从列表中过滤掉，避免其他妈妈看到。
            // TODO(C): 等有用户系统后，让作者本人能看到自己的危机帖。
            entities.filter { !it.isHidden }.map { entity ->
                val replies = replyDao.observeRepliesByMessageId(entity.id)
                entity.toMessage(
                    replies = emptyList(), // 列表页不加载回复，详情页加载
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
        author: String,
        isAnonymous: Boolean
    ): Long {
        val isCrisis = CrisisKeywords.detect(content)
        val entity = MessageEntity(
            content = content,
            author = author,
            category = category.name,
            tagsJson = gson.toJson(tags),
            timestamp = Date().time,
            isAnonymous = isAnonymous,
            isCrisis = isCrisis,
            isHidden = isCrisis // 危机内容默认隐藏（仅自己可见）
        )
        return messageDao.insertMessage(entity)
    }

    // ── 更新 AI 回应 ──────────────────────────────────────────────────────────

    suspend fun updateAiResponse(messageId: Long, response: String) {
        messageDao.updateAiResponse(messageId, response)
    }
    // ── 生成 AI 回应 ──────────────────────────────────────────────────────────

    suspend fun generateAiResponse(content: String): String {
        val systemPrompt = "你是一个温柔的情绪支持助手，专门帮助产后妈妈。请用50-80字给予共情回应，不要说教，避免使用'应该''必须'等词语。"

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
        return if (existing == null) {
            userInteractionDao.insertInteraction(
                UserInteractionEntity(messageId, "like")
            )
            messageDao.incrementLikes(messageId)
            true // 已点赞
        } else {
            userInteractionDao.deleteInteraction(messageId, "like")
            messageDao.decrementLikes(messageId)
            false // 已取消
        }
    }

    // ── 共鸣 / 取消共鸣 ──────────────────────────────────────────────────────

    suspend fun toggleResonance(messageId: Long): Boolean {
        val existing = userInteractionDao.getInteraction(messageId, "resonance")
        return if (existing == null) {
            userInteractionDao.insertInteraction(
                UserInteractionEntity(messageId, "resonance")
            )
            messageDao.incrementResonances(messageId)
            true
        } else {
            userInteractionDao.deleteInteraction(messageId, "resonance")
            messageDao.decrementResonances(messageId)
            false
        }
    }

    // ── 发布回复 ──────────────────────────────────────────────────────────────

    suspend fun postReply(
        messageId: Long,
        content: String,
        author: String,
        isAnonymous: Boolean
    ): Long {
        val entity = ReplyEntity(
            messageId = messageId,
            content = content,
            author = author,
            isAnonymous = isAnonymous
        )
        return replyDao.insertReply(entity)
    }
    // ── 查询单条留言（含回复，响应式）──────────────────────────────────────────

    fun observeMessageWithReplies(messageId: Long): Flow<Message?> {
        val repliesFlow = replyDao.observeRepliesByMessageId(messageId)

        return repliesFlow.map { replyEntities ->
            val messageEntity = messageDao.getMessageById(messageId) ?: return@map null
            val likedIds = userInteractionDao.getLikedMessageIds().toSet()
            val resonatedIds = userInteractionDao.getResonatedMessageIds().toSet()

            val replies = replyEntities.map { entity ->
                Reply(
                    id = entity.id,
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
        return Message(
            id = id,
            content = content,
            author = author,
            category = MessageCategory.valueOf(category),
            tags = tagList,
            timestamp = Date(timestamp),
            likes = likes,
            resonances = resonances,
            replies = replies,
            isAnonymous = isAnonymous,
            aiResponse = aiResponse,
            isCrisis = isCrisis,
            isHidden = isHidden,
            isLiked = isLiked,
            isResonated = isResonated
        )
    }
}
