package com.empowermom.app.feature.messageboard.model

import java.util.Date

/**
 * 留言板核心数据模型
 *
 * 对应产品功能：
 * - 支持4个分区：情绪树洞 / 育儿求助 / 经验分享 / 身材恢复
 * - 匿名发布
 * - 标签系统
 * - AI 回应
 * - 危机内容识别
 */
data class Message(
    val id: Long = 0,
    val content: String,
    val author: String,
    val category: MessageCategory,
    val tags: List<String> = emptyList(),
    val timestamp: Date = Date(),
    val likes: Int = 0,
    val resonances: Int = 0,   // "我也经历过" 共鸣数
    val replies: List<Reply> = emptyList(),
    val isAnonymous: Boolean = true,
    val aiResponse: String = "",
    val isCrisis: Boolean = false,   // 危机内容标记
    val isHidden: Boolean = false,   // 危机内容仅自己可见
    val isLiked: Boolean = false,    // 当前用户是否已点赞
    val isResonated: Boolean = false // 当前用户是否已共鸣
)

data class Reply(
    val id: Long = 0,
    val messageId: Long,
    val content: String,
    val author: String,
    val timestamp: Date = Date(),
    val isAnonymous: Boolean = true
)

/**
 * 留言分区枚举
 * 对应 HTML 中的 Tab 分区
 */
enum class MessageCategory(val displayName: String, val iconDescription: String) {
    EMOTION("情绪树洞", "heart"),
    PARENTING("育儿求助", "question"),
    EXPERIENCE("经验分享", "lightbulb"),
    RECOVERY("身材恢复", "child");

    companion object {
        fun fromValue(value: String): MessageCategory? =
            entries.find { it.name.lowercase() == value.lowercase() }
    }
}

/**
 * 留言板筛选状态
 */
data class MessageFilter(
    val category: MessageCategory? = null,  // null 表示"全部"
    val tag: String? = null
)

/**
 * 预置标签列表（对应 HTML 中的 tag-pill）
 */
object PresetTags {
    val all = listOf(
        "宝宝肠胀气",
        "产后脱发",
        "婆媳关系",
        "睡眠不足",
        "母乳喂养困难",
        "产后抑郁",
        "身材焦虑",
        "育儿困惑"
    )
}

/**
 * 危机关键词（用于自动识别需要干预的内容）
 */
object CrisisKeywords {
    val keywords = listOf(
        "自杀", "想死", "不想活", "太累了不想撑",
        "撑不下去", "伤害自己", "绝望", "无助",
        "活不下去", "消失"
    )

    fun detect(content: String): Boolean =
        keywords.any { content.contains(it) }
}
