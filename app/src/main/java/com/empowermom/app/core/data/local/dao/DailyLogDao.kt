package com.empowermom.app.core.data.local.dao

import androidx.room.*
import com.empowermom.app.core.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyLogEntity>>

    // 查询最近 7 条，用于生成本周小结
    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT 7")
    suspend fun getRecentLogs(): List<DailyLogEntity>

    // 查询今天是否已经记录过
    @Query("SELECT * FROM daily_logs WHERE date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun getTodayLog(startOfDay: Long, endOfDay: Long): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DailyLogEntity): Long

    @Query("UPDATE daily_logs SET aiCardText = :text WHERE id = :id")
    suspend fun updateAiCardText(id: Long, text: String)

    @Query("UPDATE daily_logs SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun updatePrivacy(id: Long, isPrivate: Boolean)

    @Query("SELECT COUNT(*) FROM daily_logs")
    suspend fun getTotalCount(): Int

    // ── 同步相关 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM daily_logs WHERE syncStatus = 'local'")
    suspend fun getUnsyncedLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): DailyLogEntity?

    @Query("UPDATE daily_logs SET remoteId = :remoteId, syncStatus = 'synced' WHERE id = :localId")
    suspend fun markSynced(localId: Long, remoteId: Long)

    @Query("UPDATE daily_logs SET aiCardText = :text, syncStatus = 'local' WHERE id = :id")
    suspend fun updateAiCardTextAndMarkDirty(id: Long, text: String)
}