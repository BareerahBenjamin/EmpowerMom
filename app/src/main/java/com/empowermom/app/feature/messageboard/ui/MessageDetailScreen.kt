package com.empowermom.app.feature.messageboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empowermom.app.feature.messageboard.viewmodel.MessageDetailViewModel

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
    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(messageId) {
        viewModel.loadMessage(messageId)
    }

    Scaffold(
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("写下你的回复...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                viewModel.postReply(messageId, replyText)
                                replyText = ""
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("发送")
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
                        onLikeClick = { viewModel.toggleLike(messageId) },
                        onResonanceClick = { viewModel.toggleResonance(messageId) },
                        onReplyClick = {},
                        onCardClick = {}
                    )
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
                        ReplyItem(reply = reply)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(reply: com.empowermom.app.feature.messageboard.model.Reply) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(reply.author, style = MaterialTheme.typography.titleSmall)
            Text(
                formatRelativeTime(reply.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(reply.content, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
    }
}
