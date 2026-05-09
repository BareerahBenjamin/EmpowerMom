package com.empowermom.app.feature.messageboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empowermom.app.core.data.repository.MessageRepository
import com.empowermom.app.core.data.repository.DraftRepository
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.messageboard.model.MessageCategory
import com.empowermom.app.feature.messageboard.model.PresetTags
import com.empowermom.app.feature.messageboard.model.CrisisKeywords
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

// ── UI State ──────────────────────────────────────────────────────────────────

data class MessageBoardUiState(
    val messages: List<Message> = emptyList(),
    val selectedCategory: MessageCategory? = null,  // null = 全部
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // 写留言编辑器状态
    val isEditorOpen: Boolean = false,
    val editorState: EditorState = EditorState()
)

data class EditorState(
    val content: String = "",
    val selectedCategory: MessageCategory? = null,
    val selectedTags: List<String> = emptyList(),
    val isAnonymous: Boolean = true,
    val nickname: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String? = null
) {
    val isValid: Boolean get() = content.isNotBlank() && selectedCategory != null && charCount < 500
    val charCount: Int get() = content.length
}

// ── Intent (用户意图) ──────────────────────────────────────────────────────────

sealed class MessageBoardIntent {
    data class SelectCategory(val category: MessageCategory?) : MessageBoardIntent()
    data object OpenEditor : MessageBoardIntent()
    data object CloseEditor : MessageBoardIntent()
    data class UpdateEditorContent(val content: String) : MessageBoardIntent()
    data class SelectEditorCategory(val category: MessageCategory) : MessageBoardIntent()
    data class ToggleTag(val tag: String) : MessageBoardIntent()
    data class SetAnonymous(val isAnonymous: Boolean) : MessageBoardIntent()
    data class UpdateNickname(val nickname: String) : MessageBoardIntent()
    data object SubmitMessage : MessageBoardIntent()
    data class ToggleLike(val messageId: Long) : MessageBoardIntent()
    data class ToggleResonance(val messageId: Long) : MessageBoardIntent()
}

// ── ViewModel ──────────────────────────────────────────────────────────────────

