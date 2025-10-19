package com.dsd.baccarat.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.dsd.baccarat.ui.page.MIN_TABLE_COLUMN_COUNT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InputViewModel : ViewModel() {

    private var _opendList: MutableList<InputType> = mutableListOf()
    private var _betList: MutableList<InputType> = mutableListOf()

    private val _bppcTableStateFlow = MutableStateFlow<List<BppcDisplayItem>>(DEFAULT_BPPCDISPLAY_LIST)
    val bppcTableStateFlow: StateFlow<List<BppcDisplayItem>> = _bppcTableStateFlow.asStateFlow()

    private val _bpCounterStateFlow = MutableStateFlow(BpCounter())
    val bppcCounterStateFlow: StateFlow<BpCounter> = _bpCounterStateFlow.asStateFlow()

    private val _aStrategyStateFlow = MutableStateFlow(StrategyData())
    val aStrategyStateFlow: StateFlow<StrategyData> = _aStrategyStateFlow.asStateFlow()

    private val _bStrategyStateFlow = MutableStateFlow(StrategyData())
    val bStrategyStateFlow: StateFlow<StrategyData> = _bStrategyStateFlow.asStateFlow()

    private val _cStrategyStateFlow = MutableStateFlow(StrategyData())
    val cStrategyStateFlow: StateFlow<StrategyData> = _cStrategyStateFlow.asStateFlow()

    // 新增：每列的动态预告 StateFlow（null 表示未知）
    private val _aPredictionStateFlow = MutableStateFlow(PredictedStrategyValue())
    val aPredictionStateFlow: StateFlow<PredictedStrategyValue> = _aPredictionStateFlow.asStateFlow()

    private val _bPredictionStateFlow = MutableStateFlow(PredictedStrategyValue())
    val bPredictionStateFlow: StateFlow<PredictedStrategyValue> = _bPredictionStateFlow.asStateFlow()

    private val _cPredictionStateFlow = MutableStateFlow(PredictedStrategyValue())
    val cPredictionStateFlow: StateFlow<PredictedStrategyValue> = _cPredictionStateFlow.asStateFlow()

    fun openB() {
        _opendList.add(InputType.B)
        updateOpenData()
    }

    fun openP() {
        _opendList.add(InputType.P)
        updateOpenData()
    }

    // Kotlin
    fun removeLasOpen() {
        _opendList.removeLastOrNull() ?: return

        // 重置展示相关状态： 数据量不大，不考虑性能，否则代码将会很复杂
        _bppcTableStateFlow.value = DEFAULT_BPPCDISPLAY_LIST
        _bpCounterStateFlow.value = DEFAULT_BPCOUNTER
        _aStrategyStateFlow.value = DEFAULT_STRATEGYDATA
        _bStrategyStateFlow.value = DEFAULT_STRATEGYDATA
        _cStrategyStateFlow.value = DEFAULT_STRATEGYDATA
        _aPredictionStateFlow.value = DEFAULT_PREDICTION
        _bPredictionStateFlow.value = DEFAULT_PREDICTION
        _cPredictionStateFlow.value = DEFAULT_PREDICTION

        // 在删除最近一项后，按原来“加入时”的逻辑从头按顺序重建表格、计数器和策略
        // 但只有在索引到达第 3 个及以后时才触发表格/策略的更新（避免不足三项时错误计算）。
        for (i in _opendList.indices) {
            if (i >= 2) {
                // 取最后三项输入
                val last3 = _opendList.subList(0, i + 1).takeLast(3)
                // 与添加时一致：在第三项及之后才更新计数、表格与策略
                updateBpCounter(_opendList[i])
                val filledColumn = updateBppcTable(last3)
                updateStrategyData(last3, filledColumn)
            }
        }

        // 重建完成后更新预测（若不足 1 项则已置为默认）
        if (_opendList.isNotEmpty()) {
            updateAllPredictions()
        }
    }


    private fun updateOpenData() {
        updateAllPredictions()

        val last3Inputs = _opendList.takeLast(3)
        if (last3Inputs.size >= 3) {
            Log.d("InputViewModel", "Current Inputs: $last3Inputs")
            updateBpCounter(_opendList.last())
            val filledColumn = updateBppcTable(last3Inputs)
            updateStrategyData(last3Inputs, filledColumn)
        }
    }

    /**
     * [核心优化] 结合了您的配置列表和我之前的简洁计算逻辑。
     */
    private fun updateAllPredictions() {
        if (_opendList.isEmpty()) return // 空列表直接返回，避免索引越界

        _aPredictionStateFlow.value = DEFAULT_PREDICTION
        _bPredictionStateFlow.value = DEFAULT_PREDICTION
        _cPredictionStateFlow.value = DEFAULT_PREDICTION

        val lastIndex = _opendList.lastIndex
        if (lastIndex % 3 == 0) {
            if (_opendList.size > 3) {
                _cPredictionStateFlow.value = predictNextStrategyValue("3", _opendList)
            }
            _aPredictionStateFlow.value = predictNextStrategyValue("2", _opendList)

        } else if (lastIndex % 3 == 1) {
            _aPredictionStateFlow.value = predictNextStrategyValue("3", _opendList)
            _bPredictionStateFlow.value = predictNextStrategyValue("2", _opendList)
        } else if (lastIndex % 3 == 2) {
            _bPredictionStateFlow.value = predictNextStrategyValue("3", _opendList)
            _cPredictionStateFlow.value = predictNextStrategyValue("2", _opendList)
        }
    }

    /**
     * 根据策略值预测下一次输入的函数，便于后续实现和维护。
     */
    private fun predictNextStrategyValue(title: String, inputHistory: MutableList<InputType>): PredictedStrategyValue {
        val lastInput = inputHistory.last()
        // 判断最后一个输入的索引是否为偶数
        val isLastIndexEven = inputHistory.lastIndex % 2 == 0
        fun flip(input: InputType) = if (input == InputType.B) InputType.P else InputType.B

        // 第2位与第1位相同，第3位与第2位相同；
        val strategy12 = lastInput.toString()

        // 第2位与第1位相反，第3位与第2位相反；
        val strategy56 = flip(lastInput).toString()

        // 第2位与第1位相反，第3位与第2位相同；
        val strategy34 = (if (isLastIndexEven) lastInput else flip(lastInput)).toString()

        // 第2位与第1位相同，第3位与第2位相反；
        val strategy78 = (if (isLastIndexEven) flip(lastInput) else lastInput).toString()

        return PredictedStrategyValue(title, strategy12, strategy34, strategy56, strategy78)
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
     * 专门负责更新 BppcTable 的函数，逻辑清晰。
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
     * 将策略更新分发到正确的 StateFlow。
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
     * 专门负责更新单个策略列表的函数，逻辑与 updateBppcTable 类似但更简单。
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
        _betList.add(InputType.BET_B)
    }

    fun betP() {
        _betList.add(InputType.BET_P)
    }

    fun removeLastBet() {
        _betList.removeLastOrNull()
    }

    companion object {
        private val DEFAULT_PREDICTION = PredictedStrategyValue()
        private val DEFAULT_STRATEGYDATA = StrategyData()
        private val DEFAULT_BPCOUNTER = BpCounter()
        private val DEFAULT_BPPCDISPLAY_LIST = List(MIN_TABLE_COLUMN_COUNT) { BppcDisplayItem.Empty }
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