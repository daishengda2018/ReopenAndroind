package com.dsd.baccarat.data

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InputViewModel : ViewModel() {
    private var mInputItemOfOpen: MutableList<InputType> = mutableListOf()

    // 私有可变 StateFlow（仅 ViewModel 内部可修改）
    private val mBppcTableMutableStateFlow = MutableStateFlow<List<BppcItem>>(emptyList())

    // 暴露给 UI 的不可变 StateFlow
    val bppcTableStateFlow: StateFlow<List<BppcItem>> = mBppcTableMutableStateFlow.asStateFlow()

    fun openB() {
        performBppcTableLogic(InputType.B)
    }

    fun openP() {
        performBppcTableLogic(InputType.P)
    }

    private fun performBppcTableLogic(inputType: InputType = InputType.NONE) {
        // 1. 添加输入类型并获取最近3次输入（提取逻辑以提升可读性）
        mInputItemOfOpen.add(inputType)
        val last3Inputs = mInputItemOfOpen.takeLast(3)
        if (last3Inputs.size < 3) {
            return
        }

        Log.d("InputViewModel", "Current Inputs: $last3Inputs")
        val inputCombination = last3Inputs.joinToString("")

        // 2. 通过常量映射表匹配结果（提升可维护性）
        val result = inputCombinationToResult[inputCombination] ?: 0

        // 3. 高效更新 BppcItem 列表
        mBppcTableMutableStateFlow.value = mBppcTableMutableStateFlow.value
            .toMutableList()
            .apply {
                if (isEmpty()) {
                    add(BppcItem())
                }
                val lastItem = last()
                when {
                    // 用 copy 创建新对象，替换旧对象
                    lastItem.dataA == 0 -> this[lastIndex] = lastItem.copy(dataA = result)
                    lastItem.dataB == 0 -> this[lastIndex] = lastItem.copy(dataB = result)
                    lastItem.dataC == 0 -> this[lastIndex] = lastItem.copy(dataC = result)
                    else -> add(BppcItem(dataA = result))
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