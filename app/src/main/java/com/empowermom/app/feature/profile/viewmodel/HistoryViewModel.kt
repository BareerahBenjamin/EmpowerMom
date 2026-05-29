package com.empowermom.app.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empowermom.app.core.data.repository.DailyLogRepository
import com.empowermom.app.core.data.repository.MessageRepository
import com.empowermom.app.core.data.repository.UserRepository
import com.empowermom.app.feature.dailylog.model.DailyLog
import com.empowermom.app.feature.messageboard.model.Message
import com.empowermom.app.feature.profile.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val dailyLogs: List<DailyLog> = emptyList(),
    val messages: List<Message> = emptyList(),
    val userProfile: UserProfile = UserProfile()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    dailyLogRepository: DailyLogRepository,
    messageRepository: MessageRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dailyLogRepository.observeAll().collect { logs ->
                _uiState.update { it.copy(dailyLogs = logs) }
            }
        }
        viewModelScope.launch {
            messageRepository.observeMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            userRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }
}
