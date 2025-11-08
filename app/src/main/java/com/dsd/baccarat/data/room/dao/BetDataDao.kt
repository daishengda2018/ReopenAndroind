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

    /**
     * 查询最新的 66 条记录
     */
    @Query("""
            -- 先取最新的 66 条（按时间降序），再按时间升序排列（从老到新）
           SELECT * FROM (
               SELECT * FROM bet_data 
               ORDER BY curTime DESC  -- 最新的在前面
               LIMIT 65              -- 取最近的 65 条
           ) AS recent_data 
           ORDER BY curTime ASC      -- 最终按从老到新排列
    """)
    fun loadHistory(): List<BetEntity>

    // 查询指定时间内的所有笔记（按时间倒序排列）
    @Query("SELECT * FROM bet_data WHERE gameId = :gameId ORDER BY curTime ASC")
    fun getBetDataByGameId(gameId: String): List<BetEntity>

    @Query("DELETE FROM bet_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)

    @Query("SELECT * FROM bet_data")
    fun getAll(): List<BetEntity>
}