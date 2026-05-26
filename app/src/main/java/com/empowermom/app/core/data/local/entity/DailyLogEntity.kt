package com.empowermom.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = Date().time,
    val q1Type: String = "",
    val q1Answer: String = "",
    val q1Color: String = "",
    val q2Question: String = "",
    val q2Answer: String = "",
    val q3Question: String = "",
    val q3Text: String = "",
    val aiCardText: String = "",
    val isPrivate: Boolean = false,
    val remoteId: Long? = null,
    val syncStatus: String = "local",
    val userId: String? = null
)
