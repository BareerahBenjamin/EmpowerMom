package com.empowermom.app.feature.dailylog.model

import java.util.Date

// ── 领域模型 ──────────────────────────────────────────────────────────────────

data class DailyLog(
    val id: Long = 0,
    val date: Long = Date().time,          // 记录日期（当天 0 点时间戳）
    val q1Type: String = "",               // 核心状态类型，如"情绪天气"
    val q1Answer: String = "",             // Q1 选项文字
    val q1Color: String = "",              // 卡片背景色 token
    val q2Question: String = "",           // Q2 题目文字
    val q2Answer: String = "",             // Q2 选项文字
    val q3Question: String = "",           // Q3 题目文字
    val q3Text: String = "",               // Q3 自由输入内容
    val aiCardText: String = "",           // AI 生成的卡片文案
    val isPrivate: Boolean = false
)

// ── 问题数据类 ────────────────────────────────────────────────────────────────

data class QuestionOption(
    val text: String,
    val emoji: String,
    val colorToken: String = ""            // 只有 Q1 有颜色 token
)

data class CoreQuestion(
    val type: String,
    val title: String,
    val options: List<QuestionOption>
)

data class LifeQuestion(
    val title: String,
    val options: List<QuestionOption>
)

data class OpenQuestion(
    val title: String,
    val tip: String
)

// ── 问题库 ────────────────────────────────────────────────────────────────────

object QuestionBank {

    val coreQuestions = listOf(
        CoreQuestion(
            type = "情绪天气", title = "现在的内心天气是？",
            options = listOf(
                QuestionOption("晴朗",   "☀️", "sunny"),
                QuestionOption("多云",   "☁️", "cloudy"),
                QuestionOption("迷雾",   "🌫️", "fog"),
                QuestionOption("暴雨",   "⛈️", "rain"),
                QuestionOption("见彩虹", "🌈", "rainbow"),
            )
        ),
        CoreQuestion(
            type = "电量监测", title = "此时此刻你的电量？",
            options = listOf(
                QuestionOption("满格复活", "🔋", "full"),
                QuestionOption("红色预警", "🪫", "low"),
                QuestionOption("充电中",   "🔌", "charging"),
                QuestionOption("已关机",   "📴", "off"),
            )
        ),
        CoreQuestion(
            type = "体感色调", title = "如果今天有一种颜色？",
            options = listOf(
                QuestionOption("暖黄（温馨）",    "💡", "warm"),
                QuestionOption("冰蓝（冷静）",    "🧊", "cold"),
                QuestionOption("棕褐（沉重）",    "🪵", "heavy"),
                QuestionOption("嫩绿（轻盈）",    "🍏", "light"),
            )
        ),
        CoreQuestion(
            type = "压力值", title = "脑海里的杂音多吗？",
            options = listOf(
                QuestionOption("静音模式",   "🔇", "quiet"),
                QuestionOption("偶尔嘈杂",   "📻", "normal"),
                QuestionOption("震耳欲聋",   "📢", "loud"),
            )
        )
    )

    val lifeQuestions = listOf(
        LifeQuestion(
            title = "昨晚睡得怎么样？",
            options = listOf(
                QuestionOption("深度断片",   "😴"),
                QuestionOption("碎片拼接",   "😵‍💫"),
                QuestionOption("睁眼到天亮", "👁️"),
            )
        ),
        LifeQuestion(
            title = "今天有'自己'的时间吗？",
            options = listOf(
                QuestionOption("享受片刻",   "☕"),
                QuestionOption("挤出几分钟", "⏳"),
                QuestionOption("完全被占据", "🚫"),
            )
        ),
        LifeQuestion(
            title = "今天的重担有人分担吗？",
            options = listOf(
                QuestionOption("队友给力",   "🤝"),
                QuestionOption("孤军奋战",   "👤"),
                QuestionOption("想说但没开口", "🗨️"),
            )
        ),
        LifeQuestion(
            title = "今天的胃口还好吗？",
            options = listOf(
                QuestionOption("认真吃饭",   "😋"),
                QuestionOption("随便糊弄",   "🥖"),
                QuestionOption("忙到忘了吃", "🤢"),
            )
        )
    )

    val openQuestions = listOf(
        OpenQuestion("今日高光时刻", "今天哪一刻让你觉得自己挺厉害的？"),
        OpenQuestion("心动瞬间",     "今天哪个瞬间最让你触动？"),
        OpenQuestion("治愈小物",     "今天有什么让你感到被治愈的小物吗？"),
        OpenQuestion("自我放过",     "今天你允许自己哪件事没做好？"),
    )

    // Q1 颜色 token 映射为暖色主题对应颜色
    fun colorFromToken(token: String): androidx.compose.ui.graphics.Color {
        return when (token) {
            "sunny"   -> androidx.compose.ui.graphics.Color(0xFFFFF9E5)
            "cloudy"  -> androidx.compose.ui.graphics.Color(0xFFF5F7FA)
            "fog"     -> androidx.compose.ui.graphics.Color(0xFFF3F1FF)
            "rain"    -> androidx.compose.ui.graphics.Color(0xFFEAEAEA)
            "rainbow" -> androidx.compose.ui.graphics.Color(0xFFF8C8DC)
            "full"    -> androidx.compose.ui.graphics.Color(0xFFE8F5E9)
            "low"     -> androidx.compose.ui.graphics.Color(0xFFFFEBEE)
            "charging"-> androidx.compose.ui.graphics.Color(0xFFFFF8E1)
            "off"     -> androidx.compose.ui.graphics.Color(0xFFF5F5F5)
            "warm"    -> androidx.compose.ui.graphics.Color(0xFFFFF3E0)
            "cold"    -> androidx.compose.ui.graphics.Color(0xFFE3F2FD)
            "heavy"   -> androidx.compose.ui.graphics.Color(0xFFD7CCC8)
            "light"   -> androidx.compose.ui.graphics.Color(0xFFF1F8E9)
            "quiet"   -> androidx.compose.ui.graphics.Color(0xFFF3E5F5)
            "normal"  -> androidx.compose.ui.graphics.Color(0xFFFFE0B2)
            "loud"    -> androidx.compose.ui.graphics.Color(0xFFFFCDD2)
            else      -> androidx.compose.ui.graphics.Color(0xFFFEF0E4)
        }
    }
}