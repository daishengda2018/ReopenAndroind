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

    // 6. 根据 gameId 查询所有记录
    @Query("SELECT * FROM input_data WHERE gameId = :gameId ORDER BY curTime ASC")
    suspend fun getInputsByGameId(gameId: String): List<InputEntity>

    @Query("SELECT * FROM input_data")
    fun getAll(): List<InputEntity>
}
