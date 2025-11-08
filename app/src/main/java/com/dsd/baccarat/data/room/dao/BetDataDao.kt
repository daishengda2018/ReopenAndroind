package com.dsd.baccarat.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsd.baccarat.data.room.entity.BetEntity

@Dao
interface BetDataDao {
    // 插入单条数据（冲突策略：主键重复时替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(betEntity: BetEntity)

    // 插入多条数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(betList: List<BetEntity>)

    @Query("SELECT * FROM bet_data WHERE gameId = :gameId ORDER BY curTime ASC")
    fun loadDataWithGameId(gameId: String) : List<BetEntity>

    @Query("""
            -- 先取最新的 66 条（按时间降序），再按时间升序排列（从老到新）
           SELECT * FROM (
               SELECT * FROM bet_data 
               ORDER BY curTime DESC  -- 最新的在前面
               LIMIT :limit              -- 取最近的 65 条
           ) AS recent_data 
           ORDER BY curTime ASC      -- 最终按从老到新排列
    """)
    fun loadHistory(limit: Int = 65): List<BetEntity>

    /**
     * 查询 gameID 对应的数据
     */
    @Query("SELECT * FROM bet_data WHERE gameId = :gameId ORDER BY curTime ASC")
    fun getBetDataByGameId(gameId: String): List<BetEntity>

    /**
     * 1. 先根据 gameId 查询所有数据，获取其最小时间（MIN(curTime)）
     * 2. 以该最小时间为基准，查询时间早于它的 65 条历史记录（按时间从新到旧排序，取最近的65条）
     */
    @Query("""
        SELECT * FROM bet_data 
        -- 条件：时间早于 startTime
        WHERE curTime < :startTime
        -- 按时间从新到旧排序（最近的历史记录在前）
        ORDER BY curTime DESC 
        -- 限制取 65 条
        LIMIT 65
    """)
    suspend fun queryHistoryBefore(
        startTime: Long
    ): List<BetEntity>

    @Query("DELETE FROM bet_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)

    @Query("SELECT * FROM bet_data")
    fun getAll(): List<BetEntity>
}