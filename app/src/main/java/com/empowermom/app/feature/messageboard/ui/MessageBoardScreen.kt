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
import com.empowermom.app.feature.messageboard.viewmodel.MessageBoardTab
import com.empowermom.app.feature.messageboard.viewmodel.MessageBoardViewModel
import com.empowermom.app.feature.profile.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.empowermom.app.feature.messageboard.model.MediaKind
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.empowermom.app.core.ui.theme.EmpowerMomTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.empowermom.app.R
import kotlin.math.roundToInt
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
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUserId = viewModel.currentUserId
    var deleteConfirmMessageId by remember { mutableStateOf<Long?>(null) }

    // 监听跳转事件：危机帖发布后自动跳转到详情页
    LaunchedEffect(Unit) {
        viewModel.navigateToDetail.collectLatest { messageId ->
            onNavigateToDetail(messageId)
        }
    }

    if (uiState.isSearchOpen) {
        MessageSearchDialog(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.handleIntent(MessageBoardIntent.UpdateSearchQuery(it)) },
            onSearch = { viewModel.handleIntent(MessageBoardIntent.ExecuteSearch) },
            onDismiss = { viewModel.handleIntent(MessageBoardIntent.CloseSearch) }
        )
    }

    // 删除确认弹窗
    deleteConfirmMessageId?.let { messageId ->
        AlertDialog(
            onDismissRequest = { deleteConfirmMessageId = null },
            title = { Text("删除留言") },
            text = { Text("确定要删除这条留言吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(MessageBoardIntent.DeleteMessage(messageId))
                    deleteConfirmMessageId = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmMessageId = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        // ── 顶部导航栏 ──────────────────────────────────────────────────────────
        topBar = {
            MessageBoardTopBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab -> viewModel.handleIntent(MessageBoardIntent.SelectTab(tab)) },
                onSearchClick = { viewModel.handleIntent(MessageBoardIntent.OpenSearch) }
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

        if (uiState.isSearchResultOpen) {
            SearchResultScreen(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                onBack = { viewModel.handleIntent(MessageBoardIntent.CloseSearchResult) },
                onNavigateToDetail = onNavigateToDetail,
                userProfile = userProfile
            )
            return@Scaffold
        }

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
                                top = 12.dp,
                                bottom = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // item { BoardHeader() }
                            items(
                                items = uiState.messages,
                                key = { it.id }
                            ) { message ->
                                MessageCard(
                                    message = message,
                                    userProfile = userProfile,
                                    onLikeClick = {
                                        viewModel.handleIntent(MessageBoardIntent.ToggleLike(message.id))
                                    },
                                    onResonanceClick = {
                                        viewModel.handleIntent(MessageBoardIntent.ToggleResonance(message.id))
                                    },
                                    onReplyClick = { onNavigateToDetail(message.id) },
                                    onCardClick = { onNavigateToDetail(message.id) },
                                    currentUserId = currentUserId,
                                    onDeleteClick = { deleteConfirmMessageId = message.id }
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
            onPrivateOnlyChange = { viewModel.handleIntent(MessageBoardIntent.SetPrivateOnly(it)) },
            onAddAttachment = { uri, mimeType ->
                viewModel.handleIntent(MessageBoardIntent.AddAttachment(uri, mimeType))
            },
            onRemoveAttachment = { uri ->
                viewModel.handleIntent(MessageBoardIntent.RemoveAttachment(uri))
            },
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
    selectedTab: MessageBoardTab,
    onTabSelected: (MessageBoardTab) -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 0.dp,
                color = Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // 分类 Tab 行（对应 HTML 的 nav tabs）
        ScrollableTabRow(
            selectedTabIndex = when (selectedTab) {
                MessageBoardTab.All -> 0
                MessageBoardTab.Private -> 1
                is MessageBoardTab.Category -> selectedTab.category.ordinal + 2
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp,
            divider = {},
            indicator = {}
        ) {
            val tabShape = RoundedCornerShape(14.dp)
            // "全部" Tab
            Tab(
                selected = selectedTab is MessageBoardTab.All,
                onClick = { onTabSelected(MessageBoardTab.All) },
                modifier = Modifier
                    .clip(tabShape)
                    .background(if (selectedTab is MessageBoardTab.All) EmpowerMomColors.PeachPale else Color.Transparent)
                    .border(
                        width = if (selectedTab is MessageBoardTab.All) 0.5.dp else 0.dp,
                        color = if (selectedTab is MessageBoardTab.All) EmpowerMomColors.Rose else Color.Transparent,
                        shape = tabShape
                    ),
                selectedContentColor = EmpowerMomColors.RoseDark,
                unselectedContentColor = MaterialTheme.colorScheme.secondary,
                text = {
                    Text(
                        "全部",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            Tab(
                selected = selectedTab is MessageBoardTab.Private,
                onClick = { onTabSelected(MessageBoardTab.Private) },
                modifier = Modifier
                    .clip(tabShape)
                    .background(if (selectedTab is MessageBoardTab.Private) EmpowerMomColors.PeachPale else Color.Transparent)
                    .border(
                        width = if (selectedTab is MessageBoardTab.Private) 0.5.dp else 0.dp,
                        color = if (selectedTab is MessageBoardTab.Private) EmpowerMomColors.Rose else Color.Transparent,
                        shape = tabShape
                    ),
                selectedContentColor = EmpowerMomColors.RoseDark,
                unselectedContentColor = MaterialTheme.colorScheme.secondary,
                text = { Text("私密", style = MaterialTheme.typography.bodySmall) }
            )

            // 各分类 Tab
            MessageCategory.entries.forEach { category ->
                val isSelected = selectedTab is MessageBoardTab.Category && selectedTab.category == category
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(MessageBoardTab.Category(category)) },
                    modifier = Modifier
                        .clip(tabShape)
                        .background(if (isSelected) EmpowerMomColors.PeachPale else Color.Transparent)
                        .border(
                            width = if (isSelected) 0.5.dp else 0.dp,
                            color = if (isSelected) EmpowerMomColors.Rose else Color.Transparent,
                            shape = tabShape
                        ),
                    selectedContentColor = EmpowerMomColors.RoseDark,
                    unselectedContentColor = MaterialTheme.colorScheme.secondary,
                    text = {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }

        HorizontalDivider(
            color = Color.Transparent,
            thickness = 0.dp
        )
    }
}

@Composable
private fun MessageSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "搜索留言", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入关键词…", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onSearch) { Text("搜索") }
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchResultScreen(
    query: String,
    results: List<Message>,
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    userProfile: UserProfile
) {
    androidx.activity.compose.BackHandler(enabled = true) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索：$query") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "没有找到相关留言",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = results, key = { it.id }) { message ->
                    MessageCard(
                        message = message,
                        userProfile = userProfile,
                        onLikeClick = {},
                        onResonanceClick = {},
                        onReplyClick = { onNavigateToDetail(message.id) },
                        onCardClick = { onNavigateToDetail(message.id) }
                    )
                }
            }
        }
    }
}

/*
==================== 原有内容（保留，勿删）====================

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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        EmpowerMomColors.Peach,
                                        EmpowerMomColors.Rose
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌷", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "EmpowerMom",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
*/

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
    userProfile: UserProfile,
    onLikeClick: () -> Unit,
    onResonanceClick: () -> Unit,
    onReplyClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String? = null,
    onDeleteClick: (() -> Unit)? = null
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (message.isCrisis) 2.dp else 0.5.dp,
            color = if (message.isCrisis)
                EmpowerMomColors.CrisisRed
            else
                MaterialTheme.colorScheme.outline
        ),
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
                            .background(
                                if (message.isAnonymous) EmpowerMomColors.PeachPale
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (message.isAnonymous) 1.dp else 0.dp,
                                color = if (message.isAnonymous) EmpowerMomColors.Rose else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (message.isAnonymous) {
                            Text(text = "🦖", fontSize = 16.sp)
                        } else {
                            if (message.author == userProfile.nickname && userProfile.avatarEmoji.isNotBlank()) {
                                Text(text = userProfile.avatarEmoji, fontSize = 16.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
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
                // 分类标签 + 删除按钮
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .border(0.5.dp, MaterialTheme.colorScheme.outline)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    if (currentUserId != null && message.userId == currentUserId && onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "删除",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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

            if (message.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    message.attachments.take(6).forEach { attachment ->
                        val shape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(shape)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline, shape)
                        ) {
                            when (attachment.kind) {
                                MediaKind.Image -> AsyncImage(
                                    model = attachment.uri,
                                    contentDescription = "图片",
                                    modifier = Modifier.fillMaxSize()
                                )
                                MediaKind.Video -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(EmpowerMomColors.AmberPale),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "视频",
                                        tint = EmpowerMomColors.Rose
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
            XingyaReplyBlock(message = message)

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
private fun XingyaReplyBlock(
    message: Message
) {
    val messageId = message.id
    val hasResponse = message.aiResponse.isNotBlank()
    val avatarResId = R.drawable.ic_xingya_avatar

    var avatarVisible by remember(messageId) { mutableStateOf(hasResponse) }
    var textVisible by remember(messageId) { mutableStateOf(hasResponse) }
    var lastHadResponse by remember(messageId) { mutableStateOf(hasResponse) }

    val haloColor = remember(messageId) { Animatable(EmpowerMomColors.Peach) }

    LaunchedEffect(messageId) {
        if (!hasResponse) {
            avatarVisible = false
            textVisible = false
            avatarVisible = true
        }
    }

    LaunchedEffect(messageId, hasResponse) {
        if (!lastHadResponse && hasResponse) {
            haloColor.snapTo(EmpowerMomColors.Peach)
            haloColor.animateTo(
                targetValue = EmpowerMomColors.Amber,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            haloColor.animateTo(
                targetValue = EmpowerMomColors.Peach,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            kotlinx.coroutines.delay(300)
            textVisible = true
        } else if (hasResponse) {
            textVisible = true
        }
        lastHadResponse = hasResponse
    }

    val displayedText = when {
        !hasResponse -> ""
        message.isPrivateOnly -> "这里只有你和我。\n${message.aiResponse}"
        else -> message.aiResponse
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = avatarVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> (fullWidth * 0.25f).roundToInt() }
                    )
        ) {
            XingyaAvatar(
                haloColor = haloColor.value,
                avatarResId = avatarResId
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "星芽",
                style = MaterialTheme.typography.titleSmall,
                color = EmpowerMomColors.TextMid
            )
            Spacer(modifier = Modifier.height(6.dp))

            val bubbleShape = RoundedCornerShape(12.dp)
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = bubbleShape,
                            ambientColor = EmpowerMomColors.Peach.copy(alpha = 0.15f),
                            spotColor = EmpowerMomColors.Peach.copy(alpha = 0.15f)
                        )
                        .background(EmpowerMomColors.AmberPale, bubbleShape)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (!hasResponse) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmpowerMomColors.TextMid
                        )
                    } else {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = textVisible,
                            enter = fadeIn(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) +
                                    slideInVertically(
                                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                                        initialOffsetY = { fullHeight -> (fullHeight * 0.25f).roundToInt() }
                                    )
                        ) {
                            Text(
                                text = displayedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmpowerMomColors.TextDark
                            )
                        }
                    }
                }

                if (message.isPrivateOnly && message.isCrisis && hasResponse) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmpowerMomColors.CrisisBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "心理援助热线：400-161-9995",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmpowerMomColors.CrisisRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XingyaAvatar(
    haloColor: Color,
    avatarResId: Int
) {
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(haloColor.copy(alpha = 0.40f))
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(EmpowerMomColors.Peach),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "星芽",
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/*
==================== 原有内容（保留，勿删）- AiResponseBlock（旧版 AI 标签样式）====================

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
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        EmpowerMomColors.AmberPale,
                        EmpowerMomColors.PeachPale
                    )
                )
            )
            .drawBehind {
                drawRect(
                    color = EmpowerMomColors.Amber,
                    size = Size(3.dp.toPx(), size.height)
                )
            }
            .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)
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
            Text(
                text = "AI 正在认真思考你的话……",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.alpha(alpha)
            )
        } else {
            Text(
                text = response,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
*/

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
        HorizontalDivider(color = Color.Transparent, thickness = 0.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmpowerMomColors.Peach
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = "✏️  写心事",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }           // ← Button 闭合
        }               // ← Box(padding) 闭合
    }                   // ← Column 闭合
}                       // ← WriteMessageBottomBar 闭合

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
    }
}

/*
==================== 原有内容（保留，勿删）- BoardHeader 插入旧逻辑 ====================

// item { BoardHeader() }
*/

/*
==================== 原有内容（保留，勿删）- EmptyState「写下第一条」按钮 ====================

// Spacer(modifier = Modifier.height(20.dp))
// Button(
//     onClick = onWriteClick,
//     shape = MaterialTheme.shapes.medium,
//     colors = ButtonDefaults.buttonColors(
//         containerColor = MaterialTheme.colorScheme.primary,
//         contentColor = MaterialTheme.colorScheme.onPrimary
//     )
// ) {
//     Icon(
//         imageVector = Icons.Outlined.Edit,
//         contentDescription = null,
//         modifier = Modifier.size(16.dp)
//     )
//     Spacer(modifier = Modifier.width(8.dp))
//     Text("写下第一条", style = MaterialTheme.typography.labelLarge)
// }
*/

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

/*
==================== 原有内容（保留，勿删）- 留言卡片旧头像 ====================

// Box(
//     modifier = Modifier
//         .size(36.dp)
//         .clip(CircleShape)
//         .background(MaterialTheme.colorScheme.surfaceVariant),
//     contentAlignment = Alignment.Center
// ) {
//     Icon(
//         imageVector = Icons.Outlined.Person,
//         contentDescription = null,
//         modifier = Modifier.size(18.dp),
//         tint = MaterialTheme.colorScheme.secondary
//     )
// }
*/

/*
==================== 原有内容（保留，勿删）- 底部写心事按钮全宽旧实现 ====================

// Button(
//     onClick = onClick,
//     modifier = Modifier
//         .fillMaxWidth()
//         .height(52.dp),
//     shape = RoundedCornerShape(12.dp),
//     colors = ButtonDefaults.buttonColors(
//         containerColor = EmpowerMomColors.Peach
//     ),
//     contentPadding = PaddingValues(horizontal = 16.dp)
// ) { ... }
*/

@Preview(showBackground = true, backgroundColor = 0xFFFDF6EE, widthDp = 390)
@Composable
private fun PreviewMessageBoardTopBarAndCards() {
    EmpowerMomTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            MessageBoardTopBar(
                selectedTab = MessageBoardTab.Category(MessageCategory.EMOTION),
                onTabSelected = {},
                onSearchClick = {}
            )

            val demoMessage = Message(
                id = 1,
                content = "今天真的好累，宝宝一直哭，我也快撑不住了……想找个人说说话。",
                author = "momo",
                category = MessageCategory.EMOTION,
                isAnonymous = true,
                attachments = listOf(
                    com.empowermom.app.feature.messageboard.model.MediaAttachment(
                        uri = "content://preview/image",
                        kind = MediaKind.Image
                    )
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = listOf(demoMessage, demoMessage.copy(id = 2, content = "有人也经历过吗？", attachments = emptyList()))) { msg ->
                    MessageCard(
                        message = msg,
                        userProfile = UserProfile(nickname = "momo", avatarEmoji = "🌸", isProfileComplete = true),
                        onLikeClick = {},
                        onResonanceClick = {},
                        onReplyClick = {},
                        onCardClick = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDF6EE, widthDp = 390, heightDp = 700)
@Composable
private fun PreviewSearchResultScreen() {
    EmpowerMomTheme {
        val demoMessage = Message(
            id = 1,
            content = "睡眠不足让我有点崩溃，但我在慢慢学着放过自己。",
            author = "momo",
            category = MessageCategory.MOM_HELP,
            isAnonymous = true
        )
        SearchResultScreen(
            query = "睡眠",
            results = listOf(demoMessage, demoMessage.copy(id = 2, content = "睡不够真的会影响情绪。")),
            onBack = {},
            onNavigateToDetail = {},
            userProfile = UserProfile(nickname = "momo", avatarEmoji = "🌸", isProfileComplete = true)
        )
    }
}
