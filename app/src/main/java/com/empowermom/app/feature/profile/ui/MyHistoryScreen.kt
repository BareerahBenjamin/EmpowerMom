package com.empowermom.app.feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.dailylog.model.QuestionBank
import com.empowermom.app.feature.dailylog.model.DailyLog
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.profile.model.UserProfile
import com.empowermom.app.feature.profile.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMessageDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("我的速记", "我的留言", "隐私说明")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的记录", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmpowerMomColors.Rose,
                indicator = {},
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) EmpowerMomColors.PeachPale
                                else Color.Transparent
                            ),
                        selectedContentColor = EmpowerMomColors.RoseDark,
                        unselectedContentColor = MaterialTheme.colorScheme.secondary,
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> MyLogsTab(logs = uiState.dailyLogs)
                1 -> MyMessagesTab(
                    messages = uiState.messages,
                    userProfile = uiState.userProfile,
                    onMessageClick = onNavigateToMessageDetail
                )
                2 -> PrivacyTab()
            }
        }
    }
}

// ── 我的速记 Tab ──────────────────────────────────────────────────────────────

@Composable
private fun MyLogsTab(logs: List<DailyLog>) {
    if (logs.isEmpty()) {
        EmptyHint(icon = "📋", text = "还没有速记记录", sub = "去「每日速记」记录今天的心情吧")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            DailyLogCard(log)
        }
    }
}

@Composable
private fun DailyLogCard(log: DailyLog) {
    val dateStr = remember(log.date) {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date(log.date))
    }
    val cardBg = QuestionBank.colorFromToken(log.q1Color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(0.5.dp, EmpowerMomColors.PeachLight)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateStr, style = MaterialTheme.typography.labelMedium, color = EmpowerMomColors.TextMid)
                if (log.isPrivate) Text("🔒", fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            LogRow("💭 ${log.q1Type}", log.q1Answer)
            LogRow("📊 生活", log.q2Answer)
            if (log.q3Text.isNotBlank()) {
                LogRow("🌟 ${log.q3Question}", log.q3Text)
            }
            if (log.aiCardText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        log.aiCardText,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmpowerMomColors.TextMid,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun LogRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextMid, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── 我的留言 Tab ──────────────────────────────────────────────────────────────

@Composable
private fun MyMessagesTab(
    messages: List<Message>,
    userProfile: UserProfile,
    onMessageClick: (Long) -> Unit
) {
    if (messages.isEmpty()) {
        EmptyHint(icon = "💬", text = "还没有留言", sub = "去留言板写下第一条心事吧")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MyMessageCard(message = message, onClick = { onMessageClick(message.id) })
        }
    }
}

@Composable
private fun MyMessageCard(message: Message, onClick: () -> Unit) {
    val dateStr = remember(message.timestamp) {
        SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(message.timestamp)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    message.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmpowerMomColors.Rose,
                    modifier = Modifier
                        .border(0.5.dp, EmpowerMomColors.Rose, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (message.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.tags.forEach { tag ->
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("👍 ${message.likes}", style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
                Text("💡 ${message.resonances}", style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
                Text("💬 ${message.replies.size}", style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
            }
        }
    }
}

// ── 隐私说明 Tab ──────────────────────────────────────────────────────────────

@Composable
private fun PrivacyTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrivacySection(
            emoji = "🔒",
            title = "数据存储",
            content = "你的所有数据（速记、留言、个人资料）都存储在你的设备本地数据库中。即使没有网络，你也可以正常使用所有功能。"
        )
        PrivacySection(
            emoji = "☁️",
            title = "云端同步",
            content = "当你发布留言或互动时，数据会同步到 Supabase 云端，以便其他妈妈看到你的分享。同步过程使用加密连接，确保传输安全。"
        )
        PrivacySection(
            emoji = "👤",
            title = "匿名保护",
            content = "你可以选择以匿名身份发布留言。即使登录了账号，你的真实身份也不会展示给其他用户。你的昵称和头像仅用于个性化展示。"
        )
        PrivacySection(
            emoji = "🤖",
            title = "AI 回应",
            content = "星芽（AI 助手）会对你的留言生成温柔的回应。AI 回应由 DeepSeek 大模型生成，不涉及人工审核。请将 AI 回应视为参考，重要决定请咨询专业人士。"
        )
        PrivacySection(
            emoji = "📱",
            title = "设备权限",
            content = "应用可能请求相机、相册权限用于上传图片/视频。这些权限完全可选，你可以在系统设置中随时关闭。"
        )
        PrivacySection(
            emoji = "🗑️",
            title = "数据删除",
            content = "退出登录会清除个人资料设置，但不会删除本地速记数据。如需完全清除数据，可在应用信息中清除存储。"
        )
    }
}

@Composable
private fun PrivacySection(emoji: String, title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, fontSize = 20.sp)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextMid, lineHeight = 20.sp)
        }
    }
}

// ── 空状态 ────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyHint(icon: String, text: String, sub: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextLight)
        }
    }
}
