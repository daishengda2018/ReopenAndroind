package com.dsd.baccarat.data

enum class ColumnType(val value: Int) {
    A(0),
    B(1),
    C(2);
}

enum class StategyType() {
    STRATEGY_12,
    STRATEGY_34,
    STRATEGY_56,
    STRATEGY_78;
}

data class BppcItem(
    val dataA: Int = 0,       // 数据A
    val dataB: Int = 0,       // 数据B
    val dataC: Int = 0        // 数据C
)

// 密封类：区分实际数据项和空占位项
sealed class BppcDisplayItem {
    data class Real(val data: BppcItem) : BppcDisplayItem() // 实际数据包装
    object Empty : BppcDisplayItem() // 空占位项
}

data class BpCounter(
    var bCount: Int = 0,       // B计数
    var pCount: Int = 0        // P计数
)

// 策略数据类，包含 ‘12’，‘34‘， ’56‘， ’78‘ 策略
data class StrategyData(
    val strategy12: List<StrategeDisplayItem> = emptyList(),  // 策略12
    val strategy34: List<StrategeDisplayItem> = emptyList(),  // 策略34
    val strategy56: List<StrategeDisplayItem> = emptyList(),  // 策略56
    val strategy78: List<StrategeDisplayItem> = emptyList()   // 策略78
) {
    // 表示当前策略数据是否全部为零
    fun isEmpty(): Boolean {
        return strategy12.all { it is StrategeDisplayItem.Empty } &&
                strategy34.all { it is StrategeDisplayItem.Empty } &&
                strategy56.all { it is StrategeDisplayItem.Empty } &&
                strategy78.all { it is StrategeDisplayItem.Empty }
    }

}

data class StrategyItem(
    val strategy1: Int = 0,       // 数据1
    val strategy2: Int = 0        // 数据2
)

// 密封类：区分实际数据项和空占位项
sealed class StrategeDisplayItem {
    data class Real(val data: StrategyItem) : StrategeDisplayItem() // 实际数据包装
    object Empty : StrategeDisplayItem() // 空占位项
}

enum class InputType {
    NONE,
    B,
    P,
    BET_B,
    BET_P,
}
