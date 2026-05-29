package com.empowermom.app.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: Long = 0,
    @SerialName("user_id")          val userId: String? = null,
    val content: String,
    val author: String,
    val category: String,
    @SerialName("tags_json")        val tagsJson: String = "[]",
    @SerialName("attachments_json") val attachmentsJson: String = "[]",
    val timestamp: Long,
    val likes: Int = 0,
    val resonances: Int = 0,
    @SerialName("is_anonymous")     val isAnonymous: Boolean = true,
    @SerialName("ai_response")      val aiResponse: String = "",
    @SerialName("is_crisis")        val isCrisis: Boolean = false,
    @SerialName("is_hidden")        val isHidden: Boolean = false,
    @SerialName("is_private_only")  val isPrivateOnly: Boolean = false,
    @SerialName("sync_status")      val syncStatus: String = "synced"
)

@Serializable
data class ReplyDto(
    val id: Long = 0,
    @SerialName("message_id")   val messageId: Long,
    @SerialName("user_id")      val userId: String? = null,
    val content: String,
    val author: String,
    val timestamp: Long,
    @SerialName("is_anonymous") val isAnonymous: Boolean = true
)

@Serializable
data class InteractionDto(
    @SerialName("user_id")          val userId: String,
    @SerialName("message_id")       val messageId: Long,
    @SerialName("interaction_type") val interactionType: String,
    val timestamp: Long
)

@Serializable
data class DailyLogDto(
    val id: Long = 0,
    @SerialName("user_id")      val userId: String? = null,
    val date: Long,
    @SerialName("q1_type")      val q1Type: String = "",
    @SerialName("q1_answer")    val q1Answer: String = "",
    @SerialName("q1_color")     val q1Color: String = "",
    @SerialName("q2_question")  val q2Question: String = "",
    @SerialName("q2_answer")    val q2Answer: String = "",
    @SerialName("q3_question")  val q3Question: String = "",
    @SerialName("q3_text")      val q3Text: String = "",
    @SerialName("ai_card_text") val aiCardText: String = "",
    @SerialName("is_private")   val isPrivate: Boolean = false
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id")          val userId: String,
    val nickname: String = "",
    @SerialName("avatar_emoji")     val avatarEmoji: String = "",
    @SerialName("avatar_photo_path") val avatarPhotoPath: String = "",
    @SerialName("baby_age_days")    val babyAgeDays: Int = 0,
    @SerialName("updated_at")       val updatedAt: String? = null
)
