package com.empowermom.app.feature.messageboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.messageboard.viewmodel.MessageDetailViewModel
import androidx.compose.foundation.layout.imePadding

import androidx.compose.ui.res.painterResource
import com.empowermom.app.R

/**
 * 留言详情页
 * 展示完整留言内容 + 全部回复列表 + 回复输入框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    messageId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MessageDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUserId = viewModel.currentUserId
    var replyText by remember { mutableStateOf("") }
    val replyFocusRequester = remember { FocusRequester() }
    var showDeleteMessageDialog by remember { mutableStateOf(false) }
    var deleteReplyId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(messageId) {
        viewModel.loadMessage(messageId)
    }

    // 删除留言确认弹窗
    if (showDeleteMessageDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMessageDialog = false },
            title = { Text("删除留言") },
            text = { Text("删除留言将同时删除所有回复，确定要继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteMessageDialog = false
                    viewModel.deleteMessage(messageId) { onNavigateBack() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMessageDialog = false }) { Text("取消") }
            }
        )
    }

    // 删除回复确认弹窗
    deleteReplyId?.let { replyId ->
        AlertDialog(
            onDismissRequest = { deleteReplyId = null },
            title = { Text("删除回复") },
            text = { Text("确定要删除这条回复吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReply(replyId)
                    deleteReplyId = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteReplyId = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("详情", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            // 回复输入框（固定在底部）
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                // 匿名开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (uiState.isAnonymous) "🦖" else userProfile.avatarEmoji,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (uiState.isAnonymous) "匿名回复" else "${userProfile.nickname.ifBlank { "momo" }} 回复",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isAnonymous,
                        onCheckedChange = { viewModel.toggleAnonymous(it) },
                        modifier = Modifier.height(28.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmpowerMomColors.Rose,
                            checkedTrackColor = EmpowerMomColors.PeachPale,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // 错误提示（仅在有错误时显示）
                uiState.replyError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = {
                            replyText = it
                            // 用户开始编辑时，清掉之前的错误提示
                            if (uiState.replyError != null) {
                                viewModel.clearReplyError()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(replyFocusRequester),
                        placeholder = { Text("写下你的回复...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        // 发送中禁用输入
                        enabled = !uiState.isPostingReply
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (replyText.isNotBlank() && !uiState.isPostingReply) {
                                viewModel.postReply(
                                    messageId = messageId,
                                    content = replyText,
                                    onSuccess = { replyText = "" }
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmpowerMomColors.Peach
                        ),
                        // 发送中或内容为空时禁用按钮
                        enabled = replyText.isNotBlank() && !uiState.isPostingReply
                    ) {
                        if (uiState.isPostingReply) {
                            Text("发送中...")
                        } else {
                            Text("发送")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 主留言卡片（完整内容，无截断）
            uiState.message?.let { message ->
                item {
                    MessageCard(
                        message = message,
                        userProfile = userProfile,
                        onLikeClick = { viewModel.toggleLike(messageId) },
                        onResonanceClick = { viewModel.toggleResonance(messageId) },
                        onReplyClick = { replyFocusRequester.requestFocus() },
                        onCardClick = {},
                        currentUserId = currentUserId,
                        onDeleteClick = { showDeleteMessageDialog = true }
                    )
                }

                // 危机帖：在评论区顶部置顶心理援助热线卡片
                if (message.isCrisis) {
                    item {
                        CrisisHotlineCard()
                    }
                }

                // 危机帖：给作者一行提示，让 ta 知道为什么帖子在列表看不到
                if (message.isCrisis) {
                    item {
                        Text(
                            text = "💗 这条留言因为含有让我们担心的词，暂时只有你能看到。我们想先把它好好接住。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }

                // 回复列表标题
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "全部回复 (${message.replies.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 回复项
                if (message.replies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无回复，来做第一个回复的人吧",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    items(message.replies) { reply ->
                        ReplyItem(
                            reply = reply,
                            userProfile = userProfile,
                            currentUserId = currentUserId,
                            onDeleteClick = { deleteReplyId = reply.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(
    reply: com.empowermom.app.feature.messageboard.model.Reply,
    userProfile: com.empowermom.app.feature.profile.model.UserProfile,
    currentUserId: String?,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 头像部分
        if (reply.author.trim() == "星芽") {
            Image(
                painter = painterResource(id = R.drawable.ic_xingya_avatar),
                contentDescription = "星芽头像",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        } else if (!reply.isAnonymous && reply.author == userProfile.nickname && userProfile.avatarEmoji.isNotBlank()) {
            // 非匿名且是自己的回复：显示用户头像
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EmpowerMomColors.PeachPale)
                    .then(
                        if (userProfile.avatarPhotoPath.isNotBlank()) Modifier
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile.avatarPhotoPath.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = userProfile.avatarPhotoPath,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(userProfile.avatarEmoji, fontSize = 16.sp)
                }
            }
        } else if (reply.isAnonymous) {
            // 匿名回复：显示恐龙 emoji
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EmpowerMomColors.PeachPale),
                contentAlignment = Alignment.Center
            ) {
                Text("🦖", fontSize = 16.sp)
            }
        } else {
            // 其他用户的非匿名回复：显示首字母
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    reply.author.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 内容部分
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reply.author, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatRelativeTime(reply.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (currentUserId != null && reply.userId == currentUserId) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "删除回复",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(reply.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun CrisisHotlineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题：温柔但能让人停下来
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "你不是一个人",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "如果你正在经历很难的时刻，请记得有人愿意听你说话：",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 遍历所有热线
            com.empowermom.app.feature.messageboard.model.CrisisHotlines.all.forEach { hotline ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hotline.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hotline.number,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = hotline.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
