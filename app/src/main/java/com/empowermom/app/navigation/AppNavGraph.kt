package com.empowermom.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empowermom.app.feature.dailylog.ui.DailyLogScreen
import com.empowermom.app.feature.family.ui.FamilyScreen
import com.empowermom.app.feature.messageboard.ui.MessageBoardScreen
import com.empowermom.app.feature.messageboard.ui.MessageDetailScreen
import com.empowermom.app.feature.sandbox.ui.SandboxScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MessageBoard.route
    ) {
        // ── 留言板 ────────────────────────────────────────────────────────────
        composable(Screen.MessageBoard.route) {
            MessageBoardScreen(
                onNavigateToDetail = { messageId ->
                    navController.navigate(Screen.MessageDetail.createRoute(messageId))
                }
            )
        }

        composable(
            route = Screen.MessageDetail.route,
            arguments = listOf(navArgument("messageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getLong("messageId") ?: return@composable
            MessageDetailScreen(
                messageId = messageId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── 数字心理沙盘 ───────────────────────────────────────────────────────
        composable(Screen.Sandbox.route) {
            SandboxScreen()
        }

        // ── 每日状态速记 ───────────────────────────────────────────────────────
        composable(Screen.DailyLog.route) {
            DailyLogScreen()
        }

        // ── 家庭协同 ───────────────────────────────────────────────────────────
        composable(Screen.Family.route) {
            FamilyScreen()
        }
    }
}
