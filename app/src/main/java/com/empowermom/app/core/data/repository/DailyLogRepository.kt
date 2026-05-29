package com.empowermom.app.core.data.repository

import android.util.Log
import com.empowermom.app.BuildConfig
import com.empowermom.app.core.data.local.dao.DailyLogDao
import com.empowermom.app.core.data.local.entity.DailyLogEntity
import com.empowermom.app.core.data.remote.dto.DailyLogDto
import com.empowermom.app.core.network.DeepSeekApiService
import com.empowermom.app.core.network.DeepSeekMessage
import com.empowermom.app.core.network.DeepSeekRequest
import com.empowermom.app.feature.dailylog.model.DailyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dao: DailyLogDao,
    private val apiService: DeepSeekApiService,
    private val supabaseRepository: SupabaseRepository,
    private val authRepository: AuthRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "DailyLogRepository"

    // 同步状态，供 UI 观察
    private val _lastSyncResult = MutableStateFlow("")
    val lastSyncResult: StateFlow<String> = _lastSyncResult.asStateFlow()

    // 观察当前用户所有非私密记录（月历、时间线展示用，私密记录不显示）
    fun observeAll(): Flow<List<DailyLog>> {
        val userId = authRepository.currentUserId() ?: ""
        return dao.observePublicLogs(userId).map { list -> list.map { it.toDomain() } }
    }

    // 今天是否已记录
    suspend fun getTodayLog(): DailyLog? {
        val userId = authRepository.currentUserId() ?: return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end   = start + 86_400_000L
        return dao.getTodayLog(userId, start, end)?.toDomain()
    }

    // 保存记录，返回 id
    suspend fun save(log: DailyLog): Long {
        val userId = authRepository.currentUserId()
        val entity = log.toEntity(userId = userId)
        val localId = dao.insert(entity)
        _lastSyncResult.value = "正在同步..."

        // 后台同步到 Supabase（在协程内部获取 userId，确保 auth 已就绪）
        syncScope.launch {
            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    Log.w(TAG, "用户未登录，速记仅保存在本地 localId=$localId")
                    _lastSyncResult.value = "未登录，仅保存本地"
                    return@launch
                }
                val saved = dao.getById(localId) ?: return@launch
                val dto = DailyLogDto(
                    userId = userId,
                    date = saved.date,
                    q1Type = saved.q1Type,
                    q1Answer = saved.q1Answer,
                    q1Color = saved.q1Color,
                    q2Question = saved.q2Question,
                    q2Answer = saved.q2Answer,
                    q3Question = saved.q3Question,
                    q3Text = saved.q3Text,
                    aiCardText = saved.aiCardText,
                    isPrivate = saved.isPrivate
                )
                Log.d(TAG, "开始同步速记到 Supabase: userId=$userId, dto=$dto")
                val remoteDto = supabaseRepository.insertDailyLog(dto)
                dao.markSynced(localId, remoteDto.id)
                Log.d(TAG, "速记同步成功 localId=$localId → remoteId=${remoteDto.id}")
                _lastSyncResult.value = "同步成功 ✓"
            } catch (e: Exception) {
                Log.e(TAG, "速记同步失败 localId=$localId: ${e.message}", e)
                e.printStackTrace()
                _lastSyncResult.value = "同步失败: ${e.message}"
            }
        }

        return localId
    }

    // 更新 AI 卡片文案
    suspend fun updateAiCardText(id: Long, text: String) {
        dao.updateAiCardText(id, text)

        // 同步到 Supabase
        syncScope.launch {
            try {
                val entity = dao.getById(id) ?: return@launch
                val remoteId = entity.remoteId ?: return@launch
                supabaseRepository.updateDailyLogAiText(remoteId, text)
                Log.d(TAG, "AI 文案同步成功 id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "AI 文案同步失败: ${e.message}", e)
            }
        }
    }

    // 切换私密状态
    suspend fun togglePrivacy(id: Long, isPrivate: Boolean) {
        dao.updatePrivacy(id, isPrivate)

        syncScope.launch {
            try {
                val entity = dao.getById(id) ?: return@launch
                val remoteId = entity.remoteId ?: return@launch
                supabaseRepository.updateDailyLogPrivacy(remoteId, isPrivate)
                Log.d(TAG, "私密状态同步成功 id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "私密状态同步失败: ${e.message}", e)
            }
        }
    }

    // 获取当前用户最近 7 条非私密记录，用于本周小结
    suspend fun getRecentLogs(): List<DailyLog> {
        val userId = authRepository.currentUserId() ?: return emptyList()
        return dao.getRecentPublicLogs(userId).map { it.toDomain() }
    }

    // ── 从远程拉取速记数据 ──────────────────────────────────────────────

    suspend fun refreshFromRemote() {
        try {
            val userId = authRepository.currentUserId() ?: return

            // 切换账号时清理其他用户的数据
            dao.deleteOtherUsersData(userId)

            val remoteLogs = supabaseRepository.fetchDailyLogs(userId)
            Log.d(TAG, "拉取到 ${remoteLogs.size} 条远程速记")

            for (dto in remoteLogs) {
                val existing = dao.getByRemoteId(dto.id)
                if (existing != null) {
                    // 更新已有记录
                    val updated = existing.copy(
                        aiCardText = dto.aiCardText,
                        isPrivate = dto.isPrivate,
                        syncStatus = "synced"
                    )
                    dao.insert(updated)
                } else {
                    // 插入远程记录到本地
                    val entity = DailyLogEntity(
                        remoteId = dto.id,
                        userId = dto.userId,
                        date = dto.date,
                        q1Type = dto.q1Type,
                        q1Answer = dto.q1Answer,
                        q1Color = dto.q1Color,
                        q2Question = dto.q2Question,
                        q2Answer = dto.q2Answer,
                        q3Question = dto.q3Question,
                        q3Text = dto.q3Text,
                        aiCardText = dto.aiCardText,
                        isPrivate = dto.isPrivate,
                        syncStatus = "synced"
                    )
                    dao.insert(entity)
                }
            }

            // 同步本地未同步的记录
            val unsynced = dao.getUnsyncedLogs()
            for (entity in unsynced) {
                try {
                    val dto = DailyLogDto(
                        userId = userId,
                        date = entity.date,
                        q1Type = entity.q1Type,
                        q1Answer = entity.q1Answer,
                        q1Color = entity.q1Color,
                        q2Question = entity.q2Question,
                        q2Answer = entity.q2Answer,
                        q3Question = entity.q3Question,
                        q3Text = entity.q3Text,
                        aiCardText = entity.aiCardText,
                        isPrivate = entity.isPrivate
                    )
                    val remoteDto = supabaseRepository.insertDailyLog(dto)
                    dao.markSynced(entity.id, remoteDto.id)
                    Log.d(TAG, "本地速记补同步成功 id=${entity.id} → remoteId=${remoteDto.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "本地速记补同步失败 id=${entity.id}: ${e.message}", e)
                }
            }

            Log.d(TAG, "速记远程同步完成")
        } catch (e: Exception) {
            Log.e(TAG, "拉取远程速记失败: ${e.message}", e)
            throw e
        }
    }

    // 调用 AI 生成今日卡片文案
    suspend fun generateCardText(
        q1Type: String, q1Answer: String,
        q2Question: String, q2Answer: String,
        q3Question: String, q3Text: String
    ): String {
        val prompt = """
你是一位温暖贴心的产后妈妈陪伴者，像最懂她的闺蜜一样说话。妈妈今天记录了自己的状态，你需要写一段话送给她。

要求：
- 60-100字，温暖、真诚、有情感共鸣
- 先看见她的情绪（不管好坏都接纳），再给一句轻柔的陪伴
- 用"你"称呼，像朋友聊天，不要任何说教
- 可以适当用1个emoji，但不要堆砌
- 禁止出现：'你应该''建议你''加油''要坚强'等指令或口号
- 禁止省略号结尾，禁止敷衍，禁止空泛套话
- 写完整的话，不要截断

妈妈今天的记录：
- 状态类型：$q1Type → $q1Answer
- $q2Question → $q2Answer
- $q3Question：${q3Text.ifBlank { "她没有写更多，也许今天有些疲惫" }}

请直接输出你想对她说的话，不要加任何前缀或引号。
        """.trimIndent()

        return try {
            val response = apiService.chatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = DeepSeekRequest(
                    model = "deepseek-chat",
                    messages = listOf(DeepSeekMessage("user", prompt)),
                    maxTokens = 200
                )
            )
            response.choices.firstOrNull()?.message?.content?.trim()
                ?: "今天的你也很了不起，每一步都算数，我在你身边 💛"
        } catch (e: Exception) {
            "今天的你也很了不起，每一步都算数，我在你身边 💛"
        }
    }

    // 调用 AI 生成本周小结
    suspend fun generateWeeklySummary(logs: List<DailyLog>): String {
        if (logs.isEmpty()) return ""
        val detail = logs.joinToString("\n") { log ->
            "状态：${log.q1Type}(${log.q1Answer})，${log.q2Question}(${log.q2Answer})"
        }
        val prompt = """
你是一位温暖贴心的产后妈妈陪伴者，像最懂她的闺蜜一样。妈妈记录了一周的速记，请为她写一段本周小结。

要求：
- 120-180字，温暖真诚，有洞察力
- 先回顾她这一周的情绪变化（看到她的努力和不容易），再给一句鼓励
- 用"你"称呼，像朋友在聊天，不要说教
- 可以适当用1个emoji
- 禁止出现：'你应该''建议你''加油''要坚强'等指令或口号
- 禁止省略号结尾，禁止空泛套话
- 写完整的话

她这一周的记录：
$detail

请直接输出你想对她说的本周小结，不要加任何标题或前缀。
        """.trimIndent()

        return try {
            val response = apiService.chatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = DeepSeekRequest(
                    model = "deepseek-chat",
                    messages = listOf(DeepSeekMessage("user", prompt)),
                    maxTokens = 300
                )
            )
            response.choices.firstOrNull()?.message?.content?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // ── 映射函数 ─────────────────────────────────────────────────────────────

    private fun DailyLogEntity.toDomain() = DailyLog(
        id = id, date = date,
        q1Type = q1Type, q1Answer = q1Answer, q1Color = q1Color,
        q2Question = q2Question, q2Answer = q2Answer,
        q3Question = q3Question, q3Text = q3Text,
        aiCardText = aiCardText, isPrivate = isPrivate
    )

    private fun DailyLog.toEntity(userId: String? = null) = DailyLogEntity(
        id = id, date = date,
        q1Type = q1Type, q1Answer = q1Answer, q1Color = q1Color,
        q2Question = q2Question, q2Answer = q2Answer,
        q3Question = q3Question, q3Text = q3Text,
        aiCardText = aiCardText, isPrivate = isPrivate,
        userId = userId
    )
}
