package com.dsd.baccarat.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// 表格列数常量
private const val MIN_TABLE_COLUMN_COUNT = 30

// 输入类型
@Serializable
@Immutable
enum class InputType(val value: String) {
    B("B"), P("P");

    fun inverted(): InputType = if (this == B) InputType.P else InputType.B
}


@Serializable
@Immutable
enum class BetResultType(val value: String) { W("W"), L("L") }

@Immutable
enum class TimerStatus { Idle, Running, Paused, Finished }

// 2. 定义操作类型枚举（累加/累减）
@Immutable
enum class OperationType {
    INCREMENT, // 累加
    DECREMENT  // 累减
}

// 列类型
@Immutable
enum class ColumnType(val value: Int) { A(0), B(1), C(2) }


// BP 计数器
@Immutable
data class Counter(val count1: Int = 0, val count2: Int = 0)

// 主列表项 (使用可空类型)
@Immutable
data class TableItem(
    val dataA: Pair<Boolean, Int?>? = null,
    val dataB: Pair<Boolean, Int?>? = null,
    val dataC: Pair<Boolean, Int?>? = null
)

@Immutable
data class StrategyGridInfo(
    val predictedList: List<String?> = emptyList(),
    val actualOpenedList: List<String?> = emptyList(),
    val itemList: List<StrategyGridItem> = emptyList()
)

@Immutable
data class StrategyGridItem(
    val isObsolete: Boolean = false,
    val title: String? = null,
    val items: List<String?> = emptyList()
)

// 策略列表项 (使用可空类型)
@Immutable
data class Strategy3WyasItem(
    val first: Int? = null,
    val second: Int? = null
)

// UI 显示项的基类
@Immutable
sealed class DisplayItem {
    @Immutable object Empty : DisplayItem()
}

// Bppc 的显示项
@Immutable
sealed class TableDisplayItem() {
    @Immutable object Empty : TableDisplayItem()
    @Immutable data class Real(val data: TableItem) : TableDisplayItem()
}

// 策略的显示项
@Immutable
sealed class Strategy3WyasDisplayItem() {
    @Immutable object Empty : Strategy3WyasDisplayItem()
    @Immutable data class Real(val data: Strategy3WyasItem) : Strategy3WyasDisplayItem()
}

// 包含四种策略的数据
@Immutable
data class Strategy3WaysData(
    val strategy12: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty },
    val strategy34: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty },
    val strategy56: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty },
    val strategy78: List<Strategy3WyasDisplayItem> = List(MIN_TABLE_COLUMN_COUNT) { Strategy3WyasDisplayItem.Empty }
)

@Immutable
data class PredictedStrategy3WaysValue(
    val titleStr: String? = null,
    val strategy12: String? = null,
    val strategy34: String? = null,
    val strategy56: String? = null,
    val strategy78: String? = null
)




