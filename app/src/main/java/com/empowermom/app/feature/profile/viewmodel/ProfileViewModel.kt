package com.empowermom.app.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.empowermom.app.core.data.repository.AuthRepository
import com.empowermom.app.core.data.repository.SupabaseRepository
import com.empowermom.app.core.data.repository.UserRepository
import com.empowermom.app.feature.profile.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ───────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val accountEmail: String? = null,
    val isLoading: Boolean = true,
    // 是否已完成「从 Supabase 拉取资料」的首次检查。
    // 用于区分「新注册用户（远程也没有资料）」与「老用户登录（远程有资料，正在拉取）」，
    // 避免在远程资料到达前就误弹个人信息设置页。
    val remoteChecked: Boolean = false,

    // 编辑状态（填写/修改资料时用）
    val isEditing: Boolean = false,
    val editNickname: String = "",
    val editAvatar: String = "🌸",
    val editAvatarPhotoPath: String = "",
    val editBabyAgeDays: Int = 0,
    val nicknameError: String? = null
)

// ── Intent ────────────────────────────────────────────────────────────────────

sealed class ProfileIntent {
    data object StartEdit : ProfileIntent()
    data object CancelEdit : ProfileIntent()
    data object SaveProfile : ProfileIntent()
    data object SignOut : ProfileIntent()
    data class UpdateNickname(val value: String) : ProfileIntent()
    data class UpdateAvatar(val emoji: String) : ProfileIntent()
    data class UpdateAvatarPhoto(val path: String) : ProfileIntent()
    data class UpdateBabyAgeDays(val days: Int) : ProfileIntent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val supabaseRepository: SupabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.userProfile.collect { profile ->
                _uiState.update { state ->
                    state.copy(
                        profile = profile,
                        isLoading = false,
                        // 资料未完成 → 自动进入编辑（新注册引导填写）；
                        // 资料已完成（含远程拉取回来后）→ 退出编辑，显示资料视图，
                        // 仅当用户通过"编辑"按钮显式进入时才由 startEdit() 置回 true。
                        isEditing = if (!profile.isProfileComplete) true else false,
                        editNickname = if (state.isEditing) state.editNickname else profile.nickname,
                        editAvatar = if (state.isEditing) state.editAvatar else profile.avatarEmoji,
                        editAvatarPhotoPath = if (state.isEditing) state.editAvatarPhotoPath else profile.avatarPhotoPath,
                        editBabyAgeDays = if (state.isEditing) state.editBabyAgeDays else profile.babyAgeDays
                    )
                }
            }
        }
        // 监听登录状态：用户变为已登录时才拉取远程资料。
        // 关键：ViewModel 在会话恢复/登录完成之前就创建了，若只在 init 里拉一次，
        // 那一刻 currentUserId 还是 null → refreshFromRemote 直接 return，永远拉不到。
        // 因此改为跟随 currentUser，登录就绪后再拉，老用户资料才能被恢复。
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(accountEmail = authRepository.currentUserEmail()) }
                if (user != null) {
                    try {
                        val diag = supabaseRepository.testTableAccess()
                        Log.d("Profile", "Supabase 表诊断: $diag")
                        userRepository.refreshFromRemote()
                    } catch (e: Exception) {
                        Log.e("Profile", "拉取远程用户资料失败: ${e.message}")
                    } finally {
                        // 已带着真实 userId 尝试过拉取，门控可据此决定是否引导新用户填写
                        _uiState.update { it.copy(remoteChecked = true) }
                    }
                }
            }
        }
    }

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.StartEdit  -> startEdit()
            ProfileIntent.CancelEdit -> cancelEdit()
            ProfileIntent.SaveProfile -> saveProfile()
            ProfileIntent.SignOut    -> signOut()
            is ProfileIntent.UpdateNickname    -> _uiState.update {
                it.copy(editNickname = intent.value, nicknameError = null)
            }
            is ProfileIntent.UpdateAvatar      -> _uiState.update {
                it.copy(editAvatar = intent.emoji, editAvatarPhotoPath = "")
            }
            is ProfileIntent.UpdateAvatarPhoto -> _uiState.update {
                it.copy(editAvatarPhotoPath = intent.path)
            }
            is ProfileIntent.UpdateBabyAgeDays -> _uiState.update {
                it.copy(editBabyAgeDays = intent.days)
            }
        }
    }

    private fun startEdit() {
        val p = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = true,
                editNickname = p.nickname,
                editAvatar = p.avatarEmoji,
                editAvatarPhotoPath = p.avatarPhotoPath,
                editBabyAgeDays = p.babyAgeDays,
                nicknameError = null
            )
        }
    }

    private fun cancelEdit() {
        if (!_uiState.value.profile.isProfileComplete) return
        _uiState.update { it.copy(isEditing = false) }
    }

    private fun saveProfile() {
        val state = _uiState.value
        val nickname = state.editNickname.trim()

        if (nickname.isBlank()) {
            _uiState.update { it.copy(nicknameError = "昵称不能为空") }
            return
        }
        if (nickname.length > 12) {
            _uiState.update { it.copy(nicknameError = "昵称最多 12 个字") }
            return
        }

        viewModelScope.launch {
            userRepository.saveProfile(
                UserProfile(
                    nickname    = nickname,
                    avatarEmoji = state.editAvatar,
                    avatarPhotoPath = state.editAvatarPhotoPath,
                    babyAgeDays = state.editBabyAgeDays,
                    isProfileComplete = true
                )
            )
            _uiState.update { it.copy(isEditing = false, nicknameError = null) }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            userRepository.logout()
        }
    }
}
