package com.empowermom.app.feature.messageboard.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.os.Build
import com.empowermom.app.feature.messageboard.model.MessageCategory
import com.empowermom.app.feature.messageboard.model.MediaKind
import com.empowermom.app.feature.messageboard.viewmodel.EditorState
import coil.compose.AsyncImage
import com.empowermom.app.core.ui.theme.EmpowerMomColors
import com.empowermom.app.core.ui.theme.EmpowerMomTheme
import androidx.compose.ui.tooling.preview.Preview

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
    onPrivateOnlyChange: (Boolean) -> Unit,
    onAddAttachment: (uri: String, mimeType: String?) -> Unit,
    onRemoveAttachment: (uri: String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val context = LocalContext.current
    var showMediaPermissionDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var addTagError by remember { mutableStateOf<String?>(null) }
    
    // 保存用户自定义添加的标签（包括未被选中的）
    val customTags = remember {
        mutableStateListOf(*editorState.selectedTags.filter { it !in presetTags }.toTypedArray())
    }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            onAddAttachment(uri.toString(), context.contentResolver.getType(uri))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = requiredPermissions.all { granted[it] == true }
        if (allGranted) {
            attachmentLauncher.launch(arrayOf("image/" + "*", "video/" + "*"))
        }
    }

    if (showMediaPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMediaPermissionDialog = false },
            title = { Text("允许访问相册/视频吗？") },
            text = { Text("用于给这条留言添加图片或视频。你可以随时在系统设置里关闭权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showMediaPermissionDialog = false
                    permissionLauncher.launch(requiredPermissions)
                }) { Text("允许") }
            },
            dismissButton = {
                TextButton(onClick = { showMediaPermissionDialog = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false; newTagText = "" },
            title = { Text("添加标签") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { value ->
                            if (value.length <= 10) newTagText = value
                        },
                        placeholder = {
                            Text(
                                text = "最多10个字",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    addTagError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = newTagText.trim().replace(",", "，")
                    when {
                        clean.isBlank() -> addTagError = "请输入标签"
                        editorState.selectedTags.size >= 3 && clean !in editorState.selectedTags -> addTagError = "最多只能选 3 个标签"
                        else -> {
                            addTagError = null
                            if (clean !in customTags && clean !in presetTags) {
                                customTags.add(clean)
                            }
                            if (clean !in editorState.selectedTags) {
                                onTagToggle(clean)
                            }
                            showAddTagDialog = false
                            newTagText = ""
                        }
                    }
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false; newTagText = ""; addTagError = null }) { Text("取消") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

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
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 附件：图片/视频 ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddAttachmentTile(
                    onClick = { showMediaPermissionDialog = true }
                )
                editorState.attachments.forEach { attachment ->
                    AttachmentPreviewTile(
                        uri = attachment.uri,
                        kind = attachment.kind,
                        onRemove = { onRemoveAttachment(attachment.uri) }
                    )
                }
            }

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
                customTags.forEach { tag ->
                    val isSelected = tag in editorState.selectedTags
                    CustomTagChip(
                        text = tag,
                        isSelected = isSelected,
                        onClick = { onTagToggle(tag) },
                        onDelete = {
                            customTags.remove(tag)
                            if (isSelected) {
                                onTagToggle(tag)
                            }
                        }
                    )
                }
                AddTagChip(
                    onClick = {
                        addTagError = null
                        showAddTagDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 4. 匿名发布（滑动） ───────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("匿名发布", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "开启后，你的账号会被匿名化处理，其他人不会识别到你的账号。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = editorState.isAnonymous,
                    onCheckedChange = onAnonymousChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmpowerMomColors.Rose,
                        checkedTrackColor = EmpowerMomColors.PeachPale,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 5. 仅自己可见（滑动）──────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("仅自己可见", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "开启后只会显示在「私密」标签下",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = editorState.isPrivateOnly,
                    onCheckedChange = onPrivateOnlyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmpowerMomColors.Rose,
                        checkedTrackColor = EmpowerMomColors.PeachPale,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 6. 发布按钮 ────────────────────────────────────────────────────
            Button(
                onClick = onSubmit,
                enabled = editorState.isValid && !editorState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmpowerMomColors.Peach,
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
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (isSelected) EmpowerMomColors.Rose else MaterialTheme.colorScheme.outline
    val background = if (isSelected) EmpowerMomColors.PeachPale else MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(if (isSelected) 1.dp else 0.5.dp, borderColor, shape)
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (category) {
            MessageCategory.EMOTION -> Icons.Outlined.FavoriteBorder
            MessageCategory.MOM_HELP -> Icons.Outlined.Face
            MessageCategory.FAMILY_RELATION -> Icons.Outlined.Home
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
            color = if (isSelected) EmpowerMomColors.RoseDark
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
    val shape = RoundedCornerShape(14.dp)
    val background = if (isSelected) EmpowerMomColors.PeachPale else MaterialTheme.colorScheme.surface
    val border = if (isSelected) EmpowerMomColors.Rose else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(0.5.dp, border, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) EmpowerMomColors.RoseDark else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun CustomTagChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val background = if (isSelected) EmpowerMomColors.PeachPale else MaterialTheme.colorScheme.surface
    val border = if (isSelected) EmpowerMomColors.Rose else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(0.5.dp, border, shape)
            .clickable { onClick() }
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) EmpowerMomColors.RoseDark else MaterialTheme.colorScheme.onBackground
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "删除标签",
                tint = if (isSelected) EmpowerMomColors.RoseDark else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun AddTagChip(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(EmpowerMomColors.PeachPale)
            .border(0.5.dp, EmpowerMomColors.Rose, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "＋ 添加",
            style = MaterialTheme.typography.labelSmall,
            color = EmpowerMomColors.RoseDark
        )
    }
}

@Composable
private fun AddAttachmentTile(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(shape)
            .background(EmpowerMomColors.PeachPale)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "添加图片或视频",
            tint = EmpowerMomColors.Rose
        )
    }
}

@Composable
private fun AttachmentPreviewTile(
    uri: String,
    kind: MediaKind,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, shape)
    ) {
        when (kind) {
            MediaKind.Image -> {
                AsyncImage(
                    model = uri,
                    contentDescription = "图片",
                    modifier = Modifier.fillMaxSize()
                )
            }
            MediaKind.Video -> {
                Box(
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

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = EmpowerMomColors.TextMid
            )
        }
    }
}

/*
==================== 原有内容（保留，勿删）====================

val icon = when (category) {
    MessageCategory.EMOTION -> Icons.Outlined.FavoriteBorder
    MessageCategory.PARENTING -> Icons.Outlined.HelpOutline
    MessageCategory.EXPERIENCE -> Icons.Outlined.Lightbulb
    MessageCategory.RECOVERY -> Icons.Outlined.SelfImprovement
}
*/

@Preview(showBackground = true, backgroundColor = 0xFFFDF6EE, widthDp = 390, heightDp = 780)
@Composable
private fun PreviewMessageEditor() {
    EmpowerMomTheme {
        MessageEditor(
            editorState = EditorState(
                content = "今天心里很乱，想把这些话写下来。",
                selectedCategory = MessageCategory.EMOTION,
                selectedTags = listOf("睡眠不足", "产后抑郁"),
                isAnonymous = true,
                isPrivateOnly = false
            ),
            presetTags = listOf(
                "宝宝肠胀气",
                "产后脱发",
                "婆媳关系",
                "睡眠不足",
                "母乳喂养困难",
                "产后抑郁",
                "身材焦虑",
                "育儿困惑"
            ),
            onDismiss = {},
            onContentChange = {},
            onCategorySelect = {},
            onTagToggle = {},
            onAnonymousChange = {},
            onPrivateOnlyChange = {},
            onAddAttachment = { _, _ -> },
            onRemoveAttachment = {},
            onNicknameChange = {},
            onSubmit = {}
        )
    }
}

/*
==================== 原有内容（保留，勿删）- 写心事旧交互 ====================

// ── 仅自己可见（Checkbox 旧版）──────────────────────────────────────────────
// Row(
//     verticalAlignment = Alignment.CenterVertically,
//     modifier = Modifier.fillMaxWidth()
// ) {
//     Checkbox(
//         checked = editorState.isPrivateOnly,
//         onCheckedChange = onPrivateOnlyChange,
//         colors = CheckboxDefaults.colors(
//             checkedColor = MaterialTheme.colorScheme.primary
//         )
//     )
//     Spacer(modifier = Modifier.width(4.dp))
//     Text("仅自己可见", style = MaterialTheme.typography.bodyMedium)
// }
// Text(
//     text = "仅在「私密」标签下显示，其他人看不到。",
//     style = MaterialTheme.typography.bodySmall,
//     color = MaterialTheme.colorScheme.secondary,
//     modifier = Modifier.padding(start = 36.dp, top = 2.dp)
// )
//
// ── 匿名发布（Checkbox 旧版）────────────────────────────────────────────────
// Checkbox(
//     checked = editorState.isAnonymous,
//     onCheckedChange = onAnonymousChange,
//     colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
// )
//
// ── CategoryCard 旧版（直角边框）────────────────────────────────────────────
// modifier = modifier
//     .border(if (isSelected) 1.dp else 0.5.dp, borderColor)
//     .clickable { onClick() }
//     .padding(vertical = 16.dp, horizontal = 8.dp)
//
// ── TagChip 旧版（无填充色）─────────────────────────────────────────────────
// .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
// .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
// .padding(horizontal = 12.dp, vertical = 6.dp)
*/

/*
==================== 原有内容（保留，勿删）- 非匿名昵称输入框 ====================

// AnimatedVisibility(visible = !editorState.isAnonymous) {
//     Row {
//         Spacer(modifier = Modifier.width(16.dp))
//         OutlinedTextField(
//             value = editorState.nickname,
//             onValueChange = onNicknameChange,
//             modifier = Modifier
//                 .width(160.dp)
//                 .focusRequester(nicknameFocusRequester),
//             placeholder = {
//                 Text("输入昵称", color = MaterialTheme.colorScheme.secondary)
//             },
//             singleLine = true,
//             shape = RoundedCornerShape(12.dp),
//             colors = OutlinedTextFieldDefaults.colors(
//                 focusedBorderColor = MaterialTheme.colorScheme.primary,
//                 unfocusedBorderColor = MaterialTheme.colorScheme.outline
//             )
//         )
//     }
// }
*/

/*
==================== 原有内容（保留，勿删）- 匿名开关关闭闪退相关旧逻辑 ====================

// val nicknameFocusRequester = remember { FocusRequester() }
// LaunchedEffect(editorState.isAnonymous) {
//     if (!editorState.isAnonymous) {
//         kotlinx.coroutines.delay(150)
//         nicknameFocusRequester.requestFocus()
//     }
// }
*/

/*
==================== 原有内容（保留，勿删）- 添加附件旧逻辑（无权限确认）====================

// AddAttachmentTile(
//     onClick = { attachmentLauncher.launch(arrayOf("image/" + "*", "video/" + "*")) }
// )
*/

/*
==================== 原有内容（保留，勿删）- 自定义标签输入旧规则（12字）====================

// onValueChange = { value ->
//     if (value.length <= 12) newTagText = value
// }
// placeholder = { Text("输入 1-12 个字") }
*/
