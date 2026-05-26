package com.empowermom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.core.ui.theme.EmpowerMomTheme
import com.empowermom.app.feature.auth.ui.AuthScreen
import com.empowermom.app.feature.auth.viewmodel.AuthViewModel
import com.empowermom.app.feature.profile.ui.ProfileScreen
import com.empowermom.app.feature.profile.viewmodel.ProfileViewModel
import com.empowermom.app.navigation.AppNavGraph
import com.empowermom.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

// 底部导航栏数据类
private data class NavTab(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmpowerMomTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.uiState.collectAsState()
                    val profileViewModel: ProfileViewModel = hiltViewModel()
                    val profileState by profileViewModel.uiState.collectAsState()

                    if (!authState.isAuthenticated) {
                        AuthScreen(
                            onLoginSuccess = {},
                            viewModel = authViewModel
                        )
                        return@Surface
                    }

                    // 登录后若尚未填写资料，先引导完成个人资料
                    if (!profileState.isLoading && !profileState.profile.isProfileComplete) {
                        ProfileScreen(viewModel = profileViewModel)
                        return@Surface
                    }

                    val navController = rememberNavController()
                    val currentBackStack by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStack?.destination?.route

                    // 详情页不显示底部导航
                    val showBottomBar = currentRoute != Screen.MessageDetail.route

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                empowerMomBottomNav(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo(Screen.MessageBoard.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        AppNavGraph(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun empowerMomBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val tabs = listOf(
        NavTab(Screen.MessageBoard.route, Icons.Outlined.ChatBubble,  "留言板"),
        NavTab(Screen.Sandbox.route,      Icons.Outlined.GridView,     "沙盘"),
        NavTab(Screen.DailyLog.route,     Icons.Outlined.NoteAlt,      "每日速记"),
        NavTab(Screen.Profile.route,      Icons.Outlined.Person,       "我的"),
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = EmpowerMomColors.Rose,
                    selectedTextColor   = EmpowerMomColors.Rose,
                    unselectedIconColor = EmpowerMomColors.TextLight,
                    unselectedTextColor = EmpowerMomColors.TextLight,
                    indicatorColor      = EmpowerMomColors.PeachPale
                )
            )
        }
    }
}
