package com.dsd.baccarat.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 游戏场次实体类
 * @property gameId 游戏场次唯一标识
 * @property startTime 游戏开始时间
 * @property endTime 游戏结束时间
 * @property isActive 标记当前游戏是否活跃
 */
@Serializable
@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey
    val gameId: String = "", // 生成唯一ID
    @ColumnInfo(index = true)
    val startTime: Long = 0, // 游戏开始时间
    @ColumnInfo(index = true)
    var endTime: Long = 0, // 游戏结束时间
    @ColumnInfo(index = true)
    var isActive: Boolean = true, // 标记是否活跃
) {
    companion object {
        fun create() = GameSessionEntity(UUID.randomUUID().toString(), System.currentTimeMillis(), 0, true)
    }
}
