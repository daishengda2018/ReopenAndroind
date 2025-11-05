package com.dsd.baccarat.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Create by DaiShengda 2025/11/5 14:00
 */
@Serializable
@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(index = true) // 为curTime添加索引
    val curTime: Long = 0,
    @ColumnInfo(index = true)
    val gameId: String,
    @ColumnInfo(index = true)
    val content: String,

) {
    companion object {
        fun create(gameId: String, content: String) = NoteEntity(curTime = System.currentTimeMillis(), gameId = gameId, content = content)
    }
}
