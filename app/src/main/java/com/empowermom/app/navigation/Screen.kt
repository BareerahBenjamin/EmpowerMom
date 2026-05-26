package com.empowermom.app.navigation

sealed class Screen(val route: String) {

    // ── 留言板 (P0) ──────────────────────────────────────────────────────────
    data object MessageBoard : Screen("message_board")
    data object MessageDetail : Screen("message_detail/{messageId}") {
        fun createRoute(messageId: Long) = "message_detail/$messageId"
    }

    // ── 数字心理沙盘 ──────────────────────────────────────────────────────────
    data object Sandbox : Screen("sandbox")

    // ── 每日状态速记 ──────────────────────────────────────────────────────────
    data object DailyLog : Screen("daily_log")

    // ── 我的（个人资料 / 登录）────────────────────────────────────────────────
    data object Profile : Screen("profile")

    // ── 我的记录（速记/留言/隐私）─────────────────────────────────────────────
    data object MyHistory : Screen("my_history")

    // ── 家庭协同（P2 占位）────────────────────────────────────────────────────
    data object Family : Screen("family")
}
