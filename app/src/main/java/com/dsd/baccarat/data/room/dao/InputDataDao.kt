package com.dsd.baccarat.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import com.dsd.baccarat.data.room.entity.InputEntity


/**
 * Create by Shengda 2025/11/4$ 13:58$
 */
@Dao
interface InputDataDao {
    // 1. 插入单条数据（冲突策略：若主键curTime重复则替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inputEntity: InputEntity)

    // 2. 插入多条数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inputs: List<InputEntity>)

    /**
     * 查询所有数据的时间戳（用于提取日期）
     */
    @Query("SELECT curTime FROM input_data")
    suspend fun getAllCurTimes(): List<Long>

    /**
     * 查询指定日期范围内的 InputData（当天00:00:00 至 23:59:59）
     * @param startTime 当天开始时间戳（毫秒）
     * @param endTime 当天结束时间戳（毫秒）
     */
    @Query(
        """
        SELECT * FROM input_data 
        WHERE curTime BETWEEN :startTime AND :endTime 
        ORDER BY curTime ASC
    """
    )
    suspend fun queryByDateRange(
        startTime: Long,
        endTime: Long
    ): List<InputEntity>

    // 6. 根据 gameId 查询所有记录
    @Query("SELECT * FROM input_data WHERE gameId = :gameId ORDER BY curTime ASC")
    suspend fun getInputsByGameId(gameId: String): List<InputEntity>

    // 5. 根据时间戳删除数据
    @Query("DELETE FROM input_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)

    @Query("SELECT * FROM input_data")
    fun getAll(): List<InputEntity>
}
