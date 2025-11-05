package com.dsd.baccarat.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dsd.baccarat.data.room.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

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

    // 根据 curTime 查询一条笔记
    @Query("SELECT * FROM note WHERE curTime = :curTime")
    suspend fun getNoteByTime(curTime: Long): NoteEntity?

    // 查询指定时间内的所有笔记（按时间倒序排列）
    @Query("SELECT * FROM note WHERE curTime BETWEEN :startOfDay AND :endOfDay ORDER BY curTime DESC")
    fun getNotesBy(startOfDay: Long, endOfDay: Long): Flow<List<NoteEntity>>
}