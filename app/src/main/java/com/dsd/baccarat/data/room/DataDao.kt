package com.dsd.baccarat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Create by Shengda 2025/11/4$ 13:58$
 */
@Dao
interface InputDataDao {
    // 1. 插入单条数据（冲突策略：若主键curTime重复则替换）
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(inputData: InputData)

    // 2. 插入多条数据
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(inputs: List<InputData>)

    // 3. 查询所有数据（按时间戳倒序，最新的在前）
    @Query("SELECT * FROM input_data ORDER BY curTime DESC")
    fun getAllInputs(): Flow<List<InputData>>  // 返回Flow，数据变化时自动通知

    // 5. 根据时间戳删除数据
    @Query("DELETE FROM input_data WHERE curTime = :curTime")
    suspend fun deleteByTime(curTime: Long)
}