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
    // 在 InputViewModel 类中添加新的 StateFlow 来存储策略的结果
    private val mAStrategyStateFlow = MutableStateFlow<List<StrategeDisplayItem>>(
        List(MIN_TABLE_COLUMN_COUNT) { StrategeDisplayItem.Empty } // 初始总长度 = MIN_COUNT)
    )
    val aStrategyStateFlow: StateFlow<List<StrategeDisplayItem>> = mAStrategyStateFlow.asStateFlow()

    private val mBStrategyStateFlow = MutableStateFlow<List<StrategeDisplayItem>>(
        List(MIN_TABLE_COLUMN_COUNT) { StrategeDisplayItem.Empty } // 初始总长度 = MIN_COUNT)
    )
    val bStrategyStateFlow: StateFlow<List<StrategeDisplayItem>> = mAStrategyStateFlow.asStateFlow()

    private val mCStrategyStateFlow = MutableStateFlow<List<StrategeDisplayItem>>(
        List(MIN_TABLE_COLUMN_COUNT) { StrategeDisplayItem.Empty } // 初始总长度 = MIN_COUNT)
    )
    val cStrategyStateFlow: StateFlow<List<StrategeDisplayItem>> = mAStrategyStateFlow.asStateFlow()

    fun openB() {
        performBppcTableLogic(InputType.B)
    }

    fun openP() {
        performBppcTableLogic(InputType.P)
    }

    private fun performBppcTableLogic(inputType: InputType) {
        // 1. 添加输入类型并获取最近3次输入（提取逻辑以提升可读性）
        mInputItemOfOpen.add(inputType)
        val last3Inputs = mInputItemOfOpen.takeLast(3)
        if (last3Inputs.size != 3) {
            return
        }

        calculateBppcCounter(last3Inputs)
        calculateBppcTableData(last3Inputs)
        calculateStrategyData(last3Inputs)
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

    private fun calculateBppcTableData(last3Inputs: List<InputType>) {
        Log.d("InputViewModel", "Current Inputs: $last3Inputs")
        val inputCombination = last3Inputs.joinToString("")

        // 2. 通过常量映射表匹配结果（提升可维护性）
        val result = inputCombinationToResult[inputCombination] ?: return

        // 3. 高效更新 BppcItem 列表
        val updatedList = mBppcTableStateFlow.value.toMutableList()
        // 3.1. 找到列表中最后一个 "Real" 实际项（跳过末尾的 Empty 占位项）
        val lastRealIndex = updatedList.indexOfLast { it is BppcDisplayItem.Real }
        val lastRealItem = if (lastRealIndex != -1) updatedList[lastRealIndex] as BppcDisplayItem.Real else null

        if (lastRealItem == null) {
            // 3.2. 列表中没有 Real 项（全是 Empty），替换第一个 Empty 为 Real 项
            updatedList[0] = BppcDisplayItem.Real(BppcItem(dataA = result))

        } else {
            // 3.3. 存在 Real 项，且其 dataA/dataB/dataC 有未填充的字段（值为 0）
            val updatedRealItem = when {
                lastRealItem.data.dataA == 0 -> lastRealItem.data.copy(dataA = result)
                lastRealItem.data.dataB == 0 -> lastRealItem.data.copy(dataB = result)
                lastRealItem.data.dataC == 0 -> lastRealItem.data.copy(dataC = result)
                else -> null // 三个字段都已填充，需要新增 Real 项
            }

            if (updatedRealItem != null) {
                // 3.3.1 更新现有 Real 项
                updatedList[lastRealIndex] = BppcDisplayItem.Real(updatedRealItem)
            } else {
                // 3.3.2 新增 Real 项（插入到最后一个 Real 项后面，占位项前面）
                val insertIndex = lastRealIndex + 1
                updatedList.add(insertIndex, BppcDisplayItem.Real(BppcItem(dataA = result)))
                // 确保列表总长度不超过 MIN_COUNT（若超过则删除末尾的 Empty 项）
                if (updatedList.size > MIN_TABLE_COLUMN_COUNT && updatedList.last() is BppcDisplayItem.Empty) {
                    updatedList.removeLastOrNull()
                }
            }
        }

        mBppcTableStateFlow.value = updatedList
    }

    private fun calculateStrategyData(last3Inputs: List<InputType>) {
        if (last3Inputs.size < 3) return

        val strategyData = computeStrategyData(last3Inputs)

        val updateItems = mAStrategyStateFlow.value.toMutableList()
        val lastRealIndex = updateItems.indexOfLast { it is StrategeDisplayItem.Real }
        val lastRealItem = (if (lastRealIndex != -1) updateItems[lastRealIndex] as StrategeDisplayItem.Real else null)

        if (lastRealItem == null) {
            // 全是 Empty，直接替换第一个占位
            updateItems[0] = StrategeDisplayItem.Real(StrategyItem(strategyData))
        } else {
            val lastRealItem = (updateItems[lastRealIndex] as StrategeDisplayItem.Real)
            val updated = when {
                lastRealItem.data.strategy1.isEmpty() -> lastRealItem.data.copy(strategy1 = strategyData)
                lastRealItem.data.strategy2.isEmpty() -> lastRealItem.data.copy(strategy2 = strategyData)
                else -> null
            }

            if (updated != null) {
                // 更新最后一个 Real 项的空位
                updateItems[lastRealIndex] = StrategeDisplayItem.Real(updated)
            } else {
                // 最后一个 Real 已满，插入新 Real 项
                val insertIndex = lastRealIndex + 1
                updateItems.add(insertIndex, StrategeDisplayItem.Real(StrategyItem(strategyData)))
                // 保持列表长度与原逻辑一致：若超长并且末尾是占位则移除末尾
                if (updateItems.size > MIN_TABLE_COLUMN_COUNT && updateItems.last() is StrategeDisplayItem.Empty) {
                    updateItems.removeLastOrNull()
                }
            }
        }

        mAStrategyStateFlow.value = updateItems;
    }


    private fun computeStrategyData(last3Inputs: List<InputType>): StrategyData {
        val eq01 = last3Inputs[0] == last3Inputs[1]
        val eq12 = last3Inputs[1] == last3Inputs[2]
        val pair = Pair(eq01, eq12)

        // 对对=1/错错=2，对错=3/错对=4；

        // 押注方向12：第2位与第1位相同，第3位与第2位相同；
        val strategy12 = when (pair) {
            Pair(true, true) -> 1
            Pair(false, false) -> 2
            Pair(true, false) -> 3
            else -> 4
        }

        // 押注方向56：第2位与第1位相反，第3位与第2位相反；
        val strategy56 = when (pair) {
            Pair(false, false) -> 1
            Pair(true, true) -> 2
            Pair(false, true) -> 3
            else -> 4
        }

        //  押注方向34：第2位与第1位相反，第3位与第2位相同；
        val strategy34 = when (pair) {
            Pair(false, true) -> 1
            Pair(true, true) -> 2
            Pair(false, false) -> 3
            else -> 4
        }

        // 押注方向78：第2位与第1位相同，第3位与第2位相反；
        val strategy78 = when (pair) {
            Pair(true, false) -> 1
            Pair(false, true) -> 2
            Pair(true, true) -> 3
            else -> 4
        }

        return StrategyData(
            strategy12 = strategy12,
            strategy34 = strategy34,
            strategy56 = strategy56,
            strategy78 = strategy78
        )
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