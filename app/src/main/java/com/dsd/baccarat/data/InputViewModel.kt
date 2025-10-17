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

    // 在 InputViewModel 类中添加新的 StateFlow 来存储 '12' 策略的结果
    private val mStrategy12StateFlow = MutableStateFlow<List<StrategeDisplayItem>>(
        List(MIN_TABLE_COLUMN_COUNT) { StrategeDisplayItem.Empty } // 初始总长度 = MIN_COUNT)
    )
    val strategy12StateFlow: StateFlow<List<StrategeDisplayItem>> =
        mStrategy12StateFlow.asStateFlow()

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
        calculateBppcStrategyData(last3Inputs)
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
        mBppcTableStateFlow.value = mBppcTableStateFlow.value
            .toMutableList() // 创建可变副本（触发 StateFlow 状态更新）
            .apply {
                // 1. 找到列表中最后一个 "Real" 实际项（跳过末尾的 Empty 占位项）
                val lastRealIndex = indexOfLast { it is BppcDisplayItem.Real }
                val lastRealItem = if (lastRealIndex != -1) this[lastRealIndex] as BppcDisplayItem.Real else null

                if (lastRealItem != null) {
                    // 2. 存在 Real 项，且其 dataA/dataB/dataC 有未填充的字段（值为 0）
                    val updatedRealItem = when {
                        lastRealItem.data.dataA == 0 -> lastRealItem.data.copy(dataA = result)
                        lastRealItem.data.dataB == 0 -> lastRealItem.data.copy(dataB = result)
                        lastRealItem.data.dataC == 0 -> lastRealItem.data.copy(dataC = result)
                        else -> null // 三个字段都已填充，需要新增 Real 项
                    }

                    if (updatedRealItem != null) {
                        // 2.1 更新现有 Real 项
                        this[lastRealIndex] = BppcDisplayItem.Real(updatedRealItem)
                    } else {
                        // 2.2 新增 Real 项（插入到最后一个 Real 项后面，占位项前面）
                        val insertIndex = lastRealIndex + 1
                        this.add(insertIndex, BppcDisplayItem.Real(BppcItem(dataA = result)))
                        // 确保列表总长度不超过 MIN_COUNT（若超过则删除末尾的 Empty 项）
                        if (size > MIN_TABLE_COLUMN_COUNT && last() is BppcDisplayItem.Empty) {
                            removeLastOrNull()
                        }
                    }
                } else {
                    // 3. 列表中没有 Real 项（全是 Empty），替换第一个 Empty 为 Real 项
                    this[0] = BppcDisplayItem.Real(BppcItem(dataA = result))
                }
            }
    }

    private fun calculateBppcStrategyData(last3Inputs: List<InputType>) {
        val value = when {
            // 对对
            (last3Inputs[0] == last3Inputs[1] && last3Inputs[1] == last3Inputs[2]) -> 1
            // 错错
            (last3Inputs[0] != last3Inputs[1] && last3Inputs[1] != last3Inputs[2]) -> 2
            // 对对
            (last3Inputs[0] == last3Inputs[1] && last3Inputs[1] != last3Inputs[2]) -> 3
            // 错对
            (last3Inputs[0] != last3Inputs[1] && last3Inputs[1] == last3Inputs[2]) -> 4
            else -> 0
        }

        if (value == 0) return

        mStrategy12StateFlow.value = mStrategy12StateFlow.value
            .toMutableList()
            .apply {
                val lastRealIndex = indexOfLast { it is StrategeDisplayItem.Real }
                val lastRealItem = if (lastRealIndex != -1) this[lastRealIndex] as StrategeDisplayItem.Real else null

                if (lastRealItem != null) {
                    // 存在 Real 项，且其 data1/data2 有未填充的
                    val updatedRealItem = when {
                        lastRealItem.data.data1 == 0 -> lastRealItem.data.copy(data1 = value)
                        lastRealItem.data.data2 == 0 -> lastRealItem.data.copy(data2 = value)
                        else -> null // 两个字段都已填充，需要新增 Real 项
                    }
                    if (updatedRealItem != null) {
                        // 更新现有 Real 项
                        this[lastRealIndex] = StrategeDisplayItem.Real(updatedRealItem)
                    } else {
                        // 新增 Real 项（插入到最后一个 Real 项后面，占位项前面）
                        val insertIndex = lastRealIndex + 1
                        this.add(insertIndex, StrategeDisplayItem.Real(StrategyItem(data1 = value)))
                        // 确保列表总长度不超过 MIN_COUNT（若超过则删除末尾的 Empty 项）
                        if (size > MIN_TABLE_COLUMN_COUNT && last() is StrategeDisplayItem.Empty) {
                            removeLastOrNull()
                        }
                    }
                } else {
                    // 列表中没有 Real 项（全是 Empty），替换第一个 Empty 为 Real 项
                    this[0] = StrategeDisplayItem.Real(StrategyItem(data1 = value))
                }
            }
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