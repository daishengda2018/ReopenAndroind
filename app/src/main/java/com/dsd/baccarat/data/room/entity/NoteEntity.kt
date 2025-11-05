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
    val content: String,
) {
    // 次构造函数（不影响序列化，序列化器仅关注主构造函数属性）
    constructor(content: String) : this(System.currentTimeMillis(), content)
}
