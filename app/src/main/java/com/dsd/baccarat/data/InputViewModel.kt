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

/**
 * 输入 ViewModel
 */
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

    private val _strategy3WaysStateFlowList = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_STRATEGY_3WAYS) }
    val strategy3WaysStateFlowList: List<StateFlow<Strategy3WaysData>> = _strategy3WaysStateFlowList.map { it.asStateFlow() }

    private val _stragetyGridStateFlow: List<MutableStateFlow<StrategyGridInfo>> = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_STRANTYGE_GRID) }
    val stragetyGridStateFlow: List<StateFlow<StrategyGridInfo>> = _stragetyGridStateFlow.map { it.asStateFlow() }

    // 每列的动态预告 StateFlow（null 表示未知）
    private val _predictionStateFlowList = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_PREDICTED_3WAYS) }
    val predictedStateFlowList: List<StateFlow<PredictedStrategy3WaysValue>> = _predictionStateFlowList.map { it.asStateFlow() }

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

    // 用于存储去重后的key（自动去重）
    private val _uniqueBppcConbinationList = MutableList<MutableSet<String>>(MAX_COLUMN_COUNT, { mutableSetOf() })
    private val _strategyGridStateMap: MutableMap<ColumnType, Boolean?> = HashMap(MAX_COLUMN_COUNT)

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
        val lastInput = _openInputList.last()
        val last3Inputs = _openInputList.takeLast(MIX_CONBINATION_ITEM_COUNT)
        updateBppcAndStrantegy(lastInput, last3Inputs)
        //  WL 表格
        updateWlTable()
    }

    private fun updateBppcAndStrantegy(lastInput: InputType, last3Inputs: List<InputType>) {
        if (_openInputList.isNotEmpty()) {
            updateBppcCounter(lastInput)
        }
        if (last3Inputs.size < MIX_CONBINATION_ITEM_COUNT) {
            return
        }

        updateBppcTable(last3Inputs)?.let { filledColumn ->
            update3WayStrategy(last3Inputs, filledColumn)
            updateGridStrategy(last3Inputs, filledColumn)
        }
    }

    /**
     * 更新 BPPC 计数器
     */
    private fun updateBppcCounter(lastInput: InputType) {
        _bppcCounterStateFlow.update { currentCounter ->
            when (lastInput) {
                InputType.B -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                InputType.P -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    /**
     * 更新 BPPC 表格
     */
    private fun updateBppcTable(last3Inputs: List<InputType>): ColumnType? {
        val inputCombination = last3Inputs.joinToString("")
        val result = bppcCombinationToResult[inputCombination] ?: return null
        val columnType = updateTableStageFlow(_bppcTableStateFlow, result)
        return columnType
    }

    private fun updateAllPredictions() {
        // 如果没有输入，则不更新预测
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
    private fun predictNextStrategyValue(title: String, inputHistory: MutableList<InputType>): PredictedStrategy3WaysValue {
        val lastInput = inputHistory.last()
        val isLastIndexEven = inputHistory.lastIndex % 2 == 0
        fun flip(input: InputType) = if (input == InputType.B) InputType.P else InputType.B

        val strategy12 = lastInput.value
        val strategy56 = flip(lastInput).value
        val strategy34 = (if (isLastIndexEven) lastInput else flip(lastInput)).value
        val strategy78 = (if (isLastIndexEven) flip(lastInput) else lastInput).value

        return PredictedStrategy3WaysValue(title, strategy12, strategy34, strategy56, strategy78)
    }

    /**
     * 更新 WL 表格
     */
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


    private fun updateWlCounter(lastInput: BeltResultType) {
        _wlCounterStateFlow.update { currentCounter ->
            when (lastInput) {
                BeltResultType.W -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                BeltResultType.L -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    /**
     * 更新 3 种策略
     */
    private fun update3WayStrategy(last3Inputs: List<InputType>, filledColumn: ColumnType) {
        _strategy3WaysStateFlowList[filledColumn.value].update { currentStrategyData ->
            currentStrategyData.copy(
                strategy12 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_12, currentStrategyData.strategy12, last3Inputs),
                strategy34 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_34, currentStrategyData.strategy34, last3Inputs),
                strategy56 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_56, currentStrategyData.strategy56, last3Inputs),
                strategy78 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_78, currentStrategyData.strategy78, last3Inputs)
            )
        }
    }

    private fun updateSingleStrategyListFor3Ways(
        strategyType: StrategyType,
        currentList: List<Strategy3WyasDisplayItem>,
        last3Inputs: List<InputType>
    ): List<Strategy3WyasDisplayItem> {
        val newValue = computeStrategyValue(strategyType, last3Inputs)
        val updatedList = currentList.toMutableList()
        val lastRealIndex = updatedList.indexOfLast { it is Strategy3WyasDisplayItem.Real }

        if (lastRealIndex == -1) {
            updatedList[0] = Strategy3WyasDisplayItem.Real(Strategy3WyasItem(first = newValue))
        } else {
            val lastRealItem = updatedList[lastRealIndex] as Strategy3WyasDisplayItem.Real
            val currentData = lastRealItem.data

            when {
                currentData.first == null -> {
                    updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(first = newValue))
                }

                currentData.second == null -> {
                    updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(second = newValue))
                }
                else -> {
                    val insertIndex = lastRealIndex + 1
                    updatedList.add(insertIndex, Strategy3WyasDisplayItem.Real(Strategy3WyasItem(first = newValue)))
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

    /**
     * 更新 BPPC 表格和策略
     */
    private fun updateGridStrategy(last3Inputs: List<InputType>, filledColumn: ColumnType) {
        // 收集已经出现的且不重复的组合
        val currentConbinations = _uniqueBppcConbinationList[filledColumn.value]
        val inputCombination = last3Inputs.joinToString("")
        currentConbinations.add(inputCombination)

        _strategyGridStateMap.forEach { it ->
            if (it.value == true) {
                updateGridStrategyData(_openInputList.last(), it.key)
            }
        }

        // 判断是否正好存在三未曾出现的组合
        if (currentConbinations.size == THRESHOLD_HAS_SHOWED_CONBINATION) {
            createGridStrategyData(currentConbinations, filledColumn)
            updateGridStrategyData(null, filledColumn)
        }
    }

    private fun createGridStrategyData(currentConbinations: MutableSet<String>, filledColumn: ColumnType) {
        // 获取未出现的组合, 并根据形态的序号排序
        val missingKeys: List<String> = (allBppcConbinations - currentConbinations).sortedBy { key -> bppcCombinationToResult[key] }
        // 获取未出现形态的反形态
        val missingWithAntonyms: List<String> = missingKeys.map { key -> antonymBppcCombination[key]!! }

        // 当已出现的组合达到阈值时，更新策略网格数据 ： 使用未出现的组合的反形态作为预测策略值
        _stragetyGridStateFlow[filledColumn.value].update { currentData ->
            // 将形态转换为字母： “PPB” -> “P", "P", "B"
            val itemList = missingWithAntonyms.map { word ->
                // 第一个 item 为形态标号
                val title = bppcCombinationToResult[word].toString()
                val wordList = word.map { it.toString() }
                // 获取组合的形态和状态：是否已经开过
                StrategyGridItem(false, title, wordList)
            }

            _strategyGridStateMap[filledColumn] = true
            currentData.copy(itemList = itemList)
        }
    }

    private fun updateGridStrategyData(inputType: InputType?, filledColumn: ColumnType) {
        _stragetyGridStateFlow[filledColumn.value].update { currentData ->
            var result = currentData.copy()

            // 1. 更新 actualOpenedList
            val actualOpenedList = result.actualOpenedList.toMutableList().apply {
                if (size >= MAX_COLUMN_COUNT) clear()
                inputType?.let { add(it.toString()) }
            }
            // 2. 准备数据
            val predictedList = result.predictedList.toMutableList().apply {
                if (size >= MAX_COLUMN_COUNT) clear()
            }

            if (actualOpenedList.size >= MAX_COLUMN_COUNT) {
                // 删除需要排除的形态
                val openedSize = actualOpenedList.size
                val itemList = result.itemList.toMutableList().map { item ->
                    if (item.items.withIndex().none { (index, value) ->
                            index < openedSize && value == actualOpenedList[index]
                        }) {
                        // 完全不匹配时标记为 true
                        item.copy(status = true)
                    } else {
                        item // 有匹配时保持不变
                    }
                }
                actualOpenedList.clear()
                // 返回更新后的数据
                result = result.copy(predictedList = predictedList, actualOpenedList = actualOpenedList, itemList = itemList)
            }

            // 如果所有形态都被删除了，不再预测
            if (result.itemList.none { !it.status }) {
                return@update result
            }

            val currentIndex = actualOpenedList.size
            val aRowItemList = if (actualOpenedList.isEmpty()) {
                // 情况1: actualOpenedList 为空，直接提取当前索引的值
                result.itemList.filterNot { it.status }.map { it.items[currentIndex] }
            } else {
                // 情况2: 过滤掉与 actualOpenedList 完全匹配的项，提取当前索引的值
                result.itemList
                    // 过滤掉已经排除的形态
                    .filterNot { it.status }
                    .filterNot { item ->
                        item.items.withIndex().any { (index, value) ->
                            index < actualOpenedList.size && value == actualOpenedList[index]
                        }
                    }
                    .map { it.items[currentIndex] }
                    .ifEmpty { emptyList() }
            }

            // 4. 更新 predictedList
            predictedList.add(if (aRowItemList.distinct().size == 1) aRowItemList.first() else "-")
            // 5. 返回更新后的数据
            result = result.copy(
                predictedList = predictedList,
                actualOpenedList = actualOpenedList,
            )

            return@update result
        }
    }

    /**
     * 撤销最后一个开
     */
    fun removeLastOpen() {
        _openInputList.removeLastOrNull() ?: return

        // 重置展示相关状态
        clearAll()

        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        for (i in _openInputList.indices) {
            if (i >= 2) {
                val last3Inputs = _openInputList.subList(0, i + 1).takeLast(3)
                val lastInput = _openInputList[i]
                updateBppcAndStrantegy(lastInput, last3Inputs)
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

    fun clearAll() {
        _bppcTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        _bppcCounterStateFlow.value = DEFAULT_BPCOUNTER
        _strategy3WaysStateFlowList.forEach { it.value = DEFAULT_STRATEGY_3WAY }
        _stragetyGridStateFlow.forEach { it.value = DEFAULT_STRANTYGE_GRID }
        _predictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }
        _uniqueBppcConbinationList.forEach { it.clear() }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerJob()
    }

    companion object {

        private const val THRESHOLD_HAS_SHOWED_CONBINATION = 5
        private val DEFAULT_PREDICTION = PredictedStrategy3WaysValue()
        private val DEFAULT_STRATEGY_3WAY = Strategy3WaysData()
        private val DEFAULT_BPCOUNTER = Counter()
        private val DEFAULT_TABLE_DISPLAY_LIST = List(MIN_TABLE_COLUMN_COUNT) { TableDisplayItem.Empty }
        private val DEFAULT_STRANTYGE_GRID = StrategyGridInfo()
        private val DEFAULT_STRATEGY_3WAYS = Strategy3WaysData()
        private val DEFAULT_PREDICTED_3WAYS = PredictedStrategy3WaysValue()
        const val MAX_COLUMN_COUNT = 3
        const val MIN_TABLE_COLUMN_COUNT = 30
        private const val MAX_SECONDS = 45 * 60
        private const val MIX_CONBINATION_ITEM_COUNT = 3

        private val bppcCombinationToResult = mapOf(
            "BBB" to 1, "PPP" to 2, "BPP" to 3, "PBB" to 4,
            "PBP" to 5, "BPB" to 6, "PPB" to 7, "BBP" to 8
        )

        //  反形态映射表（斜杠两边互为反形态）
        val antonymBppcCombination = mapOf(
            "BBB" to "PPP",
            "PPP" to "BBB",
            "BPP" to "PBB",
            "PBB" to "BPP",
            "PBP" to "BPB",
            "BPB" to "PBP",
            "PPB" to "BBP",
            "BBP" to "PPB"
        )

        val allBppcConbinations = bppcCombinationToResult.keys.toSet() // 所有合法key

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
