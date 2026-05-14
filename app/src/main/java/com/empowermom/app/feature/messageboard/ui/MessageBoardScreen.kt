package com.empowermom.app.feature.messageboard.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collectLatest
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.messageboard.model.MessageCategory
import com.empowermom.app.feature.messageboard.viewmodel.MessageBoardIntent
import com.empowermom.app.feature.messageboard.viewmodel.MessageBoardViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

// ─────────────────────────────────────────────────────────────────────────────
// 留言板主屏幕
// 对应 HTML: header + main + footer 结构
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBoardScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: MessageBoardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // 监听跳转事件：危机帖发布后自动跳转到详情页
    LaunchedEffect(Unit) {
        viewModel.navigateToDetail.collectLatest { messageId ->
            onNavigateToDetail(messageId)
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.messages.isEmpty() && !uiState.isLoading) {
            viewModel.debugInsertMockData()
        }
    }

    Scaffold(
        // ── 顶部导航栏 ──────────────────────────────────────────────────────────
        topBar = {
            MessageBoardTopBar(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    viewModel.handleIntent(MessageBoardIntent.SelectCategory(category))
                }
            )
        },
        // ── 底部「写心事」按钮 ────────────────────────────────────────────────
        bottomBar = {
            WriteMessageBottomBar(
                onClick = { viewModel.handleIntent(MessageBoardIntent.OpenEditor) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        errorMessage = uiState.errorMessage!!,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(paddingValues),
                        onRetry = { viewModel.handleIntent(MessageBoardIntent.Retry) }
                    )
                }

                uiState.messages.isEmpty() && !uiState.isLoading -> {
                    EmptyState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(paddingValues),
                        onWriteClick = { viewModel.handleIntent(MessageBoardIntent.OpenEditor) }
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.handleIntent(MessageBoardIntent.Refresh) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 24.dp,
                                bottom = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { BoardHeader() }
                            items(
                                items = uiState.messages,
                                key = { it.id }
                            ) { message ->
                                MessageCard(
                                    message = message,
                                    onLikeClick = {
                                        viewModel.handleIntent(MessageBoardIntent.ToggleLike(message.id))
                                    },
                                    onResonanceClick = {
                                        viewModel.handleIntent(MessageBoardIntent.ToggleResonance(message.id))
                                    },
                                    onReplyClick = { onNavigateToDetail(message.id) },
                                    onCardClick = { onNavigateToDetail(message.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    // ── 半屏编辑器（写留言）────────────────────────────────────────────
    if (uiState.isEditorOpen) {
        MessageEditor(
            editorState = uiState.editorState,
            presetTags = viewModel.presetTags,
            onDismiss = { viewModel.handleIntent(MessageBoardIntent.CloseEditor) },
            onContentChange = { viewModel.handleIntent(MessageBoardIntent.UpdateEditorContent(it)) },
            onCategorySelect = { viewModel.handleIntent(MessageBoardIntent.SelectEditorCategory(it)) },
            onTagToggle = { viewModel.handleIntent(MessageBoardIntent.ToggleTag(it)) },
            onAnonymousChange = { viewModel.handleIntent(MessageBoardIntent.SetAnonymous(it)) },
            onNicknameChange = { viewModel.handleIntent(MessageBoardIntent.UpdateNickname(it)) },
            onSubmit = { viewModel.handleIntent(MessageBoardIntent.SubmitMessage) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 顶部导航栏：Logo + 分类 Tab（对应 HTML header）
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageBoardTopBar(
    selectedCategory: MessageCategory?,
    onCategorySelected: (MessageCategory?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
    ) {
        // Logo 行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 圆形 Logo：对应 HTML 的 border rounded-full
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "暖心留言板",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "暖心留言板",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        // 分类 Tab 行（对应 HTML 的 nav tabs）
        ScrollableTabRow(
            selectedTabIndex = selectedCategory?.ordinal?.plus(1) ?: 0,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = {}
        ) {
            // "全部" Tab
            Tab(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                text = {
                    Text(
                        "全部",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
            // 各分类 Tab
            MessageCategory.entries.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    text = {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }

        // 分隔线
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 标题区（对应 HTML 的 "妈妈们的分享" 标题区域）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoardHeader() {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "妈妈们的分享",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        // 装饰性短横线（对应 HTML 的 line-element w-24）
        HorizontalDivider(
            modifier = Modifier.width(96.dp),
            color = MaterialTheme.colorScheme.primary,
            thickness = 1.dp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 留言卡片（对应 HTML 的 message-card）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MessageCard(
    message: Message,
    onLikeClick: () -> Unit,
    onResonanceClick: () -> Unit,
    onReplyClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (message.isCrisis) EmpowerMomColors.CrisisRed
    else MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (message.isCrisis) 2.dp else 0.5.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── 作者信息行 ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像 + 名字 + 时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = message.author,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = formatRelativeTime(message.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                // 分类标签（对应 HTML 的右上角分区badge）
                Text(
                    text = message.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // 分隔线
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp
            )

            // ── 留言内容 ──────────────────────────────────────────────────────
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            // ── 标签列表 ──────────────────────────────────────────────────────
            if (message.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    message.tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 互动按钮区 ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // 👍 点赞
                    InteractionButton(
                        icon = if (message.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        count = message.likes,
                        isActive = message.isLiked,
                        onClick = onLikeClick
                    )
                    // 💡 我也经历过（共鸣）
                    InteractionButton(
                        icon = if (message.isResonated) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                        count = message.resonances,
                        isActive = message.isResonated,
                        onClick = onResonanceClick
                    )
                }
                // 💬 回复
                TextButton(
                    onClick = onReplyClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "回复",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "回复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // — AI 回应区块（对应 HTML 的灰色背景区）-
            // 不再判断 aiResponse 是否为空——空时显示 Loading 态，避免空白
            Spacer(modifier = Modifier.height(12.dp))
            AiResponseBlock(response = message.aiResponse)

            // ── 危机干预提示 ──────────────────────────────────────────────────
            if (message.isCrisis) {
                Spacer(modifier = Modifier.height(8.dp))
                CrisisInterventionBlock()
            }
        }
    }
}

@Composable
private fun InteractionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val color = if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun AiResponseBlock(response: String) {
    val isLoading = response.isBlank()

    // Loading 态的呼吸动画：透明度在 0.3-1.0 之间循环，像在思考
    val infiniteTransition = rememberInfiniteTransition(label = "ai_loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_loading_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "AI",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AI 回应",
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            // Loading 态：温柔的等待文案 + 呼吸动画
            Text(
                text = "AI 正在认真思考你的话……",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.alpha(alpha)
            )
        } else {
            // 已有回应：显示真实内容
            Text(
                text = response,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun CrisisInterventionBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmpowerMomColors.CrisisBg)
            .padding(12.dp)
    ) {
        Text(
            text = "💙 你不是一个人",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "如果你需要帮助，请拨打心理援助热线：400-161-9995",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 底部「写心事」按钮（对应 HTML footer）
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WriteMessageBottomBar(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "写心事",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onWriteClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有留言",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "写下第一条吧，让其他妈妈感受到你",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onWriteClick,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("写下第一条", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onRetry,
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "重试",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────────────────────

fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000}小时前"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}天前"
        else -> SimpleDateFormat("MM-dd", Locale.CHINA).format(date)
    }
}
