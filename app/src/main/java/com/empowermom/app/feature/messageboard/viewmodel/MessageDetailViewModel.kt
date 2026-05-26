package com.empowermom.app.feature.messageboard.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empowermom.app.core.data.repository.AuthRepository
import com.empowermom.app.core.data.repository.MessageRepository
import com.empowermom.app.core.data.repository.UserRepository
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.profile.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageDetailUiState(
    val message: Message? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // 回复发送状态
    val isPostingReply: Boolean = false,
    val replyError: String? = null,
    val isAnonymous: Boolean = true
)

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState(isLoading = true))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUserId()

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserProfile()
    )

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
                val isAnonymous = _uiState.value.isAnonymous
                val profile = userRepository.userProfile.first()
                val author = if (isAnonymous) {
                    "momo"
                } else {
                    profile.nickname.ifBlank { "momo" }
                }
                repository.postReply(
                    messageId = messageId,
                    content = content,
                    author = author,
                    isAnonymous = isAnonymous
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

    fun toggleAnonymous(isAnonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = isAnonymous) }
    }

    /**
     * 清除错误提示（用户重新编辑时调用）
     */
    fun clearReplyError() {
        _uiState.update { it.copy(replyError = null) }
    }

    fun deleteMessage(messageId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteMessage(messageId)
                if (success) onDeleted()
            } catch (e: Exception) {
                Log.e("MessageDetail", "删除留言失败: ${e.message}", e)
            }
        }
    }

    fun deleteReply(replyId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteReply(replyId)
            } catch (e: Exception) {
                Log.e("MessageDetail", "删除回复失败: ${e.message}", e)
            }
        }
    }
}
