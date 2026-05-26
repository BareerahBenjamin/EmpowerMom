package com.empowermom.app.feature.dailylog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.feature.dailylog.model.QuestionBank
import com.empowermom.app.feature.dailylog.model.QuestionOption
import com.empowermom.app.feature.dailylog.viewmodel.DailyLogIntent
import com.empowermom.app.feature.dailylog.viewmodel.DailyLogViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import com.empowermom.app.feature.dailylog.model.DailyLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyLogScreen(
    viewModel: DailyLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 连续 7 天后自动加载小结
    LaunchedEffect(uiState.allLogs.size) {
        if (uiState.allLogs.size >= 7 && uiState.weeklySummary.isEmpty()) {
            viewModel.handleIntent(DailyLogIntent.LoadWeeklySummary)
        }
    }

    Scaffold(
        topBar = { DailyLogTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 日期 & 进度 ──────────────────────────────────────────────────
            item { DateAndProgressCard(totalLogs = uiState.allLogs.size) }

            // ── 月历打卡 ─────────────────────────────────────────────────────
            item { MonthlyCalendarCard(logs = uiState.allLogs) }

            // ── Q1 核心状态 ──────────────────────────────────────────────────
            item {
                QuestionCard(
                    label = "Q1",
                    title = uiState.currentCore.title,
                    onRefresh = { viewModel.handleIntent(DailyLogIntent.RefreshQ1) }
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(uiState.currentCore.options) { option ->
                            OptionChip(
                                option = option,
                                selected = uiState.q1Selected == option,
                                onClick = { viewModel.handleIntent(DailyLogIntent.SelectQ1(option)) }
                            )
                        }
                    }
                }
            }

            // ── Q2 生活实录 ──────────────────────────────────────────────────
            item {
                QuestionCard(
                    label = "Q2",
                    title = uiState.currentLife.title,
                    onRefresh = { viewModel.handleIntent(DailyLogIntent.RefreshQ2) }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.currentLife.options.forEach { option ->
                            OptionChip(
                                option = option,
                                selected = uiState.q2Selected == option,
                                onClick = { viewModel.handleIntent(DailyLogIntent.SelectQ2(option)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Q3 开放题 ────────────────────────────────────────────────────
            item {
                QuestionCard(
                    label = "Q3",
                    title = uiState.currentOpen.title,
                    onRefresh = { viewModel.handleIntent(DailyLogIntent.RefreshQ3) }
                ) {
                    Text(
                        text = uiState.currentOpen.tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = uiState.q3Text,
                        onValueChange = { viewModel.handleIntent(DailyLogIntent.UpdateQ3Text(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("写下你的感受…", style = MaterialTheme.typography.bodySmall) },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmpowerMomColors.Peach,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // ── 生成按钮 ─────────────────────────────────────────────────────
            item {
                Button(
                    onClick = { viewModel.handleIntent(DailyLogIntent.GenerateCard) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmpowerMomColors.Peach),
                    enabled = !uiState.isGenerating
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI 生成中…", color = Color.White)
                    } else {
                        Text("✨  一键生成今日卡片", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── 今日卡片 ─────────────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = uiState.isCardVisible,
                    enter = fadeIn() + expandVertically()
                ) {
                    TodayCard(
                        q1Type    = uiState.currentCore.type,
                        q1Answer  = uiState.q1Selected?.let { "${it.emoji} ${it.text}" } ?: "未选择",
                        q1Color   = uiState.q1Selected?.colorToken ?: "",
                        q2Answer  = uiState.q2Selected?.let { "${it.emoji} ${it.text}" } ?: "未选择",
                        q3Title   = uiState.currentOpen.title,
                        q3Text    = uiState.q3Text.ifBlank { "无记录" },
                        aiText    = uiState.aiCardText,
                        isLoading = uiState.isGenerating,
                        isPrivate = uiState.isPrivate,
                        onPrivacyToggle = { viewModel.handleIntent(DailyLogIntent.TogglePrivacy) }
                    )
                }
            }

            // 同步状态提示
            if (uiState.syncStatus.isNotBlank()) {
                item {
                    Text(
                        uiState.syncStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.syncStatus.contains("失败"))
                            MaterialTheme.colorScheme.error
                        else EmpowerMomColors.TextLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ── 本周小结 ─────────────────────────────────────────────────────
            item { WeeklySummaryCard(
                summary        = uiState.weeklySummary,
                isLoading      = uiState.isSummaryLoading,
                totalLogs      = uiState.allLogs.size
            ) }

            // ── 心情时间线 ────────────────────────────────────────────────────
            item { MoodTimeline(logs = uiState.allLogs) }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── TopBar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyLogTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(EmpowerMomColors.Peach, EmpowerMomColors.Rose))
                        ),
                    contentAlignment = Alignment.Center
                ) { Text("📋", fontSize = 16.sp) }
                Text("每日速记", style = MaterialTheme.typography.headlineSmall)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ── 日期 & 打卡进度 ───────────────────────────────────────────────────────────

@Composable
private fun DateAndProgressCard(totalLogs: Int) {
    val today = remember {
        SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA).format(Date())
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(today, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "3步完成 · 1分钟生成卡片",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 连续打卡 badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(EmpowerMomColors.PeachPale)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "🌷 已记录 $totalLogs 天",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmpowerMomColors.Rose
                )
            }
        }
    }
}

// ── 问题卡片容器 ──────────────────────────────────────────────────────────────

@Composable
private fun QuestionCard(
    label: String,
    title: String,
    onRefresh: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // label badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmpowerMomColors.PeachPale)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.Rose, fontWeight = FontWeight.Bold)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Refresh, contentDescription = "换一题",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ── 选项芯片 ──────────────────────────────────────────────────────────────────

@Composable
private fun OptionChip(
    option: QuestionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) EmpowerMomColors.PeachPale else MaterialTheme.colorScheme.background
    val borderColor = if (selected) EmpowerMomColors.Rose else MaterialTheme.colorScheme.outline
    val textColor = if (selected) EmpowerMomColors.Rose else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${option.emoji} ${option.text}",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

// ── 今日卡片 ──────────────────────────────────────────────────────────────────

@Composable
private fun TodayCard(
    q1Type: String, q1Answer: String, q1Color: String,
    q2Answer: String, q3Title: String, q3Text: String,
    aiText: String, isLoading: Boolean,
    isPrivate: Boolean, onPrivacyToggle: () -> Unit
) {
    val cardBg = QuestionBank.colorFromToken(q1Color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题行
            Text(
                "今日身心记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // 答案列表
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CardRow("💭 $q1Type", q1Answer)
                CardRow("📊 生活状态", q2Answer)
                CardRow("🌟 $q3Title", q3Text)
            }

            Spacer(Modifier.height(16.dp))

            // AI 文案区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(14.dp)
            ) {
                if (isLoading) {
                    val alpha by rememberInfiniteTransition(label = "").animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                        label = "loading"
                    )
                    Text(
                        "AI 正在温柔思考……",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = EmpowerMomColors.TextMid,
                        modifier = Modifier.alpha(alpha)
                    )
                } else {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Outlined.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            tint = EmpowerMomColors.Rose
                        )
                        Text(
                            aiText.ifBlank { "今天也辛苦了，你已经很棒了 💛" },
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = EmpowerMomColors.TextMid
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "AI 温柔生成",
                style = MaterialTheme.typography.labelSmall,
                color = EmpowerMomColors.TextLight,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // 操作行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPrivacyToggle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        if (isPrivate) "🔒 私密" else "🌐 公开",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CardRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextMid, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── 本周小结 ──────────────────────────────────────────────────────────────────

@Composable
private fun WeeklySummaryCard(
    summary: String,
    isLoading: Boolean,
    totalLogs: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = EmpowerMomColors.AmberPale
        ),
        border = BorderStroke(1.dp, EmpowerMomColors.Amber)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📊", fontSize = 16.sp)
                Text("本周身心小结", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            when {
                isLoading -> {
                    val alpha by rememberInfiniteTransition(label = "").animateFloat(
                        initialValue = 0.4f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label = "sum_loading"
                    )
                    Text(
                        "AI 正在整理你的本周记录……",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmpowerMomColors.TextMid,
                        modifier = Modifier.alpha(alpha)
                    )
                }
                summary.isNotBlank() -> {
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextDark, lineHeight = 22.sp)
                }
                else -> {
                    Text(
                        if (totalLogs < 7)
                            "连续记录满 7 天后，AI 会为你生成专属本周身心分析 🌱\n已记录 $totalLogs / 7 天"
                        else
                            "连续记录7天，生成专属分析报告~",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmpowerMomColors.TextLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── 月历打卡 ─────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyCalendarCard(logs: List<DailyLog>) {
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    // 构建该月记录日期集合
    val recordedDates = remember(logs, currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        logs.mapNotNull { log ->
            cal.timeInMillis = log.date
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)
            if (y == currentYear && m == currentMonth) d to log else null
        }.toMap()
    }

    // 今天
    val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 月份导航
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ChevronLeft, "上月", tint = EmpowerMomColors.TextMid)
                }

                val monthNames = listOf("一月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月","十二月")
                Text(
                    "${currentYear}年 ${monthNames[currentMonth]}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                IconButton(onClick = {
                    if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ChevronRight, "下月", tint = EmpowerMomColors.TextMid)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 星期头
            val weekHeaders = listOf("一", "二", "三", "四", "五", "六", "日")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmpowerMomColors.TextLight
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 日历网格
            val firstDay = Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1)
            }
            // 周一为第一天
            val firstDayOfWeek = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (week in 0 until totalCells / 7) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (dayIndex in 0 until 7) {
                            val cellIndex = week * 7 + dayIndex
                            val dayNum = cellIndex - firstDayOfWeek + 1
                            val isValid = dayNum in 1..daysInMonth
                            val isToday = isValid && currentYear == todayYear && currentMonth == todayMonth && dayNum == todayDay
                            val hasRecord = isValid && recordedDates.containsKey(dayNum)
                            val recordColor = if (hasRecord) QuestionBank.colorFromToken(recordedDates[dayNum]!!.q1Color) else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValid) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .then(
                                                    if (isToday) Modifier.border(1.5.dp, EmpowerMomColors.Rose, CircleShape)
                                                    else Modifier
                                                )
                                                .then(
                                                    if (isToday) Modifier.background(EmpowerMomColors.PeachPale)
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                dayNum.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isToday) EmpowerMomColors.Rose else EmpowerMomColors.TextDark
                                            )
                                        }
                                        // 记录指示点
                                        if (hasRecord) {
                                            Spacer(Modifier.height(1.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (recordColor != Color.Transparent) recordColor
                                                        else EmpowerMomColors.Peach
                                                    )
                                            )
                                        } else {
                                            Spacer(Modifier.height(7.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 图例
            if (logs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmpowerMomColors.Peach)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "已记录 ${logs.size} 天",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmpowerMomColors.TextLight
                    )
                }
            }
        }
    }
}

// ── 心情时间线 ─────────────────────────────────────────────────────────────────

@Composable
private fun MoodTimeline(logs: List<DailyLog>) {
    val dateFormat = remember { SimpleDateFormat("M月d日", Locale.CHINA) }
    val weekdayFormat = remember { SimpleDateFormat("EEEE", Locale.CHINA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(EmpowerMomColors.Peach, EmpowerMomColors.Amber))),
                    contentAlignment = Alignment.Center
                ) { Text("⏳", fontSize = 14.sp) }
                Text("心情时间线", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            }

            if (logs.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "开始记录你的心情吧，这里会展示你的每一天 🌱",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmpowerMomColors.TextLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                )
            } else {
                Spacer(Modifier.height(16.dp))

                val displayLogs = logs.take(14)
                displayLogs.forEachIndexed { index, log ->
                    val moodEmoji = QuestionBank.emojiFromToken(log.q1Color)
                    val moodColor = QuestionBank.colorFromToken(log.q1Color)
                    val positivity = QuestionBank.positivityScore(log.q1Color)
                    val dateStr = dateFormat.format(Date(log.date))
                    val weekdayStr = weekdayFormat.format(Date(log.date))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 时间线左侧
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(moodColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (positivity > 0.5f) EmpowerMomColors.Rose
                                            else EmpowerMomColors.TextMid
                                        )
                                )
                            }
                            // 连接线
                            if (index < displayLogs.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(56.dp)
                                        .background(EmpowerMomColors.PeachLight, RoundedCornerShape(1.dp))
                                )
                            }
                        }

                        // 内容卡片
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (index == 0) moodColor.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.background
                                )
                                .padding(12.dp)
                        ) {
                            // 日期行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(dateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = EmpowerMomColors.TextDark)
                                    Text(weekdayStr, style = MaterialTheme.typography.labelSmall, color = EmpowerMomColors.TextLight)
                                }
                                Text(moodEmoji, fontSize = 18.sp)
                            }

                            Spacer(Modifier.height(4.dp))

                            // 状态摘要
                            Text(
                                "${log.q1Type}：${log.q1Answer}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmpowerMomColors.TextMid
                            )
                            if (log.q2Answer.isNotBlank() && log.q2Answer != "未选择") {
                                Text(log.q2Answer, style = MaterialTheme.typography.bodySmall, color = EmpowerMomColors.TextLight)
                            }

                            // AI 文案
                            if (log.aiCardText.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.SmartToy, contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = EmpowerMomColors.Rose.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        log.aiCardText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = EmpowerMomColors.TextMid
                                    )
                                }
                            }
                        }
                    }

                    if (index < displayLogs.lastIndex) {
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (logs.size > 14) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "还有 ${logs.size - 14} 条更早的记录 → 前往「我的」查看全部",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmpowerMomColors.TextLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}