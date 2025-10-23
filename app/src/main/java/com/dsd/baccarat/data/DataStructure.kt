package com.dsd.baccarat.data

import com.dsd.baccarat.data.InputViewModel.Companion.MIN_TABLE_COLUMN_COUNT
// 输入类型
enum class InputType(val value: String) {
    B("B"),
    P("P")
}

enum class BeltResultType() {
    W,
    L
}

// BP 计数器
data class Counter(val count1: Int = 0, val count2: Int = 0)


// 列类型
enum class ColumnType(val value: Int) { A(0), B(1), C(2) }

// 策略类型
enum class StrategyType { STRATEGY_12, STRATEGY_34, STRATEGY_56, STRATEGY_78 }

// 主列表项 (使用可空类型)
data class TableItem(
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
sealed class TableDisplayItem() {
    object Empty : TableDisplayItem()
    data class Real(val data: TableItem) : TableDisplayItem()
}

// 策略的显示项
sealed class StrategyDisplayItem() {
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

enum class TimerStatus { Idle, Running }


