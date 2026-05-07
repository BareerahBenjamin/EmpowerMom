package com.empowermom.app.feature.messageboard.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empowermom.app.feature.messageboard.model.MessageCategory
import com.empowermom.app.feature.messageboard.viewmodel.EditorState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester


/**
 * 写留言半屏编辑器
 * 对应 HTML 中的 #editor-overlay + .editor-container
 *
 * 流程：
 * 1. 选择分区（必选）
 * 2. 输入内容（500字上限）
 * 3. 选择标签（可选，最多3个）
 * 4. 匿名设置
 * 5. 发布
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MessageEditor(
    editorState: EditorState,
    presetTags: List<String>,
    onDismiss: () -> Unit,
    onContentChange: (String) -> Unit,
    onCategorySelect: (MessageCategory) -> Unit,
    onTagToggle: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    // 遮罩层
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        // 半屏内容区（从底部弹出，点击遮罩不关闭编辑区本身）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.background)
                .clickable(enabled = false) {} // 拦截点击，防止关闭
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // ── 标题栏 ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("分享你的故事", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 1. 选择分区 ────────────────────────────────────────────────────
            SectionLabel(text = "选择主题分区", required = true)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessageCategory.entries.forEach { category ->
                    val isSelected = editorState.selectedCategory == category
                    CategoryCard(
                        category = category,
                        isSelected = isSelected,
                        onClick = { onCategorySelect(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. 输入内容 ────────────────────────────────────────────────────
            SectionLabel(text = "内容", required = true)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editorState.content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                placeholder = {
                    Text(
                        "分享你的心情、困惑或经验...",
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                maxLines = 10,
                shape = RoundedCornerShape(0.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

// 字数统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val isNearLimit = editorState.charCount > 450
                val isAtLimit = editorState.charCount >= 500
                Column(horizontalAlignment = Alignment.End) {
                    if (isAtLimit) {
                        Text(
                            text = "已达字数上限",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "${editorState.charCount}/500",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isNearLimit) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. 预置标签 ────────────────────────────────────────────────────
            SectionLabel(text = "添加标签（可选，最多3个）")
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetTags.forEach { tag ->
                    val isSelected = tag in editorState.selectedTags
                    TagChip(
                        text = tag,
                        isSelected = isSelected,
                        onClick = { onTagToggle(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

        // ── 4. 匿名设置 ────────────────────────────────────────────────────
            val nicknameFocusRequester = remember { FocusRequester() }

        // 监听 isAnonymous 变化，取消匿名时自动聚焦
            LaunchedEffect(editorState.isAnonymous) {
                if (!editorState.isAnonymous) {
                    // 稍微延迟一下，等 AnimatedVisibility 展开后再聚焦
                    kotlinx.coroutines.delay(150)
                    nicknameFocusRequester.requestFocus()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = editorState.isAnonymous,
                    onCheckedChange = onAnonymousChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("匿名发布", style = MaterialTheme.typography.bodyMedium)

                // 非匿名时显示昵称输入框
                AnimatedVisibility(visible = !editorState.isAnonymous) {
                    Row {
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = editorState.nickname,
                            onValueChange = onNicknameChange,
                            modifier = Modifier
                                .width(160.dp)
                                .focusRequester(nicknameFocusRequester),
                            placeholder = {
                                Text("输入昵称", color = MaterialTheme.colorScheme.secondary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5. 发布按钮 ────────────────────────────────────────────────────
            Button(
                onClick = onSubmit,
                enabled = editorState.isValid && !editorState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline
                )
            ) {
                if (editorState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("发布", style = MaterialTheme.typography.labelLarge)
                }
            }

            // 错误提示
            editorState.submitError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── 子组件 ────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, required: Boolean = false) {
    Row {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
        if (required) {
            Spacer(modifier = Modifier.width(2.dp))
            Text("*", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CategoryCard(
    category: MessageCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .border(if (isSelected) 1.dp else 0.5.dp, borderColor)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (category) {
            MessageCategory.EMOTION -> Icons.Outlined.FavoriteBorder
            MessageCategory.PARENTING -> Icons.Outlined.HelpOutline
            MessageCategory.EXPERIENCE -> Icons.Outlined.Lightbulb
            MessageCategory.RECOVERY -> Icons.Outlined.SelfImprovement
        }
        Icon(
            imageVector = icon,
            contentDescription = category.displayName,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun TagChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(
                0.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.background
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}
