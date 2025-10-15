package com.dsd.baccarat.data

data class BppcItem(
    val dataA: Int = 0,       // 数据A
    val dataB: Int = 0,       // 数据B
    val dataC: Int = 0        // 数据C
)

data class BpCounter(
    var bCount: Int = 0,       // B计数
    var pCount: Int = 0        // P计数
)

// 密封类：区分实际数据项和空占位项
sealed class BppcDisplayItem {
    data class Real(val data: BppcItem) : BppcDisplayItem() // 实际数据包装
    object Empty : BppcDisplayItem() // 空占位项
}

enum class InputType {
    NONE,
    B,
    P,
    BET_B,
    BET_P,
}
