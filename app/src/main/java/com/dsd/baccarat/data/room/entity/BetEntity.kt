package com.dsd.baccarat.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dsd.baccarat.data.BetResultType
import kotlinx.serialization.Serializable

/**
 * Create by DaiShengda 2025/11/5 13:59
 */
@Serializable
@Entity(tableName = "bet_data")
data class BetEntity(
    @PrimaryKey
    @ColumnInfo(index = true) // 为curTime添加索引
    val curTime: Long = 0,
    val type: BetResultType,
) {
    // 次构造函数（不影响序列化，序列化器仅关注主构造函数属性）
    constructor(type: BetResultType) : this(System.currentTimeMillis(), type)

    companion object {
        fun createW() = BetEntity(BetResultType.W)
        fun createL() = BetEntity(BetResultType.L)
    }
}