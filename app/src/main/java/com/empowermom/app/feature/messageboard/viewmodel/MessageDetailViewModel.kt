package com.empowermom.app.feature.messageboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empowermom.app.core.data.repository.MessageRepository
import com.empowermom.app.feature.messageboard.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageDetailUiState(
    val message: Message? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // 回复发送状态
    val isPostingReply: Boolean = false,
    val replyError: String? = null
)

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState(isLoading = true))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    fun loadMessage(messageId: Long) {
        viewModelScope.launch {
            repository.observeMessageWithReplies(messageId).collect { message ->
                _uiState.update { current ->
                    current.copy(
                        message = message,
                        isLoading = false,
                        error = if (message == null) "留言不存在或已被删除" else null
                    )
                }
            }
        }
    }

    fun toggleLike(messageId: Long) {
        viewModelScope.launch { repository.toggleLike(messageId) }
    }

    fun toggleResonance(messageId: Long) {
        viewModelScope.launch { repository.toggleResonance(messageId) }
    }

    /**
     * 发送回复。
     * @param onSuccess 成功后的回调，UI 用它清空输入框
     */
    fun postReply(messageId: Long, content: String, onSuccess: () -> Unit) {
        // 防重复点击：正在发送时直接忽略
        if (_uiState.value.isPostingReply) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPostingReply = true, replyError = null) }
            try {
                repository.postReply(
                    messageId = messageId,
                    content = content,
                    author = "匿名妈妈",
                    isAnonymous = true
                )
                _uiState.update { it.copy(isPostingReply = false, replyError = null) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPostingReply = false,
                        replyError = "发送失败，请重试"
                    )
                }
            }
        }
    }

    /**
     * 清除错误提示（用户重新编辑时调用）
     */
    fun clearReplyError() {
        _uiState.update { it.copy(replyError = null) }
    }
}