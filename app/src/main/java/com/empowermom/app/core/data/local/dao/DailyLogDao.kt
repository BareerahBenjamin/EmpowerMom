package com.empowermom.app.core.data.local.dao

import androidx.room.*
import com.empowermom.app.core.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    // ── UI 查询（按 userId 隔离）──────────────────────────────────────────

    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC")
    fun observeAll(userId: String): Flow<List<DailyLogEntity>>

    // 仅非私密记录（用于月历、时间线展示）
    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND isPrivate = 0 ORDER BY date DESC")
    fun observePublicLogs(userId: String): Flow<List<DailyLogEntity>>

    // 查询最近 7 条，用于生成本周小结
    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    suspend fun getRecentLogs(userId: String): List<DailyLogEntity>

    // 查询最近 7 条非私密记录（用于本周小结）
    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND isPrivate = 0 ORDER BY date DESC LIMIT 7")
    suspend fun getRecentPublicLogs(userId: String): List<DailyLogEntity>

    // 查询今天是否已经记录过
    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun getTodayLog(userId: String, startOfDay: Long, endOfDay: Long): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DailyLogEntity?

    @Query("SELECT COUNT(*) FROM daily_logs WHERE userId = :userId")
    suspend fun getTotalCount(userId: String): Int

    // ── 写入 ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DailyLogEntity): Long

    @Query("UPDATE daily_logs SET aiCardText = :text WHERE id = :id")
    suspend fun updateAiCardText(id: Long, text: String)

    @Query("UPDATE daily_logs SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun updatePrivacy(id: Long, isPrivate: Boolean)

    // ── 同步相关 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM daily_logs WHERE syncStatus = 'local'")
    suspend fun getUnsyncedLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId")
    suspend fun getLogsByUser(userId: String): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): DailyLogEntity?

    @Query("UPDATE daily_logs SET remoteId = :remoteId, syncStatus = 'synced' WHERE id = :localId")
    suspend fun markSynced(localId: Long, remoteId: Long)

    @Query("UPDATE daily_logs SET aiCardText = :text, syncStatus = 'local' WHERE id = :id")
    suspend fun updateAiCardTextAndMarkDirty(id: Long, text: String)

    // 切换账号时清理其他用户的数据
    @Query("DELETE FROM daily_logs WHERE userId != :currentUserId AND userId IS NOT NULL")
    suspend fun deleteOtherUsersData(currentUserId: String)
}