package com.dsd.baccarat.data

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dsd.baccarat.data.InputViewModel.Companion.MIN_TABLE_COLUMN_COUNT

// 输入类型
enum class InputType(val value: String) { B("B"), P("P") }

enum class BetResultType(val value: String) { W("W"), L("L") }

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

data class StrategyGridInfo(
    val predictedList: List<String?> = emptyList(),
    val actualOpenedList: List<String?> = emptyList(),
    val itemList: List<StrategyGridItem> = emptyList()
)

data class StrategyGridItem(
    val status: Boolean = false,
    val title: String? = null,
    val items: List<String?> = emptyList()
)

// 策略列表项 (使用可空类型)
data class Strategy3WyasItem(
    val first: Int? = null,
    val second: Int? = null
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
sealed class Strategy3WyasDisplayItem() {
    object Empty : Strategy3WyasDisplayItem()
    data class Real(val data: Strategy3WyasItem) : Strategy3WyasDisplayItem()
}

// 包含四种策略的数据
data class Strategy3WaysData(
    val strategy12: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty },
    val strategy34: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty },
    val strategy56: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty },
    val strategy78: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty }
)

data class PredictedStrategy3WaysValue(
    val predictionIndex: String? = null,
    val strategy12: String? = null,
    val strategy34: String? = null,
    val strategy56: String? = null,
    val strategy78: String? = null
)

enum class TimerStatus { Idle, Running }

object CountKeys {
    val W_COUNT = intPreferencesKey("w_count")
    val L_COUNT = intPreferencesKey("l_count")

    val NOTE_TEXT = stringPreferencesKey("note_text")

    val INPUT_TYPE_LIST = stringPreferencesKey("opend_list")
    val BET_TYPE_LIST = stringPreferencesKey("bet_list")
}

// 2. 定义操作类型枚举（累加/累减）
enum class OperationType {
    INCREMENT, // 累加
    DECREMENT  // 累减
}