@HiltViewModel
class MessageBoardViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageBoardUiState(isLoading = true))
    val uiState: StateFlow<MessageBoardUiState> = _uiState.asStateFlow()

    // 危机帖发布后，发出跳转事件让 UI 跳转到详情页
    private val _navigateToDetail = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val navigateToDetail: SharedFlow<Long> = _navigateToDetail.asSharedFlow()

    // 当前分类筛选
    private val selectedCategory = MutableStateFlow<MessageCategory?>(null)

    val presetTags = PresetTags.all

    init {
        // 响应分类切换，重新订阅留言列表
        viewModelScope.launch {
            selectedCategory.collectLatest { category ->
                repository.observeMessages(category)
                    .catch { e ->
                        _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                    }
                    .collect { messages ->
                        _uiState.update { it.copy(messages = messages, isLoading = false) }
                    }
            }
        }
    }

    fun handleIntent(intent: MessageBoardIntent) {
        when (intent) {
            is MessageBoardIntent.SelectCategory -> onSelectCategory(intent.category)
            is MessageBoardIntent.OpenEditor -> openEditor()
            is MessageBoardIntent.CloseEditor -> closeEditor()
            is MessageBoardIntent.UpdateEditorContent -> updateContent(intent.content)
            is MessageBoardIntent.SelectEditorCategory -> selectEditorCategory(intent.category)
            is MessageBoardIntent.ToggleTag -> toggleTag(intent.tag)
            is MessageBoardIntent.SetAnonymous -> setAnonymous(intent.isAnonymous)
            is MessageBoardIntent.UpdateNickname -> updateNickname(intent.nickname)
            is MessageBoardIntent.SubmitMessage -> submitMessage()
            is MessageBoardIntent.ToggleLike -> toggleLike(intent.messageId)
            is MessageBoardIntent.ToggleResonance -> toggleResonance(intent.messageId)
        }
    }

    private fun onSelectCategory(category: MessageCategory?) {
        selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category, isLoading = true) }
    }

    private fun openEditor() {
        viewModelScope.launch {
            // 一次性读取草稿（combine 三个 Flow 取第一个值）
            val content = draftRepository.draftContent.first()
            val categoryStr = draftRepository.draftCategory.first()
            val tagsStr = draftRepository.draftTags.first()

            val category = if (categoryStr.isNotBlank()) {
                MessageCategory.entries.find { it.name == categoryStr }
            } else null

            val tags = if (tagsStr.isNotBlank()) {
                tagsStr.split(",").filter { it.isNotBlank() }
            } else emptyList()

            _uiState.update {
                it.copy(
                    isEditorOpen = true,
                    editorState = EditorState(
                        content = content,
                        selectedCategory = category,
                        selectedTags = tags
                    )
                )
            }
        }
    }

    private fun closeEditor() {
        _uiState.update { it.copy(isEditorOpen = false, editorState = EditorState()) }
    }

    private fun updateContent(content: String) {
        if (content.length > 500) return
        _uiState.update { it.copy(editorState = it.editorState.copy(content = content)) }
        // 自动保存草稿
        viewModelScope.launch {
            val editor = _uiState.value.editorState
            draftRepository.saveDraft(
                content = content,
                category = editor.selectedCategory?.name ?: "",
                tags = editor.selectedTags
            )
        }
    }

    private fun selectEditorCategory(category: MessageCategory) {
        _uiState.update { it.copy(editorState = it.editorState.copy(selectedCategory = category)) }
        viewModelScope.launch {
            val editor = _uiState.value.editorState
            draftRepository.saveDraft(
                content = editor.content,
                category = category.name,
                tags = editor.selectedTags
            )
        }
    }

    private fun toggleTag(tag: String) {
        val currentTags = _uiState.value.editorState.selectedTags.toMutableList()
        if (tag in currentTags) {
            currentTags.remove(tag)
        } else {
            if (currentTags.size >= 3) return // 最多3个标签
            currentTags.add(tag)
        }
        _uiState.update { it.copy(editorState = it.editorState.copy(selectedTags = currentTags)) }
        // 自动保存草稿
        viewModelScope.launch {
            val editor = _uiState.value.editorState
            draftRepository.saveDraft(
                content = editor.content,
                category = editor.selectedCategory?.name ?: "",
                tags = currentTags
            )
        }
    }

    private fun setAnonymous(isAnonymous: Boolean) {
        _uiState.update { it.copy(editorState = it.editorState.copy(isAnonymous = isAnonymous)) }
    }

    private fun updateNickname(nickname: String) {
        _uiState.update { it.copy(editorState = it.editorState.copy(nickname = nickname)) }
    }

    private fun submitMessage() {
        val editor = _uiState.value.editorState
        Log.d("Submit", "1. 进入 submitMessage, content=${editor.content}, category=${editor.selectedCategory}, tags=${editor.selectedTags}")

        if (!editor.isValid) {
            Log.d("Submit", "2. 校验未通过！isValid=false. content非空=${editor.content.isNotBlank()}, 选了分区=${editor.selectedCategory != null}")
            return
        }
        Log.d("Submit", "2. 校验通过，准备调 repository")

        // 逐项校验，给出具体提示
        val validationError = when {
            editor.selectedCategory == null -> "请选择主题分区"
            editor.content.isBlank() -> "请输入内容"
            !editor.isAnonymous && editor.nickname.isBlank() -> "请输入昵称"
            else -> null
        }
        if (validationError != null) {
            _uiState.update {
                it.copy(editorState = it.editorState.copy(submitError = validationError))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(editorState = it.editorState.copy(isSubmitting = true)) }
            try {
                val author = if (editor.isAnonymous || editor.nickname.isBlank()) "匿名妈妈" else editor.nickname
                Log.d("Submit", "3. 开始写数据库, author=$author")

                val newId = repository.postMessage(
                    content = editor.content,
                    category = editor.selectedCategory!!,
                    tags = editor.selectedTags,
                    author = author,
                    isAnonymous = editor.isAnonymous
                )
                draftRepository.clearDraft()
                Log.d("Submit", "4. 数据库写入成功! 新留言ID=$newId")
                closeEditor()
                Log.d("Submit", "5. 编辑器已关闭")

                // 危机帖：发出跳转事件，让 UI 跳转到详情页
                // 因为列表已过滤 isHidden，作者本人也看不到，所以发完直接跳详情页让作者看到心理援助卡片
                if (CrisisKeywords.detect(editor.content)) {
                    Log.d("Submit", "5.5 ⚠️ 检测到危机内容，发出跳转事件，目标ID=$newId")
                    _navigateToDetail.tryEmit(newId)
                }

                // 异步触发 AI 回应（不阻塞 UI）
                viewModelScope.launch {
                    try {
                        Log.d("Submit", "6. 开始请求 AI 回应...")
                        val aiResponse = repository.generateAiResponse(editor.content)
                        repository.updateAiResponse(newId, aiResponse)
                        Log.d("Submit", "7. ✅ AI 回应已写入: $aiResponse")
                    } catch (e: Exception) {
                        Log.e("Submit", "❌ AI 回应失败: ${e.message}", e)
                        repository.updateAiResponse(newId, "谢谢你愿意分享这些。")
                    }
                }
            } catch (e: Exception) {
                Log.e("Submit", "❌ 发布失败！异常: ${e.message}", e)
                _uiState.update {
                    it.copy(editorState = it.editorState.copy(
                        isSubmitting = false,
                        submitError = "发布失败，请重试"
                    ))
                }
            }
        }
    }

    private fun toggleLike(messageId: Long) {
        viewModelScope.launch {
            try {
                repository.toggleLike(messageId)
            } catch (e: Exception) { /* 静默失败 */ }
        }
    }

    private fun toggleResonance(messageId: Long) {
        viewModelScope.launch {
            try {
                repository.toggleResonance(messageId)
            } catch (e: Exception) { /* 静默失败 */ }
        }
    }
}
