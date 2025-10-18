package com.dsd.baccarat.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.dsd.baccarat.ui.page.MIN_TABLE_COLUMN_COUNT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InputViewModel : ViewModel() {
    private var mInputItemOfOpen: MutableList<InputType> = mutableListOf()

    // 私有可变 StateFlow（仅 ViewModel 内部可修改）
    private val mBppcTableStateFlow = MutableStateFlow<List<BppcDisplayItem>>(
        List(MIN_TABLE_COLUMN_COUNT) { BppcDisplayItem.Empty } // 初始总长度 = MIN_COUNT
    )

    // 暴露给 UI 的不可变 StateFlow
    val bppcTableStateFlow: StateFlow<List<BppcDisplayItem>> = mBppcTableStateFlow.asStateFlow()

    private val mBpCounterStateFlow = MutableStateFlow(BpCounter(0, 0))
    val bppcCounterStateFlow: StateFlow<BpCounter> = mBpCounterStateFlow.asStateFlow()


    /* 三路策略*/
    private val mAStrategyStateFlow = MutableStateFlow(StrategyData())
    val aStrategyStateFlow: StateFlow<StrategyData> = mAStrategyStateFlow.asStateFlow()

    private val mBStrategyStateFlow = MutableStateFlow(StrategyData())
    val bStrategyStateFlow: StateFlow<StrategyData> = mBStrategyStateFlow.asStateFlow()

    private val mCStrategyStateFlow = MutableStateFlow(StrategyData())
    val cStrategyStateFlow: StateFlow<StrategyData> = mCStrategyStateFlow.asStateFlow()

    fun openB() {
        performBppcTableLogic(InputType.B)
    }

    fun openP() {
        performBppcTableLogic(InputType.P)
    }

    private fun performBppcTableLogic(inputType: InputType) {
        mInputItemOfOpen.add(inputType)
        val last3Inputs = mInputItemOfOpen.takeLast(3)
        if (last3Inputs.size != 3) {
            return
        }

        calculateBppcCounter(last3Inputs)
        val filledColumn = calculateBppcTableData(last3Inputs)
        calculateStrategyData(last3Inputs, filledColumn)
    }

    private fun calculateBppcCounter(last3Inputs: List<InputType>) {
        val current = mBpCounterStateFlow.value
        val newValue = when (last3Inputs.last()) {
            InputType.B -> current.copy(bCount = current.bCount + 1)
            InputType.P -> current.copy(pCount = current.pCount + 1)
            else -> current
        }
        mBpCounterStateFlow.value = newValue
    }

    /**
     * 返回 ColumnType：A/B/C，或 null 表示未写入
     */
    private fun calculateBppcTableData(last3Inputs: List<InputType>): ColumnType? {
        Log.d("InputViewModel", "Current Inputs: $last3Inputs")
        val inputCombination = last3Inputs.joinToString("")

        // 2. 通过常量映射表匹配结果（提升可维护性）
        val result = inputCombinationToResult[inputCombination] ?: return null

        // 3. 高效更新 BppcItem 列表
        val updatedList = mBppcTableStateFlow.value.toMutableList()
        // 3.1. 找到列表中最后一个 "Real" 实际项（跳过末尾的 Empty 占位项）
        val lastRealIndex = updatedList.indexOfLast { it is BppcDisplayItem.Real }
        val lastRealItem = if (lastRealIndex != -1) updatedList[lastRealIndex] as BppcDisplayItem.Real else null

        if (lastRealItem == null) {
            // 3.2. 列表全空，替换第一个 Empty 为 Real 并写入 dataA
            if (updatedList.isNotEmpty()) {
                updatedList[0] = BppcDisplayItem.Real(BppcItem(dataA = result))
            } else {
                updatedList.add(BppcDisplayItem.Real(BppcItem(dataA = result)))
            }

            mBppcTableStateFlow.value = updatedList
            return ColumnType.A
        } else {
            // 3.3. 存在 Real 项，且其 dataA/dataB/dataC 有未填充的字段（值为 0）
            val (newItem, filledColumn) = when {
                lastRealItem.data.dataA == 0 -> Pair(lastRealItem.data.copy(dataA = result), ColumnType.A)
                lastRealItem.data.dataB == 0 -> Pair(lastRealItem.data.copy(dataB = result), ColumnType.B)
                lastRealItem.data.dataC == 0 -> Pair(lastRealItem.data.copy(dataC = result), ColumnType.C)
                else -> null
            } ?: run {
                // 三字段已满，新增一列并写入 dataA
                val insertIndex = lastRealIndex + 1
                updatedList.add(insertIndex, BppcDisplayItem.Real(BppcItem(dataA = result)))
                // 确保列表总长度不超过 MIN_COUNT（若超过则删除末尾的 Empty 项）
                if (updatedList.size > MIN_TABLE_COLUMN_COUNT && updatedList.last() is BppcDisplayItem.Empty) {
                    updatedList.removeLastOrNull()
                }
                mBppcTableStateFlow.value = updatedList
                return ColumnType.A
            }

            // 更新现有 Real 项并返回填充列
            updatedList[lastRealIndex] = BppcDisplayItem.Real(newItem)
            mBppcTableStateFlow.value = updatedList
            return filledColumn
        }
    }

    // 仅更新被写入的那一路策略 flow（filledColumn: A/B/C/null）
    private fun calculateStrategyData(last3Inputs: List<InputType>, filledColumn: ColumnType?) {
        if (filledColumn == null) return

        when (filledColumn) {
            ColumnType.A -> updateStrategyFlow(mAStrategyStateFlow, last3Inputs)
            ColumnType.B -> updateStrategyFlow(mBStrategyStateFlow, last3Inputs)
            ColumnType.C -> updateStrategyFlow(mCStrategyStateFlow, last3Inputs)
        }
    }

    private fun updateStrategyFlow(
        flow: MutableStateFlow<StrategyData>,
        last3Inputs: List<InputType>
    ) {
        val current = flow.value

        // 将每一路策略列表通过通用函数追加或更新
        val updated = StrategyData(
            strategy12 = calculateStrategyList(StategyType.STRATEGY_12, current.strategy12, last3Inputs),
            strategy34 = calculateStrategyList(StategyType.STRATEGY_34, current.strategy34, last3Inputs),
            strategy56 = calculateStrategyList(StategyType.STRATEGY_56, current.strategy56, last3Inputs),
            strategy78 = calculateStrategyList(StategyType.STRATEGY_78, current.strategy78, last3Inputs)
        )

        flow.value = updated
    }

    /**
     * 通用追加/更新策略列的函数
     *
     * 说明：该函数对传入的 list 进行安全处理（list 可能最初为空），
     * 并按规则尝试写入 strategy1 或 strategy2，若当前列已满则在其后插入新列。
     */
    private fun calculateStrategyList(
        type: StategyType,
        list: List<StrategeDisplayItem>,
        last3Inputs: List<InputType>
    ): List<StrategeDisplayItem> {
        val updated = list.toMutableList()
        val lastRealIndex = updated.indexOfLast { it is StrategeDisplayItem.Real }
        val lastRealItem = if (lastRealIndex != -1) updated[lastRealIndex] as StrategeDisplayItem.Real else null
        val strategyValue = computeStrategyValue(type, last3Inputs)

        if (lastRealItem == null) {
            // 列表全空，替换第一个 Empty 为 Real 并写入 strategy1
            if (updated.isNotEmpty()) {
                updated[0] = StrategeDisplayItem.Real(StrategyItem(strategy1 = strategyValue))
            } else {
                updated.add(StrategeDisplayItem.Real(StrategyItem(strategy1 = strategyValue)))
            }
        } else {
            // 存在 Real 项，尝试写入 strategy1 或 strategy2
            val existing = lastRealItem.data
            val replaced = when {
                existing.strategy1 == 0 -> existing.copy(strategy1 = strategyValue)
                existing.strategy2 == 0 -> existing.copy(strategy2 = strategyValue)
                else -> null
            }
            if (replaced != null) {
                // 更新现有 Real 项
                updated[lastRealIndex] = StrategeDisplayItem.Real(replaced)
            } else {
                // 两个策略字段均已填满，新增一列并写入 strategy1
                val insertIndex = lastRealIndex + 1
                updated.add(insertIndex, StrategeDisplayItem.Real(StrategyItem(strategy1 = strategyValue)))
                if (updated.size > MIN_TABLE_COLUMN_COUNT && updated.last() is StrategeDisplayItem.Empty) {
                    updated.removeLastOrNull()
                }
            }
        }
        return updated
    }

    private fun computeStrategyValue(type: StategyType, last3Inputs: List<InputType>): Int {
        // 计算相等关系
        val eq01 = last3Inputs[0] == last3Inputs[1]
        val eq12 = last3Inputs[1] == last3Inputs[2]
        val pair = Pair(eq01, eq12)

        // 根据策略类型和相等关系映射到数值
        val value = when (type) {
            StategyType.STRATEGY_12 -> when (pair) {
                Pair(true, true) -> 1
                Pair(false, false) -> 2
                Pair(true, false) -> 3
                else -> 4
            }

            StategyType.STRATEGY_34 -> when (pair) {
                Pair(false, true) -> 1
                Pair(true, true) -> 2
                Pair(false, false) -> 3
                else -> 4
            }

            StategyType.STRATEGY_56 -> when (pair) {
                Pair(false, false) -> 1
                Pair(true, true) -> 2
                Pair(false, true) -> 3
                else -> 4
            }

            StategyType.STRATEGY_78 -> when (pair) {
                Pair(true, false) -> 1
                Pair(false, true) -> 2
                Pair(true, true) -> 3
                else -> 4
            }
        }
        return value
    }

    fun betB() {
        mInputItemOfOpen.add(InputType.BET_B)
    }

    fun betP() {
        mInputItemOfOpen.add(InputType.BET_P)
    }

    companion object {
        private val inputCombinationToResult = mapOf(
            "BBB" to 1,
            "PPP" to 2,
            "BPP" to 3,
            "PBB" to 4,
            "PBP" to 5,
            "BPB" to 6,
            "PPB" to 7,
            "BBP" to 8
        )
    }
}
