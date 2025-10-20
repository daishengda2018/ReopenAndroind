package com.dsd.baccarat.data

import com.dsd.baccarat.ui.page.MIN_TABLE_COLUMN_COUNT

// --- 数据类定义 (请确保与此一致) ---

// 输入类型
enum class InputType {
    B, P, BET_B, BET_P;

    override fun toString(): String = when (this) {
        InputType.B -> "B"
        InputType.P -> "P"
        InputType.BET_B -> "押庄"
        InputType.BET_P -> "押闲"
    }
}

// BP 计数器
data class BpCounter(val bCount: Int = 0, val pCount: Int = 0)

// 列类型
enum class ColumnType { A, B, C }

// 策略类型
enum class StrategyType { STRATEGY_12, STRATEGY_34, STRATEGY_56, STRATEGY_78 }

// 主列表项 (使用可空类型)
data class BppcItem(
    val dataA: Int? = null,
    val dataB: Int? = null,
    val dataC: Int? = null
)

// 策略列表项 (使用可空类型)
data class StrategyItem(
    val strategy1: Int? = null,
    val strategy2: Int? = null
)

// UI 显示项的基类
sealed class DisplayItem {
    object Empty : DisplayItem()
}

// Bppc 的显示项
sealed class BppcDisplayItem : DisplayItem() {
    object Empty : BppcDisplayItem()
    data class Real(val data: BppcItem) : BppcDisplayItem()
}

// 策略的显示项
sealed class StrategyDisplayItem : DisplayItem() {
    object Empty : StrategyDisplayItem()
    data class Real(val data: StrategyItem) : StrategyDisplayItem()
}

// 包含四种策略的数据
data class StrategyData(
    val strategy12: List<StrategyDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { StrategyDisplayItem.Empty },
    val strategy34: List<StrategyDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { StrategyDisplayItem.Empty },
    val strategy56: List<StrategyDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { StrategyDisplayItem.Empty },
    val strategy78: List<StrategyDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { StrategyDisplayItem.Empty }
)

data class PredictedStrategyValue(
    val predictionIndex: String? = null,
    val strategy12: String? = null,
    val strategy34: String? = null,
    val strategy56: String? = null,
    val strategy78: String? = null
)

enum class TimerStatus { Idle, Running, Paused, Finished }
