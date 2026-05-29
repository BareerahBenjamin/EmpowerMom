package com.empowermom.app.core.data.repository

import com.empowermom.app.core.data.remote.dto.DailyLogDto
import com.empowermom.app.core.data.remote.dto.InteractionDto
import com.empowermom.app.core.data.remote.dto.MessageDto
import com.empowermom.app.core.data.remote.dto.ReplyDto
import com.empowermom.app.core.data.remote.dto.UserProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val TAG = "SupabaseRepository"

    // ── 诊断 ────────────────────────────────────────────────────────────────

    /** 测试各表是否可访问，返回每张表的诊断结果 */
    suspend fun testTableAccess(): Map<String, String> {
        val results = mutableMapOf<String, String>()

        suspend fun test(name: String, block: suspend () -> Unit) {
            try {
                block()
                results[name] = "OK"
            } catch (e: Exception) {
                results[name] = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "表 $name 访问失败: ${e.message}", e)
            }
        }

        test("messages") {
            client.postgrest["messages"].select { limit(1) }.decodeList<MessageDto>()
        }
        test("daily_logs") {
            client.postgrest["daily_logs"].select { limit(1) }.decodeList<DailyLogDto>()
        }
        test("user_profiles") {
            client.postgrest["user_profiles"].select { limit(1) }.decodeList<UserProfileDto>()
        }

        Log.d(TAG, "表访问诊断结果: $results")
        return results
    }

    // ── 留言 ────────────────────────────────────────────────────────────────

    suspend fun fetchMessages(category: String? = null): List<MessageDto> =
        client.postgrest["messages"].select {
            filter {
                eq("is_hidden", false)
                if (category != null) eq("category", category)
            }
            order("timestamp", Order.DESCENDING)
            limit(50)
        }.decodeList<MessageDto>()

    suspend fun insertMessage(dto: MessageDto): MessageDto {
        Log.d(TAG, "insertMessage → table=messages, userId=${dto.userId}")
        val result = client.postgrest["messages"].insert(dto) { select() }.decodeSingle<MessageDto>()
        Log.d(TAG, "insertMessage 成功: remoteId=${result.id}")
        return result
    }

    suspend fun updateAiResponse(remoteId: Long, response: String) {
        client.postgrest["messages"].update(
            { set("ai_response", response) }
        ) { filter { eq("id", remoteId) } }
    }

    suspend fun deleteMessage(remoteId: Long) {
        client.postgrest["messages"].delete {
            filter { eq("id", remoteId) }
        }
    }

    suspend fun deleteRepliesByMessage(remoteId: Long) {
        client.postgrest["replies"].delete {
            filter { eq("message_id", remoteId) }
        }
    }

    // ── 回复 ────────────────────────────────────────────────────────────────

    suspend fun fetchReplies(messageId: Long): List<ReplyDto> =
        client.postgrest["replies"].select {
            filter { eq("message_id", messageId) }
            order("timestamp", Order.ASCENDING)
        }.decodeList<ReplyDto>()

    suspend fun insertReply(dto: ReplyDto): ReplyDto =
        client.postgrest["replies"].insert(dto) { select() }.decodeSingle<ReplyDto>()

    suspend fun deleteReply(remoteId: Long) {
        client.postgrest["replies"].delete {
            filter { eq("id", remoteId) }
        }
    }

    // ── 互动（点赞/共鸣）────────────────────────────────────────────────────

    suspend fun upsertInteraction(dto: InteractionDto) {
        client.postgrest["user_interactions"].upsert<InteractionDto>(dto)
    }

    suspend fun deleteInteraction(userId: String, messageId: Long, type: String) {
        client.postgrest["user_interactions"].delete {
            filter {
                eq("user_id", userId)
                eq("message_id", messageId)
                eq("interaction_type", type)
            }
        }
    }

    suspend fun fetchUserInteractions(userId: String): List<InteractionDto> =
        client.postgrest["user_interactions"].select {
            filter { eq("user_id", userId) }
        }.decodeList<InteractionDto>()

    // ── 每日速记 ────────────────────────────────────────────────────────

    suspend fun fetchDailyLogs(userId: String): List<DailyLogDto> =
        client.postgrest["daily_logs"].select {
            filter { eq("user_id", userId) }
            order("date", Order.DESCENDING)
        }.decodeList<DailyLogDto>()

    suspend fun insertDailyLog(dto: DailyLogDto): DailyLogDto {
        Log.d(TAG, "insertDailyLog → table=daily_logs, userId=${dto.userId}, date=${dto.date}")
        return try {
            val result = client.postgrest["daily_logs"].insert(dto) { select() }.decodeSingle<DailyLogDto>()
            Log.d(TAG, "insertDailyLog 成功: remoteId=${result.id}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "insertDailyLog 失败: ${e.javaClass.simpleName}: ${e.message}", e)
            // 回退：不带 select 的 insert（如果 select 因 RLS 失败，数据可能已插入）
            try {
                client.postgrest["daily_logs"].insert(dto)
                Log.d(TAG, "insertDailyLog(无select) 成功，重新读取...")
                // 重新读取刚插入的记录
                val fetched = client.postgrest["daily_logs"].select {
                    filter {
                        eq("user_id", dto.userId ?: "")
                        eq("date", dto.date)
                    }
                    order("id", Order.DESCENDING)
                    limit(1)
                }.decodeList<DailyLogDto>().firstOrNull()
                if (fetched != null) {
                    Log.d(TAG, "重新读取成功: remoteId=${fetched.id}")
                    return fetched
                }
            } catch (e2: Exception) {
                Log.e(TAG, "insertDailyLog(无select) 也失败: ${e2.message}", e2)
            }
            throw e
        }
    }

    suspend fun updateDailyLogAiText(remoteId: Long, aiCardText: String) {
        client.postgrest["daily_logs"].update(
            { set("ai_card_text", aiCardText) }
        ) { filter { eq("id", remoteId) } }
    }

    suspend fun updateDailyLogPrivacy(remoteId: Long, isPrivate: Boolean) {
        client.postgrest["daily_logs"].update(
            { set("is_private", isPrivate) }
        ) { filter { eq("id", remoteId) } }
    }

    // ── 头像上传 ────────────────────────────────────────────────────────

    suspend fun uploadAvatar(userId: String, fileBytes: ByteArray): String {
        val path = "$userId/avatar.jpg"
        Log.d(TAG, "上传头像到 Storage: path=$path, size=${fileBytes.size}")
        client.storage["avatars"].upload(path, fileBytes) {
            upsert = true
        }
        val url = client.storage["avatars"].publicUrl(path)
        Log.d(TAG, "头像上传成功: url=$url")
        return url
    }

    // ── 用户资料 ────────────────────────────────────────────────────────

    suspend fun fetchUserProfile(userId: String): UserProfileDto? =
        try {
            client.postgrest["user_profiles"].select {
                filter { eq("user_id", userId) }
                limit(1)
            }.decodeList<UserProfileDto>().firstOrNull()
        } catch (e: Exception) {
            null
        }

    suspend fun upsertUserProfile(dto: UserProfileDto): UserProfileDto {
        Log.d(TAG, "upsertUserProfile → table=user_profiles, userId=${dto.userId}, nickname=${dto.nickname}")
        return try {
            val result = client.postgrest["user_profiles"].upsert(dto) { select() }.decodeSingle<UserProfileDto>()
            Log.d(TAG, "upsertUserProfile 成功")
            result
        } catch (e: Exception) {
            Log.e(TAG, "upsertUserProfile 失败: ${e.javaClass.simpleName}: ${e.message}", e)
            // 回退：不带 select 的 upsert
            try {
                client.postgrest["user_profiles"].upsert(dto)
                Log.d(TAG, "upsertUserProfile(无select) 成功")
                return dto
            } catch (e2: Exception) {
                Log.e(TAG, "upsertUserProfile(无select) 也失败: ${e2.message}", e2)
            }
            throw e
        }
    }
}
