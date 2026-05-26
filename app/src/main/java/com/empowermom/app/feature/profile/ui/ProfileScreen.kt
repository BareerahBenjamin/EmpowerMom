package com.empowermom.app.feature.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.profile.model.AvatarOptions
import com.empowermom.app.feature.profile.viewmodel.ProfileIntent
import com.empowermom.app.feature.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit = {}
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
            avatarPhotoPath = uiState.editAvatarPhotoPath,
            babyAgeDays   = uiState.editBabyAgeDays,
            nicknameError = uiState.nicknameError,
            isFirstTime   = !uiState.profile.isProfileComplete,
            accountEmail  = uiState.accountEmail,
            onNicknameChange    = { viewModel.handleIntent(ProfileIntent.UpdateNickname(it)) },
            onAvatarChange      = { viewModel.handleIntent(ProfileIntent.UpdateAvatar(it)) },
            onAvatarPhotoChange = { viewModel.handleIntent(ProfileIntent.UpdateAvatarPhoto(it)) },
            onBabyAgeDaysChange = { viewModel.handleIntent(ProfileIntent.UpdateBabyAgeDays(it)) },
            onSave   = { viewModel.handleIntent(ProfileIntent.SaveProfile) },
            onCancel = { viewModel.handleIntent(ProfileIntent.CancelEdit) }
        )
    } else {
        MyProfileScreen(
            profile      = uiState.profile,
            accountEmail = uiState.accountEmail,
            onEdit       = { viewModel.handleIntent(ProfileIntent.StartEdit) },
            onSignOut    = { viewModel.handleIntent(ProfileIntent.SignOut) },
            onNavigateToHistory = onNavigateToHistory
        )
    }
}

// ── 填写/编辑资料页 ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileScreen(
    nickname: String,
    selectedAvatar: String,
    avatarPhotoPath: String,
    babyAgeDays: Int,
    nicknameError: String?,
    isFirstTime: Boolean,
    accountEmail: String?,
    onNicknameChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit,
    onAvatarPhotoChange: (String) -> Unit,
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
                            "完善你的个人资料 💛",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "设置昵称和头像后，留言板会默认使用你的昵称。资料会保存在本设备并与账号同步使用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmpowerMomColors.TextMid
                        )
                        accountEmail?.let { email ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "当前账号：$email",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmpowerMomColors.Rose
                            )
                        }
                    }
                }
            }

            // ── 选择头像 ──────────────────────────────────────────────────────
            SectionCard(title = "你的头像") {
                val context = LocalContext.current
                val hasPhoto = avatarPhotoPath.isNotBlank()

                // 头像预览 + 上传按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像预览圆圈
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(EmpowerMomColors.PeachPale, EmpowerMomColors.AmberPale)
                                )
                            )
                            .border(2.dp, EmpowerMomColors.PeachLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto) {
                            val imageModel = if (avatarPhotoPath.startsWith("http")) {
                                avatarPhotoPath
                            } else {
                                java.io.File(avatarPhotoPath)
                            }
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(selectedAvatar, fontSize = 40.sp)
                        }
                    }

                    // 上传按钮
                    val photoPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            // 复制到 app 内部存储，保证持久访问
                            val file = java.io.File(context.filesDir, "avatar_photo.jpg")
                            context.contentResolver.openInputStream(it)?.use { input ->
                                file.outputStream().use { output -> input.copyTo(output) }
                            }
                            onAvatarPhotoChange(file.absolutePath)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPicker.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmpowerMomColors.Peach),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CameraAlt, contentDescription = null,
                                modifier = Modifier.size(18.dp), tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("上传照片", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        if (hasPhoto) {
                            TextButton(
                                onClick = { onAvatarPhotoChange("") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("清除照片，使用 emoji", style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextMid)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                // emoji 选择器
                Text(
                    "或选择 emoji 头像",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmpowerMomColors.TextLight,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AvatarOptions.emojis) { emoji ->
                        val isSelected = !hasPhoto && emoji == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(46.dp)
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
                            Text(emoji, fontSize = 22.sp)
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
    accountEmail: String?,
    onEdit: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

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
                        if (profile.avatarPhotoPath.isNotBlank()) {
                            val imageModel = if (profile.avatarPhotoPath.startsWith("http")) {
                                profile.avatarPhotoPath
                            } else {
                                java.io.File(profile.avatarPhotoPath)
                            }
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(profile.avatarEmoji, fontSize = 36.sp)
                        }
                    }

                    Column {
                        Text(
                            profile.nickname.ifBlank { "未设置昵称" },
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
                        accountEmail?.let { email ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                email,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmpowerMomColors.TextLight
                            )
                        }
                    }
                }
            }

            // ── 编辑资料入口 ───────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = EmpowerMomColors.Rose)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("编辑个人资料", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "修改昵称、头像和宝宝月龄",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmpowerMomColors.TextLight
                        )
                    }
                    Text("›", fontSize = 18.sp, color = EmpowerMomColors.TextLight)
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
                    ProfileMenuItem(emoji = "📋", title = "我的速记", subtitle = "查看历史身心记录", onClick = onNavigateToHistory)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    ProfileMenuItem(emoji = "💬", title = "我的留言", subtitle = "查看发布过的留言", onClick = onNavigateToHistory)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    ProfileMenuItem(emoji = "🔒", title = "隐私说明", subtitle = "数据仅存在本设备，完全私密", onClick = onNavigateToHistory)
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
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline
                )
            ) {
                Text("退出登录", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("退出登录") },
            text  = { Text("将退出当前账号并清空个人资料设置，本地速记数据不会删除。确认吗？") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) {
                    Text("确认", color = EmpowerMomColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
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
private fun ProfileMenuItem(emoji: String, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
