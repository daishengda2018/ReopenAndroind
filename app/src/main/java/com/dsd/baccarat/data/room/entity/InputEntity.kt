package com.dsd.baccarat.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dsd.baccarat.data.InputType
import kotlinx.serialization.Serializable

/**
 * 游戏输入数据实体类
 * @property gameId 关联的游戏场次ID
 */
@Serializable
@Entity(tableName = "input_data")
data class InputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(index = true)
    val gameId: String, // 关联的 gameId
    val inputType: InputType,
    val curTime: Long = System.currentTimeMillis(),
) {
    companion object {
        fun createP(gameId: String) = InputEntity(gameId = gameId, inputType = InputType.P)
        fun createB(gameId: String) = InputEntity(gameId = gameId, inputType = InputType.B)
    }
}