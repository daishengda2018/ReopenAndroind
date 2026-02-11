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

// Table type for rendering differentiation
@Immutable
enum class TableType { BP, WL }

// Circle mark type for different marking scenarios
@Immutable
enum class CircleMarkType {
    ZF,         // Adjacent opposite patterns
    ZF_SEP,     // Skip-one opposite patterns
    CIRCLE_12,  // BP pattern betting table specific
    CIRCLE_34,
    CIRCLE_56,
    CIRCLE_78,
    WL_ALARM    // W/L table blue circle for numbers 2 and 7
}

// Circle type for overlay rendering
@Immutable
enum class CircleType { RED, BLUE, BOTH }

// W/L symbol for sync display
@Immutable
enum class WLSymbol { WIN, LOSS }


// BP 计数器
@Immutable
data class Counter(val count1: Int = 0, val count2: Int = 0)

// Display marks for V2 features
@Immutable
data class DisplayMarks(
    val circleA: CircleType? = null,
    val circleB: CircleType? = null,
    val circleC: CircleType? = null,
    val wlSymbolA: WLSymbol? = null,
    val wlSymbolB: WLSymbol? = null,
    val wlSymbolC: WLSymbol? = null,
    val isLightBackground: Boolean = false
)

// 主列表项 (使用可空类型)
@Immutable
data class TableItem(
    val dataA: Pair<Boolean, Int?>? = null,
    val dataB: Pair<Boolean, Int?>? = null,
    val dataC: Pair<Boolean, Int?>? = null,
    // V2: Display marks for circles and symbols
    val displayMarks: DisplayMarks? = null
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




