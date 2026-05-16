package com.empowermom.app.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.empowermom.app.feature.profile.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_profile")

@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_NICKNAME     = stringPreferencesKey("nickname")
    private val KEY_AVATAR       = stringPreferencesKey("avatar_emoji")
    private val KEY_BABY_AGE     = intPreferencesKey("baby_age_days")
    private val KEY_LOGGED_IN    = booleanPreferencesKey("is_logged_in")

    // 实时观察用户资料
    val userProfile: Flow<UserProfile> = context.userDataStore.data.map { prefs ->
        UserProfile(
            nickname    = prefs[KEY_NICKNAME]  ?: "",
            avatarEmoji = prefs[KEY_AVATAR]    ?: "🌸",
            babyAgeDays = prefs[KEY_BABY_AGE]  ?: 0,
            isLoggedIn  = prefs[KEY_LOGGED_IN] ?: false
        )
    }

    // 保存资料并标记已登录
    suspend fun saveProfile(profile: UserProfile) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_NICKNAME]  = profile.nickname
            prefs[KEY_AVATAR]    = profile.avatarEmoji
            prefs[KEY_BABY_AGE]  = profile.babyAgeDays
            prefs[KEY_LOGGED_IN] = true
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
}
