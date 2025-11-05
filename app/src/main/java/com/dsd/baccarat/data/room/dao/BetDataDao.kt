package com.dsd.baccarat.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsd.baccarat.data.room.entity.BetEntity
import com.dsd.baccarat.data.room.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BetDataDao {
    // 插入单条数据（冲突策略：主键重复时替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(betEntity: BetEntity)

    // 插入多条数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(betList: List<BetEntity>)
    /**
     * 查询：今天的全部数据 + 历史数据（最多66条）
     * 结果按时间戳倒序排列（最新的在前面）
     */
    @Query(
        """
        -- 子查询1：今天的所有数据（无需括号）
        SELECT * FROM bet_data 
        WHERE date(curTime / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
        
        UNION ALL
        
        -- 子查询2：历史数据（最多66条，先按时间倒序取最近的，再限制数量）
        SELECT * FROM (
            SELECT * FROM bet_data 
            WHERE date(curTime / 1000, 'unixepoch', 'localtime') < date('now', 'localtime')
            ORDER BY curTime ASC
            LIMIT 66
        )
        
        -- 整体按时间戳倒序（最新的在前面）
        ORDER BY curTime ASC
    """
    )
    fun getTodayAndHistory(): Flow<List<BetEntity>>

    // 查询指定时间内的所有笔记（按时间倒序排列）
    @Query("SELECT * FROM bet_data WHERE gameId = :gameId ORDER BY curTime ASC")
    fun getBetDataByGameId(gameId: String): List<BetEntity>

    @Query("DELETE FROM bet_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)
}