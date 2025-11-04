package com.dsd.baccarat.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsd.baccarat.data.BetData
import com.dsd.baccarat.data.InputData
import kotlinx.coroutines.flow.Flow

/**
 * Create by Shengda 2025/11/4$ 13:58$
 */
@Dao
interface InputDataDao {
    // 1. 插入单条数据（冲突策略：若主键curTime重复则替换）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inputData: InputData)

    // 2. 插入多条数据
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inputs: List<InputData>)

    // 3. 查询所有数据（按时间戳倒序，最新的在前）
    @Query("SELECT * FROM input_data ORDER BY curTime DESC")
    fun loadAllInputs(): Flow<List<InputData>>  // 返回Flow，数据变化时自动通知

//    @Query("SELECT * FROM input_data  WHERE curTime = :curTime ORDER BY curTime")
//    suspend fun loadByTime(curTime: Long): Flow<List<InputData>>

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
    ): List<InputData>

    // 5. 根据时间戳删除数据
    @Query("DELETE FROM input_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)
}

@Dao
interface BetDataDao {
    // 插入单条数据（冲突策略：主键重复时替换）
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(betData: BetData)

    // 插入多条数据
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(betList: List<BetData>)

    /**
     * 查询指定日期范围内的 BetData
     */
    @Query(
        """
        SELECT * FROM bet_data 
        WHERE curTime BETWEEN :startTime AND :endTime 
        ORDER BY curTime ASC
    """
    )
    suspend fun queryByDateRange(
        startTime: Long,
        endTime: Long
    ): List<BetData>

    /**
     * 查询：今天的全部数据 + 历史数据（最多66条）
     * 结果按时间戳倒序排列（最新的在前面）
     */
    @Query("""
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
    """)
    fun getTodayAndHistory(): Flow<List<BetData>>

    @Query("DELETE FROM bet_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)
}