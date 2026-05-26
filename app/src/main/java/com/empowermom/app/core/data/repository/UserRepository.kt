package com.empowermom.app.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.empowermom.app.core.data.remote.dto.UserProfileDto
import com.empowermom.app.feature.profile.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_profile")

@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseRepository: SupabaseRepository,
    private val authRepository: AuthRepository
) {
    private val KEY_NICKNAME     = stringPreferencesKey("nickname")
    private val KEY_AVATAR       = stringPreferencesKey("avatar_emoji")
    private val KEY_AVATAR_PHOTO = stringPreferencesKey("avatar_photo_path")
    private val KEY_BABY_AGE     = intPreferencesKey("baby_age_days")
    private val KEY_PROFILE_COMPLETE = booleanPreferencesKey("is_logged_in")

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "UserRepository"

    // 实时观察用户资料
    val userProfile: Flow<UserProfile> = context.userDataStore.data.map { prefs ->
        UserProfile(
            nickname    = prefs[KEY_NICKNAME]  ?: "",
            avatarEmoji = prefs[KEY_AVATAR]    ?: "🌸",
            avatarPhotoPath = prefs[KEY_AVATAR_PHOTO] ?: "",
            babyAgeDays = prefs[KEY_BABY_AGE]  ?: 0,
            isProfileComplete = prefs[KEY_PROFILE_COMPLETE] ?: false
        )
    }

    // 保存资料并标记已完成填写
    suspend fun saveProfile(profile: UserProfile) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_NICKNAME]  = profile.nickname
            prefs[KEY_AVATAR]    = profile.avatarEmoji
            prefs[KEY_AVATAR_PHOTO] = profile.avatarPhotoPath
            prefs[KEY_BABY_AGE]  = profile.babyAgeDays
            prefs[KEY_PROFILE_COMPLETE] = true
        }

        // 同步到 Supabase
        syncScope.launch {
            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    Log.w(TAG, "用户未登录，资料仅保存在本地")
                    return@launch
                }
                // 如果头像路径是本地文件，上传到 Supabase Storage
                var avatarUrl = ""
                if (profile.avatarPhotoPath.isNotBlank()) {
                    val file = File(profile.avatarPhotoPath)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        avatarUrl = supabaseRepository.uploadAvatar(userId, bytes)
                        Log.d(TAG, "头像已上传到 Storage: $avatarUrl")
                    } else if (profile.avatarPhotoPath.startsWith("http")) {
                        avatarUrl = profile.avatarPhotoPath
                    }
                }

                val dto = UserProfileDto(
                    userId = userId,
                    nickname = profile.nickname,
                    avatarEmoji = profile.avatarEmoji,
                    avatarPhotoPath = avatarUrl,
                    babyAgeDays = profile.babyAgeDays
                )
                Log.d(TAG, "开始同步用户资料到 Supabase: userId=$userId, dto=$dto")
                supabaseRepository.upsertUserProfile(dto)
                Log.d(TAG, "用户资料同步成功")
            } catch (e: Exception) {
                Log.e(TAG, "用户资料同步失败: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    // 更新昵称
    suspend fun updateNickname(nickname: String) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = nickname
        }
    }

    // 更新头像
    suspend fun updateAvatar(emoji: String) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_AVATAR] = emoji
        }
    }

    // 退出登录（清空资料）
    suspend fun logout() {
        context.userDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    // 从 Supabase 拉取用户资料
    suspend fun refreshFromRemote() {
        try {
            val userId = authRepository.currentUserId() ?: return
            val dto = supabaseRepository.fetchUserProfile(userId) ?: return
            val current = userProfile.first()

            // 只有远程数据更新时才覆盖本地（避免覆盖本地更完整的头像路径）
            if (dto.nickname.isNotBlank() && dto.nickname != current.nickname) {
                context.userDataStore.edit { prefs ->
                    prefs[KEY_NICKNAME] = dto.nickname
                    prefs[KEY_AVATAR] = dto.avatarEmoji
                    prefs[KEY_BABY_AGE] = dto.babyAgeDays
                    prefs[KEY_PROFILE_COMPLETE] = true
                }
                // 如果远程有头像 URL，下载到本地缓存
                if (dto.avatarPhotoPath.isNotBlank() && dto.avatarPhotoPath.startsWith("http")) {
                    try {
                        val localFile = File(context.filesDir, "avatar_photo.jpg")
                        val url = java.net.URL(dto.avatarPhotoPath)
                        url.openStream().use { input ->
                            localFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        context.userDataStore.edit { prefs ->
                            prefs[KEY_AVATAR_PHOTO] = localFile.absolutePath
                        }
                        Log.d(TAG, "远程头像已下载到本地: ${localFile.absolutePath}")
                    } catch (e: Exception) {
                        Log.e(TAG, "下载远程头像失败: ${e.message}", e)
                    }
                }
                Log.d(TAG, "远程用户资料已同步到本地")
            }
        } catch (e: Exception) {
            Log.e(TAG, "拉取远程用户资料失败: ${e.message}", e)
        }
    }
}
