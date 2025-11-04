package com.dsd.baccarat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dsd.baccarat.data.DefaultViewModel.Companion.MIN_TABLE_COLUMN_COUNT
import kotlinx.serialization.Serializable

// 输入类型
@Serializable
enum class InputType(val value: String) { B("B"), P("P") }

@Serializable
enum class BetResultType(val value: String) { W("W"), L("L") }

enum class TimerStatus { Idle, Running, Paused, Finished }

// 2. 定义操作类型枚举（累加/累减）
enum class OperationType {
    INCREMENT, // 累加
    DECREMENT  // 累减
}

// 列类型
enum class ColumnType(val value: Int) { A(0), B(1), C(2) }

// 表名默认是类名，可通过 tableName
@Serializable
@Entity(tableName = "input_data")
data class InputData(
    @PrimaryKey
    @ColumnInfo(index = true) // 为curTime添加索引
    val curTime: Long = 0,
    val inputType: InputType,
) {
    // 次构造函数（不影响序列化，序列化器仅关注主构造函数属性）
    constructor(inputType: InputType) : this(System.currentTimeMillis(), inputType)

    companion object {
        fun createP() = InputData(InputType.P)
        fun createB() = InputData(InputType.B)
    }
}

@Serializable
@Entity(tableName = "bet_data")
data class BetData(
    @PrimaryKey
    @ColumnInfo(index = true) // 为curTime添加索引
    val curTime: Long = 0,
    val type: BetResultType,
) {
    // 次构造函数（不影响序列化，序列化器仅关注主构造函数属性）
    constructor(type: BetResultType) : this(System.currentTimeMillis(), type)

    companion object {
        fun createW() = BetData(BetResultType.W)
        fun createL() = BetData(BetResultType.L)
    }
}


// 策略类型
enum class StrategyType { STRATEGY_12, STRATEGY_34, STRATEGY_56, STRATEGY_78 }

// BP 计数器
data class Counter(val count1: Int = 0, val count2: Int = 0)

// 主列表项 (使用可空类型)
data class TableItem(
    val dataA: Pair<Boolean, Int?>? = null,
    val dataB: Pair<Boolean, Int?>? = null,
    val dataC: Pair<Boolean, Int?>? = null
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




