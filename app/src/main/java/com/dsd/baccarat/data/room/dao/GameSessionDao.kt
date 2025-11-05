package com.dsd.baccarat.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 游戏场次DAO接口
 */
@Dao
interface GameSessionDao {
    // 插入新游戏场次
    @Insert
    suspend fun insert(session: GameSessionEntity)

    // 更新游戏场次（如结束时间）
    @Update
    suspend fun update(session: GameSessionEntity)

    // 查询当前活跃的游戏场次
    @Query("SELECT * FROM game_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): GameSessionEntity?

    // 查询所有历史游戏场次（按开始时间倒序）
    @Query("SELECT * FROM game_sessions WHERE isActive = 0 ORDER BY startTime DESC")
    fun getAllHistorySessions(): Flow<List<GameSessionEntity>>
    // 根据 gameId 删除游戏场次
    @Query("DELETE FROM game_sessions WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: String)

    @Query("SELECT * FROM game_sessions")
    fun getAll(): List<GameSessionEntity>
}
