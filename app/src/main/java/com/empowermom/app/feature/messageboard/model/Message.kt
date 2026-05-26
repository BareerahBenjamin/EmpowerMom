package com.empowermom.app.feature.messageboard.model

import java.util.Date

data class Message(
    val id: Long = 0,
    val userId: String? = null,
    val content: String,
    val author: String,
    val category: MessageCategory,
    val tags: List<String> = emptyList(),
    val attachments: List<MediaAttachment> = emptyList(),
    val timestamp: Date = Date(),
    val likes: Int = 0,
    val resonances: Int = 0,
    val replies: List<Reply> = emptyList(),
    val isAnonymous: Boolean = true,
    val aiResponse: String = "",
    val isCrisis: Boolean = false,
    val isHidden: Boolean = false,
    val isPrivateOnly: Boolean = false,
    val isLiked: Boolean = false,
    val isResonated: Boolean = false
)

data class MediaAttachment(
    val uri: String,
    val kind: MediaKind
)

enum class MediaKind {
    Image,
    Video
}

data class Reply(
    val id: Long = 0,
    val userId: String? = null,
    val messageId: Long,
    val content: String,
    val author: String,
    val timestamp: Date = Date(),
    val isAnonymous: Boolean = true
)

enum class MessageCategory(val displayName: String, val iconDescription: String) {
    EMOTION("情绪树洞", "heart"),
    MOM_HELP("妈妈互助", "hands"),
    FAMILY_RELATION("家庭关系", "home");

    companion object {
        fun fromValue(value: String): MessageCategory? =
            entries.find { it.name.lowercase() == value.lowercase() }
    }
}

data class MessageFilter(
    val category: MessageCategory? = null,
    val tag: String? = null
)

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

object CrisisKeywords {
    val keywords = listOf(
        "自杀", "想死", "不想活", "太累了不想撑",
        "撑不下去", "伤害自己", "绝望", "无助",
        "活不下去", "消失"
    )

    fun detect(content: String): Boolean =
        keywords.any { content.contains(it) }
}

object CrisisHotlines {
    data class Hotline(
        val name: String,
        val number: String,
        val description: String
    )

    val all = listOf(
        Hotline(
            name = "全国心理援助热线",
            number = "12356",
            description = "国家卫健委统一心理援助热线 · 24小时"
        ),
        Hotline(
            name = "希望24热线",
            number = "400-161-9995",
            description = "生命危机干预专线 · 24小时免费"
        )
    )
}
