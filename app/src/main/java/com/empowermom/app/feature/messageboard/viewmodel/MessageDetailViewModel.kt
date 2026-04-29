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
    val error: String? = null
)

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageDetailUiState(isLoading = true))
    val uiState: StateFlow<MessageDetailUiState> = _uiState.asStateFlow()

    fun loadMessage(messageId: Long) {
        // TODO: 从 repository 加载完整留言（含回复）
        // 实际实现时用 repository.observeMessageWithReplies(messageId) 流式更新
    }

    fun toggleLike(messageId: Long) {
        viewModelScope.launch { repository.toggleLike(messageId) }
    }

    fun toggleResonance(messageId: Long) {
        viewModelScope.launch { repository.toggleResonance(messageId) }
    }

    fun postReply(messageId: Long, content: String) {
        viewModelScope.launch {
            repository.postReply(
                messageId = messageId,
                content = content,
                author = "匿名妈妈",
                isAnonymous = true
            )
        }
    }
}
