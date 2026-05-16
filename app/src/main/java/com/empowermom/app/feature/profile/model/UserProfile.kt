package com.empowermom.app.feature.profile.model

// ── 用户资料数据类 ────────────────────────────────────────────────────────────

data class UserProfile(
    val nickname: String = "",
    val avatarEmoji: String = "🌸",   // 从预设 emoji 中选择，无需上传图片
    val babyAgeDays: Int = 0,          // 宝宝月龄（天数），用于个性化内容
    val isLoggedIn: Boolean = false    // 是否已完成资料填写
)

// 可选头像 emoji 列表
object AvatarOptions {
    val emojis = listOf(
        "🌸", "🌺", "🌼", "🌻", "🌹",
        "🫧", "🍀", "🌷", "🦋", "🌙",
        "⭐", "🌈", "🍓", "🧁", "🎀"
    )
}
