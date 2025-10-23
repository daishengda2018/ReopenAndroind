// kotlin
package com.dsd.baccarat.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InputViewModel : ViewModel() {

    private var _openInputList: MutableList<InputType> = mutableListOf()

    private var _betResultList: MutableList<BeltResultType> = mutableListOf()

    private val _wlTableStateFlow = MutableStateFlow<List<TableDisplayItem>>(DEFAULT_TABLE_DISPLAY_LIST)
    val wlTableStateFlow: StateFlow<List<TableDisplayItem>> = _wlTableStateFlow.asStateFlow()

    private val _wlCounterStateFlow = MutableStateFlow(Counter())
    val wlCounterStateFlow: StateFlow<Counter> = _wlCounterStateFlow.asStateFlow()

    private val _bppcTableStateFlow = MutableStateFlow<List<TableDisplayItem>>(DEFAULT_TABLE_DISPLAY_LIST)
    val bppcTableStateFlow: StateFlow<List<TableDisplayItem>> = _bppcTableStateFlow.asStateFlow()

    private val _bppcCounterStateFlow = MutableStateFlow(Counter())
    val bppcCounterStateFlow: StateFlow<Counter> = _bppcCounterStateFlow.asStateFlow()

    private val _strategyStateFlowList = List(MAX_COLUMN) { MutableStateFlow(StrategyData()) }
    val strategyStateFlowList: List<StateFlow<StrategyData>> = _strategyStateFlowList.map { it.asStateFlow() }

    // 每列的动态预告 StateFlow（null 表示未知）
    private val _predictionStateFlowList = List(MAX_COLUMN) { MutableStateFlow(PredictedStrategyValue()) }
    val predictedStateFlowList: List<StateFlow<PredictedStrategyValue>> = _predictionStateFlowList.map { it.asStateFlow() }

    val beltInputStageFlow: MutableStateFlow<InputType?> = MutableStateFlow(null)

    // Timer state moved to ViewModel
    private val _timerStatus = MutableStateFlow(TimerStatus.Idle)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0) // 秒
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    private val _showReminder = MutableStateFlow(false)
    val showReminder: StateFlow<Boolean> = _showReminder.asStateFlow()

    // 用于通知 UI 播放提示音（UI 层收集并调用 Android API）
    private val _soundEvent = MutableSharedFlow<Unit>()
    val soundEvent = _soundEvent.asSharedFlow()

    private var timerJob: Job? = null

    // 控制计时器：切换/复位/关闭提醒
    fun toggleTimer() {
        when (_timerStatus.value) {
            TimerStatus.Idle -> {
                _elapsedTime.value = 0
                _showReminder.value = false
                _timerStatus.value = TimerStatus.Running
                startTimer()
            }
            TimerStatus.Running -> {
                _elapsedTime.value = 0
                _showReminder.value = false
                _timerStatus.value = TimerStatus.Idle
                stopTimerJob()
            }
        }
    }

    fun dismissReminder() {
        _showReminder.value = false
    }

    private fun startTimer() {
        stopTimerJob()
        timerJob = viewModelScope.launch {
            while (_timerStatus.value == TimerStatus.Running && _elapsedTime.value < MAX_SECONDS) {
                delay(1000)
                // 使用原子 update，避免竞态
                _elapsedTime.update { it + 1 }
            }
            if (_elapsedTime.value >= MAX_SECONDS) {
                _timerStatus.value = TimerStatus.Idle
                _showReminder.value = true
                _soundEvent.emit(Unit)
            }
        }
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    fun openB() {
        _openInputList.add(InputType.B)
        updateOpenData()
    }

    fun openP() {
        _openInputList.add(InputType.P)
        updateOpenData()
    }
    private fun updateOpenData() {
        // 所有预测，
        updateAllPredictions()
        // BPPC 表格和策略
        updateBppcAndStrategy()
        //  WL 表格
        updateWlTable()
    }

    private fun updateBppcAndStrategy() {
        val last3Inputs = _openInputList.takeLast(3)
        if (last3Inputs.size >= 3) {
            updateBppcCounter(_openInputList.last())
            val filledColumn = updateBppcTable(last3Inputs)
            updateStrategyData(last3Inputs, filledColumn)
        }
    }

    private fun updateWlTable() {
        val inputType = beltInputStageFlow.value
        if (inputType == null) {
            return
        }
        if (_openInputList.last() == inputType) {
            _betResultList.add(BeltResultType.W)
        } else {
            _betResultList.add(BeltResultType.L)
        }

        val last3Inputs = _betResultList.takeLast(3)
        if (last3Inputs.size < 3) {
            beltInputStageFlow.update { null }
            return
        }

        val inputCombination = last3Inputs.joinToString("")
        val result = wlCombinationToResult[inputCombination] ?: run {
            beltInputStageFlow.update { null }
            return
        }

        Log.d("InputViewModel", "Current Inputs: $last3Inputs")
        updateWlCounter(_betResultList.last())
        updateTableStageFlow(_wlTableStateFlow, result)
        beltInputStageFlow.update { null }
    }

    /**
     * 优化：避免重复计算，逻辑更清晰
     */
    private fun updateAllPredictions() {
        if (_openInputList.isEmpty()) return

        _predictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }
        val lastIndex = _openInputList.lastIndex

        when (lastIndex % 3) {
            ColumnType.A.value -> {
                if (_openInputList.size > 3) _predictionStateFlowList[ColumnType.C.value].value = predictNextStrategyValue("3", _openInputList)
                _predictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("2", _openInputList)
            }
            ColumnType.B.value -> {
                _predictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("3", _openInputList)
                _predictionStateFlowList[ColumnType.B.value].value = predictNextStrategyValue("2", _openInputList)
            }
            ColumnType.C.value -> {
                _predictionStateFlowList[ColumnType.B.value].value = predictNextStrategyValue("3", _openInputList)
                _predictionStateFlowList[ColumnType.C.value].value = predictNextStrategyValue("2", _openInputList)
            }
        }
    }

    /**
     * 将预测逻辑做少量简化，便于阅读
     */
    private fun predictNextStrategyValue(title: String, inputHistory: MutableList<InputType>): PredictedStrategyValue {
        val lastInput = inputHistory.last()
        val isLastIndexEven = inputHistory.lastIndex % 2 == 0
        fun flip(input: InputType) = if (input == InputType.B) InputType.P else InputType.B

        val strategy12 = lastInput.value
        val strategy56 = flip(lastInput).value
        val strategy34 = (if (isLastIndexEven) lastInput else flip(lastInput)).value
        val strategy78 = (if (isLastIndexEven) flip(lastInput) else lastInput).value

        return PredictedStrategyValue(title, strategy12, strategy34, strategy56, strategy78)
    }

    private fun updateBppcCounter(lastInput: InputType) {
        _bppcCounterStateFlow.update { currentCounter ->
            when (lastInput) {
                InputType.B -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                InputType.P -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    private fun updateWlCounter(lastInput: BeltResultType) {
        _wlCounterStateFlow.update { currentCounter ->
            when (lastInput) {
                BeltResultType.W -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                BeltResultType.L -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    private fun updateBppcTable(last3Inputs: List<InputType>): ColumnType? {
        val inputCombination = last3Inputs.joinToString("")
        val result = bppcCombinationToResult[inputCombination] ?: return null
        return updateTableStageFlow(_bppcTableStateFlow, result)
    }

    private fun updateTableStageFlow(tableStateFlow: MutableStateFlow<List<TableDisplayItem>>, result: Int): ColumnType? {
        var filledColumn: ColumnType? = null

        tableStateFlow.update { currentList ->
            val updatedList = currentList.toMutableList()
            val lastRealIndex = updatedList.indexOfLast { it is TableDisplayItem.Real }

            if (lastRealIndex == -1) {
                updatedList[0] = TableDisplayItem.Real(TableItem(dataA = result))
                filledColumn = ColumnType.A
            } else {
                val lastRealItem = updatedList[lastRealIndex] as TableDisplayItem.Real
                val currentData = lastRealItem.data

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
                        val insertIndex = lastRealIndex + 1
                        updatedList.add(insertIndex, TableDisplayItem.Real(TableItem(dataA = result)))
                        filledColumn = ColumnType.A
                        if (updatedList.size > MIN_TABLE_COLUMN_COUNT) updatedList.removeLastOrNull()
                    }
                }
            }
            updatedList
        }
        return filledColumn
    }

    private fun updateStrategyData(last3Inputs: List<InputType>, filledColumn: ColumnType?) {
        filledColumn ?: return
        _strategyStateFlowList[filledColumn.value].update { currentStrategyData ->
            currentStrategyData.copy(
                strategy12 = updateSingleStrategyList(StrategyType.STRATEGY_12, currentStrategyData.strategy12, last3Inputs),
                strategy34 = updateSingleStrategyList(StrategyType.STRATEGY_34, currentStrategyData.strategy34, last3Inputs),
                strategy56 = updateSingleStrategyList(StrategyType.STRATEGY_56, currentStrategyData.strategy56, last3Inputs),
                strategy78 = updateSingleStrategyList(StrategyType.STRATEGY_78, currentStrategyData.strategy78, last3Inputs)
            )
        }
    }

    private fun updateSingleStrategyList(
        type: StrategyType,
        currentList: List<StrategyDisplayItem>,
        last3Inputs: List<InputType>
    ): List<StrategyDisplayItem> {
        val newValue = computeStrategyValue(type, last3Inputs)
        val updatedList = currentList.toMutableList()
        val lastRealIndex = updatedList.indexOfLast { it is StrategyDisplayItem.Real }

        if (lastRealIndex == -1) {
            updatedList[0] = StrategyDisplayItem.Real(StrategyItem(strategy1 = newValue))
        } else {
            val lastRealItem = updatedList[lastRealIndex] as StrategyDisplayItem.Real
            val currentData = lastRealItem.data

            when {
                currentData.strategy1 == null -> {
                    updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(strategy1 = newValue))
                }
                currentData.strategy2 == null -> {
                    updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(strategy2 = newValue))
                }
                else -> {
                    val insertIndex = lastRealIndex + 1
                    updatedList.add(insertIndex, StrategyDisplayItem.Real(StrategyItem(strategy1 = newValue)))
                    if (updatedList.size > MIN_TABLE_COLUMN_COUNT) updatedList.removeLastOrNull()
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
    fun removeLastOpen() {
        _openInputList.removeLastOrNull() ?: return

        // 重置展示相关状态
        _bppcTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        _bppcCounterStateFlow.value = DEFAULT_BPCOUNTER
        _strategyStateFlowList.forEach { it.value = DEFAULT_STRATEGYDATA }
        _predictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }

        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        for (i in _openInputList.indices) {
            if (i >= 2) {
                val last3 = _openInputList.subList(0, i + 1).takeLast(3)
                updateBppcCounter(_openInputList[i])
                val filledColumn = updateBppcTable(last3)
                updateStrategyData(last3, filledColumn)
            }
        }

        if (_openInputList.isNotEmpty()) {
            updateAllPredictions()
        }
    }

    fun betB() {
        beltInputStageFlow.update { InputType.B }
    }

    fun betP() {
        beltInputStageFlow.update { InputType.P }
    }

    fun removeLastBet() {
        if (beltInputStageFlow.value != null) {
            beltInputStageFlow.update { null }
        } else {
            _betResultList.removeLastOrNull() ?: return
        }

        // 重置展示相关状态
        _wlTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        _wlCounterStateFlow.value = DEFAULT_BPCOUNTER

        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        for (i in _betResultList.indices) {
            if (i >= 2) {
                val last3Inputs = _betResultList.subList(0, i + 1).takeLast(3)
                updateWlCounter(_betResultList[i])
                val inputCombination = last3Inputs.joinToString("")
                wlCombinationToResult[inputCombination]?.let { result ->
                    updateTableStageFlow(_wlTableStateFlow, result)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerJob()
    }

    companion object {

        val DEFAULT_PREDICTION = PredictedStrategyValue()
        val DEFAULT_STRATEGYDATA = StrategyData()
        val DEFAULT_BPCOUNTER = Counter()
        val DEFAULT_TABLE_DISPLAY_LIST = List(MIN_TABLE_COLUMN_COUNT) { TableDisplayItem.Empty }
        val DEFAULT_STRAGETY_DISPLAY_LIST = List(MIN_TABLE_COLUMN_COUNT) { StrategyDisplayItem.Empty }

        const val MIN_TABLE_COLUMN_COUNT = 30
        private const val MAX_SECONDS = 45 * 60
        private const val MAX_COLUMN = 3
        private val bppcCombinationToResult = mapOf(
            "BBB" to 1, "PPP" to 2, "BPP" to 3, "PBB" to 4,
            "PBP" to 5, "BPB" to 6, "PPB" to 7, "BBP" to 8
        )

        private val wlCombinationToResult = mapOf(
            "WWW" to 1, "LLL" to 2, "WLL" to 3, "LWW" to 4,
            "LWL" to 5, "WLW" to 6, "LLW" to 7, "WWL" to 8
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
