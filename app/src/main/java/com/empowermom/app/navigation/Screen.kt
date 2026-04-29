package com.empowermom.app.navigation

/**
 * 应用内所有导航路由定义
 * 对应产品功能方案中的5大功能模块
 */
sealed class Screen(val route: String) {

    // ── 留言板 (P0) ──────────────────────────────────────────────────────────
    data object MessageBoard : Screen("message_board")
    data object MessageDetail : Screen("message_detail/{messageId}") {
        fun createRoute(messageId: Long) = "message_detail/$messageId"
    }

    // ── 数字心理沙盘 ──────────────────────────────────────────────────────────
    data object Sandbox : Screen("sandbox")
    data object SandboxResult : Screen("sandbox_result/{sessionId}") {
        fun createRoute(sessionId: Long) = "sandbox_result/$sessionId"
    }

    // ── 每日状态速记 ──────────────────────────────────────────────────────────
    data object DailyLog : Screen("daily_log")
    data object WeeklySummary : Screen("weekly_summary")

    // ── 微关怀推送 ────────────────────────────────────────────────────────────
    data object Notification : Screen("notification")

    // ── 家庭协同 ──────────────────────────────────────────────────────────────
    data object Family : Screen("family")
}

/**
 * 底部导航栏 Tab 定义
 */
enum class BottomNavItem(
    val screen: Screen,
    val labelResId: Int,
    val iconDescription: String
) {
    MESSAGE_BOARD(Screen.MessageBoard, 0, "留言板"),
    SANDBOX(Screen.Sandbox, 1, "沙盘"),
    DAILY_LOG(Screen.DailyLog, 2, "记录"),
    FAMILY(Screen.Family, 3, "家庭"),
}
