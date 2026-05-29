package com.empowermom.app.feature.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empowermom.app.core.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val mode: AuthMode = AuthMode.Login
)

enum class AuthMode {
    Login,
    SignUp
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private companion object {
        const val TAG = "AuthViewModel"
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isAuthenticated = authRepository.isLoggedIn()) }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(isAuthenticated = user != null) }
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        authenticate(email, password, onSuccess) { cleanEmail, cleanPassword ->
            authRepository.signInWithEmail(cleanEmail, cleanPassword)
        }
    }

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        authenticate(email, password, onSuccess) { cleanEmail, cleanPassword ->
            authRepository.signUpWithEmail(cleanEmail, cleanPassword)
        }
    }

    fun setMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    private fun authenticate(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        action: suspend (String, String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入邮箱和密码。") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "密码至少需要 6 位。") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                action(cleanEmail, password)
            }.onSuccess {
                val isAuthenticated = authRepository.isLoggedIn()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = isAuthenticated,
                        errorMessage = if (isAuthenticated) {
                            null
                        } else {
                            "注册成功。请先在邮箱中确认账号，然后再登录。"
                        }
                    )
                }
                if (isAuthenticated) {
                    onSuccess()
                }
            }.onFailure { error ->
                Log.e(TAG, "Authentication failed", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.toFriendlyMessage()
                    )
                }
            }
        }
    }

    private fun Throwable.toFriendlyMessage(): String {
        val detail = message.orEmpty()
        return when {
            detail.contains("invalid_credentials", ignoreCase = true) ->
                "邮箱或密码不正确。如果还没有账号，请先注册。"
            detail.contains("email", ignoreCase = true) &&
                detail.contains("confirm", ignoreCase = true) ->
                "请先在邮箱中确认账号，再登录。"
            detail.contains("already", ignoreCase = true) &&
                detail.contains("registered", ignoreCase = true) ->
                "这个邮箱已经注册过，请直接登录。"
            detail.contains("network", ignoreCase = true) ||
                detail.contains("timeout", ignoreCase = true) ->
                "网络连接失败，请检查网络后重试。"
            else -> "操作失败，请稍后再试。"
        }
    }
}
