package com.empowermom.app.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.profile.model.AvatarOptions
import com.empowermom.app.feature.profile.viewmodel.ProfileIntent
import com.empowermom.app.feature.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmpowerMomColors.Rose)
        }
        return
    }

    if (uiState.isEditing) {
        EditProfileScreen(
            nickname      = uiState.editNickname,
            selectedAvatar = uiState.editAvatar,
            babyAgeDays   = uiState.editBabyAgeDays,
            nicknameError = uiState.nicknameError,
            isFirstTime   = !uiState.profile.isLoggedIn,
            onNicknameChange    = { viewModel.handleIntent(ProfileIntent.UpdateNickname(it)) },
            onAvatarChange      = { viewModel.handleIntent(ProfileIntent.UpdateAvatar(it)) },
            onBabyAgeDaysChange = { viewModel.handleIntent(ProfileIntent.UpdateBabyAgeDays(it)) },
            onSave   = { viewModel.handleIntent(ProfileIntent.SaveProfile) },
            onCancel = { viewModel.handleIntent(ProfileIntent.CancelEdit) }
        )
    } else {
        MyProfileScreen(
            profile  = uiState.profile,
            onEdit   = { viewModel.handleIntent(ProfileIntent.StartEdit) },
            onLogout = { viewModel.handleIntent(ProfileIntent.Logout) }
        )
    }
}

// ── 填写/编辑资料页 ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileScreen(
    nickname: String,
    selectedAvatar: String,
    babyAgeDays: Int,
    nicknameError: String?,
    isFirstTime: Boolean,
    onNicknameChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit,
    onBabyAgeDaysChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFirstTime) "欢迎来到 EmpowerMom 🌷" else "编辑资料") },
                navigationIcon = {
                    if (!isFirstTime) {
                        IconButton(onClick = onCancel) {
                            Text("✕", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 欢迎语（首次）
            if (isFirstTime) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmpowerMomColors.PeachPale)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "这里是你的专属空间 💛",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "填写简单的资料，让我们更好地陪伴你。你的信息仅存在本设备上，完全私密。",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmpowerMomColors.TextMid
                        )
                    }
                }
            }

            // ── 选择头像 ──────────────────────────────────────────────────────
            SectionCard(title = "选择你的头像") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AvatarOptions.emojis) { emoji ->
                        val isSelected = emoji == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) EmpowerMomColors.PeachPale
                                    else MaterialTheme.colorScheme.background
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) EmpowerMomColors.Rose
                                            else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onAvatarChange(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            }

            // ── 昵称 ──────────────────────────────────────────────────────────
            SectionCard(title = "你的昵称") {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：小橙子妈妈", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    isError = nicknameError != null,
                    supportingText = {
                        if (nicknameError != null) {
                            Text(nicknameError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("${nickname.length}/12", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmpowerMomColors.Peach,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // ── 宝宝月龄 ──────────────────────────────────────────────────────
            SectionCard(title = "宝宝多大了？（可选）") {
                val months = babyAgeDays / 30
                Text(
                    text = if (babyAgeDays == 0) "未设置" else "${months} 个月",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmpowerMomColors.Rose,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = babyAgeDays.toFloat(),
                    onValueChange = { onBabyAgeDaysChange(it.toInt()) },
                    valueRange = 0f..730f,   // 0–24 个月
                    steps = 23,              // 每步 ~30 天
                    colors = SliderDefaults.colors(
                        thumbColor = EmpowerMomColors.Rose,
                        activeTrackColor = EmpowerMomColors.Peach,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("刚出生", style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
                    Text("24 个月", style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
                }
            }

            // ── 保存按钮 ──────────────────────────────────────────────────────
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmpowerMomColors.Peach)
            ) {
                Text(
                    if (isFirstTime) "开始使用 EmpowerMom 🌸" else "保存修改",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── 个人主页 ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyProfileScreen(
    profile: com.empowermom.app.feature.profile.model.UserProfile,
    onEdit: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑资料",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 头像 & 昵称卡片 ────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(EmpowerMomColors.PeachPale, EmpowerMomColors.AmberPale)
                                )
                            )
                            .border(2.dp, EmpowerMomColors.PeachLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.avatarEmoji, fontSize = 36.sp)
                    }

                    Column {
                        Text(
                            profile.nickname,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        val babyText = if (profile.babyAgeDays > 0)
                            "宝宝 ${profile.babyAgeDays / 30} 个月啦 🍼"
                        else "还没设置宝宝月龄"
                        Text(
                            babyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmpowerMomColors.TextMid
                        )
                    }
                }
            }

            // ── 功能列表 ───────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    ProfileMenuItem(emoji = "📋", title = "我的速记", subtitle = "查看历史身心记录")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    ProfileMenuItem(emoji = "💬", title = "我的留言", subtitle = "查看发布过的留言")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    ProfileMenuItem(emoji = "🔒", title = "隐私说明", subtitle = "数据仅存在本设备，完全私密")
                }
            }

            // ── 关于 ───────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmpowerMomColors.PeachPale)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "EmpowerMom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = EmpowerMomColors.Rose
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "为产后 0–24 个月妈妈设计的身心关怀工具。\n你不是一个人在战斗 💛",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmpowerMomColors.TextMid,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── 退出登录 ───────────────────────────────────────────────────────
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline
                )
            ) {
                Text("重置资料", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 退出确认弹窗
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("重置资料") },
            text  = { Text("将清空昵称和头像设置，本地记录数据不会删除。确认吗？") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("确认", color = EmpowerMomColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── 通用组件 ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = EmpowerMomColors.TextMid,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun ProfileMenuItem(emoji: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextLight)
        }
        Text("›", fontSize = 18.sp, color = EmpowerMomColors.TextLight)
    }
}
