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
