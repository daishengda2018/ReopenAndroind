package com.dsd.baccarat.data

import androidx.lifecycle.ViewModel
import com.dsd.baccarat.ui.page.MIN_TABLE_COLUMN_COUNT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InputViewModel : ViewModel() {

    private var inputHistory: MutableList<InputType> = mutableListOf()

    private val _bppcTableStateFlow = MutableStateFlow<List<BppcDisplayItem>>(List(MIN_TABLE_COLUMN_COUNT) { BppcDisplayItem.Empty }
    )
    val bppcTableStateFlow: StateFlow<List<BppcDisplayItem>> = _bppcTableStateFlow.asStateFlow()

    private val _bpCounterStateFlow = MutableStateFlow(BpCounter())
    val bppcCounterStateFlow: StateFlow<BpCounter> = _bpCounterStateFlow.asStateFlow()

    private val _aStrategyStateFlow = MutableStateFlow(StrategyData())
    val aStrategyStateFlow: StateFlow<StrategyData> = _aStrategyStateFlow.asStateFlow()

    private val _bStrategyStateFlow = MutableStateFlow(StrategyData())
    val bStrategyStateFlow: StateFlow<StrategyData> = _bStrategyStateFlow.asStateFlow()

    private val _cStrategyStateFlow = MutableStateFlow(StrategyData())
    val cStrategyStateFlow: StateFlow<StrategyData> = _cStrategyStateFlow.asStateFlow()

    fun openB() {
        performBppcTableLogic(InputType.B)
    }

    fun openP() {
        performBppcTableLogic(InputType.P)
    }

    private fun performBppcTableLogic(inputType: InputType) {
        inputHistory.add(inputType)
        val last3Inputs = inputHistory.takeLast(3)
        if (last3Inputs.size < 3) return

        updateBpCounter(last3Inputs.last())
        val filledColumn = updateBppcTable(last3Inputs)
        updateStrategyData(last3Inputs, filledColumn)
    }

    private fun updateBpCounter(lastInput: InputType) {
        _bpCounterStateFlow.update { currentCounter ->
            when (lastInput) {
                InputType.B -> currentCounter.copy(bCount = currentCounter.bCount + 1)
                InputType.P -> currentCounter.copy(pCount = currentCounter.pCount + 1)
                else -> currentCounter
            }
        }
    }

    /**
     * [优化点] 专门负责更新 BppcTable 的函数，逻辑清晰。
     * 使用 .update 来保证原子性，内部逻辑简洁。
     */
    private fun updateBppcTable(last3Inputs: List<InputType>): ColumnType? {
        val inputCombination = last3Inputs.joinToString("")
        val result = inputCombinationToResult[inputCombination] ?: return null

        var filledColumn: ColumnType? = null

        _bppcTableStateFlow.update { currentList ->
            val updatedList = currentList.toMutableList()
            val lastRealIndex = updatedList.indexOfLast { it is BppcDisplayItem.Real }

            if (lastRealIndex == -1) {
                // 情况1：列表为空，在第一位创建新项
                updatedList[0] = BppcDisplayItem.Real(BppcItem(dataA = result))
                filledColumn = ColumnType.A
            } else {
                val lastRealItem = updatedList[lastRealIndex] as BppcDisplayItem.Real
                val currentData = lastRealItem.data

                // 情况2：最后一个 Real 项有空位，更新它
                when {
                    currentData.dataA == null -> {
                        updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataA = result))
                        filledColumn = ColumnType.A
                    }

                    currentData.dataB == null -> {
                        updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataB = result))
                        filledColumn = ColumnType.B
                    }

                    currentData.dataC == null -> {
                        updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataC = result))
                        filledColumn = ColumnType.C
                    }

                    else -> {
                        // 情况3：最后一个 Real 项已满，在其后添加新项
                        val insertIndex = lastRealIndex + 1
                        updatedList.add(insertIndex, BppcDisplayItem.Real(BppcItem(dataA = result)))
                        filledColumn = ColumnType.A

                        // 保持列表长度
                        if (updatedList.size > MIN_TABLE_COLUMN_COUNT) {
                            updatedList.removeLastOrNull()
                        }
                    }
                }
            }
            updatedList
        }
        return filledColumn
    }

    /**
     * [优化点] 将策略更新分发到正确的 StateFlow。
     */
    private fun updateStrategyData(last3Inputs: List<InputType>, filledColumn: ColumnType?) {
        val targetFlow = when (filledColumn) {
            ColumnType.A -> _aStrategyStateFlow
            ColumnType.B -> _bStrategyStateFlow
            ColumnType.C -> _cStrategyStateFlow
            null -> return
        }

        targetFlow.update { currentStrategyData ->
            currentStrategyData.copy(
                strategy12 = updateSingleStrategyList(StrategyType.STRATEGY_12, currentStrategyData.strategy12, last3Inputs),
                strategy34 = updateSingleStrategyList(StrategyType.STRATEGY_34, currentStrategyData.strategy34, last3Inputs),
                strategy56 = updateSingleStrategyList(StrategyType.STRATEGY_56, currentStrategyData.strategy56, last3Inputs),
                strategy78 = updateSingleStrategyList(StrategyType.STRATEGY_78, currentStrategyData.strategy78, last3Inputs)
            )
        }
    }

    /**
     * [优化点] 专门负责更新单个策略列表的函数，逻辑与 updateBppcTable 类似但更简单。
     */
    private fun updateSingleStrategyList(
        type: StrategyType,
        currentList: List<StrategyDisplayItem>,
        last3Inputs: List<InputType>
    ): List<StrategyDisplayItem> {
        val newValue = computeStrategyValue(type, last3Inputs)
        val updatedList = currentList.toMutableList()
        val lastRealIndex = updatedList.indexOfLast { it is StrategyDisplayItem.Real }

        if (lastRealIndex == -1) {
            // 情况1：列表为空
            updatedList[0] = StrategyDisplayItem.Real(StrategyItem(strategy1 = newValue))
        } else {
            val lastRealItem = updatedList[lastRealIndex] as StrategyDisplayItem.Real
            val currentData = lastRealItem.data

            // 情况2：最后一个 Real 项有空位
            when {
                currentData.strategy1 == null -> {
                    updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(strategy1 = newValue))
                }

                currentData.strategy2 == null -> {
                    updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(strategy2 = newValue))
                }

                else -> {
                    // 情况3：最后一个 Real 项已满
                    val insertIndex = lastRealIndex + 1
                    updatedList.add(insertIndex, StrategyDisplayItem.Real(StrategyItem(strategy1 = newValue)))
                    if (updatedList.size > MIN_TABLE_COLUMN_COUNT) {
                        updatedList.removeLastOrNull()
                    }
                }
            }
        }
        return updatedList
    }

    private fun computeStrategyValue(type: StrategyType, last3Inputs: List<InputType>): Int {
        val eq01 = last3Inputs[0] == last3Inputs[1]
        val eq12 = last3Inputs[1] == last3Inputs[2]

        return when (type) {
            StrategyType.STRATEGY_12 -> strategy12Map[Pair(eq01, eq12)] ?: 4
            StrategyType.STRATEGY_34 -> strategy34Map[Pair(eq01, eq12)] ?: 4
            StrategyType.STRATEGY_56 -> strategy56Map[Pair(eq01, eq12)] ?: 4
            StrategyType.STRATEGY_78 -> strategy78Map[Pair(eq01, eq12)] ?: 4
        }
    }

    fun betB() {
        inputHistory.add(InputType.BET_B)
    }

    fun betP() {
        inputHistory.add(InputType.BET_P)
    }

    companion object {
        private val inputCombinationToResult = mapOf(
            "BBB" to 1, "PPP" to 2, "BPP" to 3, "PBB" to 4,
            "PBP" to 5, "BPB" to 6, "PPB" to 7, "BBP" to 8
        )

        private val strategy12Map = mapOf(
            Pair(true, true) to 1, Pair(false, false) to 2, Pair(true, false) to 3
        )
        private val strategy34Map = mapOf(
            Pair(false, true) to 1, Pair(true, true) to 2, Pair(false, false) to 3
        )
        private val strategy56Map = mapOf(
            Pair(false, false) to 1, Pair(true, true) to 2, Pair(false, true) to 3
        )
        private val strategy78Map = mapOf(
            Pair(true, false) to 1, Pair(false, true) to 2, Pair(true, true) to 3
        )
    }
}