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
    @PrimaryKey()
    val curTime: Long = 0,
    @ColumnInfo(index = true)
    val gameId: String,
    @ColumnInfo(index = true)
    val type: BetResultType,
) {
    companion object {
        fun createW(gameId: String) = BetEntity(curTime = System.currentTimeMillis(), gameId = gameId, type = BetResultType.W)
        fun createL(gameId: String) = BetEntity(curTime = System.currentTimeMillis(), gameId = gameId, type = BetResultType.L)
    }
}