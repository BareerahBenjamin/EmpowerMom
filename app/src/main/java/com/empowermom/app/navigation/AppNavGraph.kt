package com.empowermom.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.empowermom.app.feature.dailylog.ui.DailyLogScreen
import com.empowermom.app.feature.family.ui.FamilyScreen
import com.empowermom.app.feature.messageboard.ui.MessageBoardScreen
import com.empowermom.app.feature.messageboard.ui.MessageDetailScreen
import com.empowermom.app.feature.profile.ui.MyHistoryScreen
import com.empowermom.app.feature.profile.ui.ProfileScreen
import com.empowermom.app.feature.sandbox.ui.SandboxScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MessageBoard.route,
        modifier = modifier
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

        // ── 我的（个人资料 / 登录）────────────────────────────────────────────
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToHistory = {
                    navController.navigate(Screen.MyHistory.route)
                }
            )
        }

        // ── 我的记录 ────────────────────────────────────────────────────────
        composable(Screen.MyHistory.route) {
            MyHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMessageDetail = { messageId ->
                    navController.navigate(Screen.MessageDetail.createRoute(messageId))
                }
            )
        }

        // ── 家庭协同（P2 占位）────────────────────────────────────────────────
        composable(Screen.Family.route) {
            FamilyScreen()
        }
    }
}
