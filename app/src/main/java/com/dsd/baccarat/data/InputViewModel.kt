// kotlin
package com.dsd.baccarat.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.baccarat.ui.page.MIN_TABLE_COLUMN_COUNT
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

    private var _openList: MutableList<InputType> = mutableListOf()
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

    // 每列的动态预告 StateFlow（null 表示未知）
    private val _aPredictionStateFlow = MutableStateFlow(PredictedStrategyValue())
    val aPredictionStateFlow: StateFlow<PredictedStrategyValue> = _aPredictionStateFlow.asStateFlow()

    private val _bPredictionStateFlow = MutableStateFlow(PredictedStrategyValue())
    val bPredictionStateFlow: StateFlow<PredictedStrategyValue> = _bPredictionStateFlow.asStateFlow()

    private val _cPredictionStateFlow = MutableStateFlow(PredictedStrategyValue())
    val cPredictionStateFlow: StateFlow<PredictedStrategyValue> = _cPredictionStateFlow.asStateFlow()

    // Timer state moved to ViewModel
    private val _elapsedTime = MutableStateFlow(0) // 秒
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.Idle)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _showReminder = MutableStateFlow(false)
    val showReminder: StateFlow<Boolean> = _showReminder.asStateFlow()

    // 用于通知 UI 播放提示音（UI 层收集并调用 Android API）
    private val _soundEvent = MutableSharedFlow<Unit>()
    val soundEvent = _soundEvent.asSharedFlow()

    private var timerJob: Job? = null
    private val maxSeconds = 45 * 60

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
                _timerStatus.value = TimerStatus.Paused
                stopTimerJob()
            }
            TimerStatus.Paused -> {
                _timerStatus.value = TimerStatus.Running
                startTimer()
            }
            TimerStatus.Finished -> {
                _elapsedTime.value = 0
                _showReminder.value = false
                _timerStatus.value = TimerStatus.Running
                startTimer()
            }
        }
    }

    fun resetTimer() {
        stopTimerJob()
        _elapsedTime.value = 0
        _timerStatus.value = TimerStatus.Idle
        _showReminder.value = false
    }

    fun dismissReminder() {
        _showReminder.value = false
    }

    private fun startTimer() {
        stopTimerJob()
        timerJob = viewModelScope.launch {
            while (_timerStatus.value == TimerStatus.Running && _elapsedTime.value < maxSeconds) {
                delay(1000)
                // 使用原子 update，避免竞态
                _elapsedTime.update { it + 1 }
            }
            if (_elapsedTime.value >= maxSeconds) {
                _timerStatus.value = TimerStatus.Finished
                _showReminder.value = true
                _soundEvent.emit(Unit)
            }
        }
    }

    private fun stopTimerJob() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerJob()
    }

    fun openB() {
        _openList.add(InputType.B)
        updateOpenData()
    }

    fun openP() {
        _openList.add(InputType.P)
        updateOpenData()
    }

    fun removeLastOpen() {
        _openList.removeLastOrNull() ?: return

        // 重置展示相关状态
        _bppcTableStateFlow.value = DEFAULT_BPPCDISPLAY_LIST
        _bpCounterStateFlow.value = DEFAULT_BPCOUNTER
        _aStrategyStateFlow.value = DEFAULT_STRATEGYDATA
        _bStrategyStateFlow.value = DEFAULT_STRATEGYDATA
        _cStrategyStateFlow.value = DEFAULT_STRATEGYDATA
        _aPredictionStateFlow.value = DEFAULT_PREDICTION
        _bPredictionStateFlow.value = DEFAULT_PREDICTION
        _cPredictionStateFlow.value = DEFAULT_PREDICTION

        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        for (i in _openList.indices) {
            if (i >= 2) {
                val last3 = _openList.subList(0, i + 1).takeLast(3)
                updateBpCounter(_openList[i])
                val filledColumn = updateBppcTable(last3)
                updateStrategyData(last3, filledColumn)
            }
        }

        if (_openList.isNotEmpty()) {
            updateAllPredictions()
        }
    }

    private fun updateOpenData() {
        updateAllPredictions()

        val last3Inputs = _openList.takeLast(3)
        if (last3Inputs.size >= 3) {
            Log.d("InputViewModel", "Current Inputs: $last3Inputs")
            updateBpCounter(_openList.last())
            val filledColumn = updateBppcTable(last3Inputs)
            updateStrategyData(last3Inputs, filledColumn)
        }
    }

    /**
     * 优化：避免重复计算，逻辑更清晰
     */
    private fun updateAllPredictions() {
        if (_openList.isEmpty()) return

        _aPredictionStateFlow.value = DEFAULT_PREDICTION
        _bPredictionStateFlow.value = DEFAULT_PREDICTION
        _cPredictionStateFlow.value = DEFAULT_PREDICTION

        val lastIndex = _openList.lastIndex
        when (lastIndex % 3) {
            0 -> {
                if (_openList.size > 3) _cPredictionStateFlow.value = predictNextStrategyValue("3", _openList)
                _aPredictionStateFlow.value = predictNextStrategyValue("2", _openList)
            }
            1 -> {
                _aPredictionStateFlow.value = predictNextStrategyValue("3", _openList)
                _bPredictionStateFlow.value = predictNextStrategyValue("2", _openList)
            }
            2 -> {
                _bPredictionStateFlow.value = predictNextStrategyValue("3", _openList)
                _cPredictionStateFlow.value = predictNextStrategyValue("2", _openList)
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

        val strategy12 = lastInput.toString()
        val strategy56 = flip(lastInput).toString()
        val strategy34 = (if (isLastIndexEven) lastInput else flip(lastInput)).toString()
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

    private fun updateBppcTable(last3Inputs: List<InputType>): ColumnType? {
        val inputCombination = last3Inputs.joinToString("")
        val result = inputCombinationToResult[inputCombination] ?: return null

        var filledColumn: ColumnType? = null

        _bppcTableStateFlow.update { currentList ->
            val updatedList = currentList.toMutableList()
            val lastRealIndex = updatedList.indexOfLast { it is BppcDisplayItem.Real }

            if (lastRealIndex == -1) {
                updatedList[0] = BppcDisplayItem.Real(BppcItem(dataA = result))
                filledColumn = ColumnType.A
            } else {
                val lastRealItem = updatedList[lastRealIndex] as BppcDisplayItem.Real
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
                        updatedList.add(insertIndex, BppcDisplayItem.Real(BppcItem(dataA = result)))
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
