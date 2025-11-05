package com.dsd.baccarat.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dsd.baccarat.data.room.entity.NoteEntity

/**
 * Create by Shengda 2025/11/5 13:57
 */
/**
 * DAO for [NoteEntity] operations.
 * Create by Shengda 2025/11/5
 */
@Dao
interface NoteDataDao {
    // 插入一条笔记
    @Insert
    suspend fun insert(note: NoteEntity)

    // 查询指定时间内的所有笔记（按时间倒序排列）
    @Query("SELECT * FROM note WHERE gameId = :gameId ORDER BY curTime ASC")
    fun getNoteByGameId(gameId: String): List<NoteEntity>
}