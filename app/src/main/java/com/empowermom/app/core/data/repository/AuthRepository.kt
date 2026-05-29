package com.empowermom.app.core.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {

    // 当前登录用户（Flow，UI 自动响应登录状态变化）
    val currentUser: Flow<UserInfo?> = client.auth.sessionStatus.map { status ->
        client.auth.currentUserOrNull()
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    fun currentUserEmail(): String? = client.auth.currentUserOrNull()?.email

    // 邮箱注册
    suspend fun signUpWithEmail(email: String, password: String) {
        client.auth.signUpWith(Email, redirectUrl = AUTH_CALLBACK_URL) {
            this.email    = email
            this.password = password
        }
    }

    // 邮箱登录
    suspend fun signInWithEmail(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email    = email
            this.password = password
        }
    }

    // 退出登录
    suspend fun signOut() {
        client.auth.signOut()
    }

    // 是否已登录
    fun isLoggedIn(): Boolean = client.auth.currentUserOrNull() != null
    private companion object {
        const val AUTH_CALLBACK_URL = "empowermom://auth-callback"
    }
}
