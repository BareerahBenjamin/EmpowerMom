package com.empowermom.app.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * DeepSeek API 服务接口
 * 文档：https://api-docs.deepseek.com/
 */
interface DeepSeekApiService {

    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse
}

// ── 请求体 ────────────────────────────────────────────────────────────────────

data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 200
)

data class DeepSeekMessage(
    val role: String, // "system" 或 "user"
    val content: String
)

// ── 响应体 ────────────────────────────────────────────────────────────────────

data class DeepSeekResponse(
    val id: String,
    val choices: List<Choice>
) {
    data class Choice(
        val index: Int,
        val message: DeepSeekMessage,
        @SerializedName("finish_reason")
        val finishReason: String?
    )
}

