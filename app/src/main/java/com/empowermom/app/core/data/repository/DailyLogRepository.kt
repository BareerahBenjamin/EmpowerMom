package com.empowermom.app.core.data.repository

import com.empowermom.app.BuildConfig
import com.empowermom.app.core.data.local.dao.DailyLogDao
import com.empowermom.app.core.data.local.entity.DailyLogEntity
import com.empowermom.app.core.network.DeepSeekApiService
import com.empowermom.app.core.network.DeepSeekMessage
import com.empowermom.app.core.network.DeepSeekRequest
import com.empowermom.app.feature.dailylog.model.DailyLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dao: DailyLogDao,
    private val apiService: DeepSeekApiService
) {
    // 观察所有记录
    fun observeAll(): Flow<List<DailyLog>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    // 今天是否已记录
    suspend fun getTodayLog(): DailyLog? {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end   = start + 86_400_000L
        return dao.getTodayLog(start, end)?.toDomain()
    }

    // 保存记录，返回 id
    suspend fun save(log: DailyLog): Long =
        dao.insert(log.toEntity())

    // 更新 AI 卡片文案
    suspend fun updateAiCardText(id: Long, text: String) =
        dao.updateAiCardText(id, text)

    // 切换私密状态
    suspend fun togglePrivacy(id: Long, isPrivate: Boolean) =
        dao.updatePrivacy(id, isPrivate)

    // 获取最近 7 条，用于本周小结
    suspend fun getRecentLogs(): List<DailyLog> =
        dao.getRecentLogs().map { it.toDomain() }

    // 调用 AI 生成今日卡片文案
    suspend fun generateCardText(
        q1Type: String, q1Answer: String,
        q2Question: String, q2Answer: String,
        q3Question: String, q3Text: String
    ): String {
        val prompt = """
你是一个温柔的产后妈妈心理陪伴助手。根据妈妈今天的状态速记，生成一句简短温暖的鼓励（不超过50字，不要说教，语气像闺蜜）。

妈妈今天的记录：
- 状态类型：$q1Type → $q1Answer
- $q2Question → $q2Answer
- $q3Question：${q3Text.ifBlank { "未填写" }}

请直接输出鼓励的话，不要加任何前缀。
        """.trimIndent()

        return try {
            val response = apiService.chatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = DeepSeekRequest(
                    model = "deepseek-chat",
                    messages = listOf(DeepSeekMessage("user", prompt)),
                    maxTokens = 80
                )
            )
            response.choices.firstOrNull()?.message?.content?.trim()
                ?: "今天也辛苦了，你已经很棒了 💛"
        } catch (e: Exception) {
            "今天也辛苦了，你已经很棒了 💛"
        }
    }

    // 调用 AI 生成本周小结
    suspend fun generateWeeklySummary(logs: List<DailyLog>): String {
        if (logs.isEmpty()) return ""
        val detail = logs.joinToString("\n") { log ->
            "状态：${log.q1Type}(${log.q1Answer})，${log.q2Question}(${log.q2Answer})"
        }
        val prompt = """
你是一个温柔的产后妈妈心理陪伴助手。根据妈妈最近7天的速记，生成一段本周身心小结（100字以内，温暖，有洞察，像好朋友说话）。

近期记录：
$detail

请直接输出小结，不要加任何标题或前缀。
        """.trimIndent()

        return try {
            val response = apiService.chatCompletion(
                authorization = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = DeepSeekRequest(
                    model = "deepseek-chat",
                    messages = listOf(DeepSeekMessage("user", prompt)),
                    maxTokens = 150
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

    private fun DailyLog.toEntity() = DailyLogEntity(
        id = id, date = date,
        q1Type = q1Type, q1Answer = q1Answer, q1Color = q1Color,
        q2Question = q2Question, q2Answer = q2Answer,
        q3Question = q3Question, q3Text = q3Text,
        aiCardText = aiCardText, isPrivate = isPrivate
    )
}