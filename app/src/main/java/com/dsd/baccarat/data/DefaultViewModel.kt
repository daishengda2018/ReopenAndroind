// kotlin
package com.dsd.baccarat.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.baccarat.HistoryActivity
import com.dsd.baccarat.data.room.BetDataDao
import com.dsd.baccarat.data.room.InputDataDao
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * 输入 ViewModel
 */
@HiltViewModel
open class DefaultViewModel @Inject constructor(
    open val repository: Repository,
    open val betDataDao: BetDataDao,
    open val inputDataDao: InputDataDao
) : ViewModel() {

    protected var mOpenInputList: MutableList<InputData> = mutableListOf()
    protected var mBetResultList: MutableList<BetData> = mutableListOf()
    protected var mCompareResultList: MutableList<Boolean> = mutableListOf()

    protected val mWlTableStateFlow = MutableStateFlow<List<TableDisplayItem>>(DEFAULT_TABLE_DISPLAY_LIST)
    val wlTableStateFlow: StateFlow<List<TableDisplayItem>> = mWlTableStateFlow.asStateFlow()

    protected val mWlCounterStateFlow = MutableStateFlow(Counter())
    val wlCounterStateFlow: StateFlow<Counter> = mWlCounterStateFlow.asStateFlow()

    protected val _bppcTableStateFlow = MutableStateFlow<List<TableDisplayItem>>(DEFAULT_TABLE_DISPLAY_LIST)
    val bppcTableStateFlow: StateFlow<List<TableDisplayItem>> = _bppcTableStateFlow.asStateFlow()

    protected val _bppcCounterStateFlow = MutableStateFlow(Counter())
    val bppcCounterStateFlow: StateFlow<Counter> = _bppcCounterStateFlow.asStateFlow()

    protected val _strategy3WaysStateFlowList = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_STRATEGY_3WAYS) }
    val strategy3WaysStateFlowList: List<StateFlow<Strategy3WaysData>> = _strategy3WaysStateFlowList.map { it.asStateFlow() }

    protected val _stragetyGridStateFlow: List<MutableStateFlow<StrategyGridInfo>> =
        List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_STRANTYGE_GRID) }
    val stragetyGridStateFlow: List<StateFlow<StrategyGridInfo>> = _stragetyGridStateFlow.map { it.asStateFlow() }

    // 每列的动态预告 StateFlow（null 表示未知）
    protected val _predictionStateFlowList = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_PREDICTED_3WAYS) }
    val predictedStateFlowList: List<StateFlow<PredictedStrategy3WaysValue>> = _predictionStateFlowList.map { it.asStateFlow() }

    protected val _curBeltInputStageFlow: MutableStateFlow<InputData?> = MutableStateFlow(null)
    val curBeltInputStageFlow = _curBeltInputStageFlow.asStateFlow()

    // Timer state moved to ViewModel
    protected val _timerStatus = MutableStateFlow(TimerStatus.Idle)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    protected val _elapsedTime = MutableStateFlow(0) // 秒
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    protected val _showReminder = MutableStateFlow(false)
    val showReminder: StateFlow<Boolean> = _showReminder.asStateFlow()

    // 用于通知 UI 播放提示音（UI 层收集并调用 Android API）
    protected val _soundEvent = MutableSharedFlow<Unit>()
    val soundEvent = _soundEvent.asSharedFlow()

    protected var _timerJob: Job? = null

    // 用于存储去重后的key（自动去重）
    protected val _uniqueBppcConbinationList = MutableList<MutableSet<String>>(MAX_COLUMN_COUNT, { mutableSetOf() })
    protected val _strategyGridStateMap: MutableMap<ColumnType, Boolean?> = HashMap(MAX_COLUMN_COUNT)

    // 1. 定义 W 类型的热流（MutableStateFlow 作为容器，初始值 0）
    protected val _wHistoryCounter = MutableStateFlow(0)
    val wHistoryCounter: StateFlow<Int> = _wHistoryCounter.asStateFlow() // 暴露不可变的 StateFlow

    // 2. 定义 L 类型的热流
    protected val _lHistoryCounter = MutableStateFlow(0)
    val lHistoryCounter: StateFlow<Int> = _lHistoryCounter.asStateFlow()

    // 输入文字的 StateFlow
    protected val mInputTextStateFlow = MutableStateFlow("")
    val inputText: StateFlow<String> = mInputTextStateFlow.asStateFlow()

    protected var mIsFirstAFor3Ways = true
    protected var mIsFirstBFor3Ways = true

    // 所有存在数据的日期（去重后）
    private val _availableDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val availableDates: StateFlow<List<LocalDate>> = _availableDates

    // 当前选中的日期
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    // 日期选择 Dialog 是否显示
    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // 每次数据变化都会响应的 Flow
        viewModelScope.launch {
            // 收集 Repository 的冷流（wCountFlow）
            repository.wHistoryCountFlow.collect { newCount ->
                // 将新值发射到热流（_wCount）
                _wHistoryCounter.value = newCount
            }
        }

        viewModelScope.launch {
            repository.lHistoryCountFlow.collect { newCount ->
                _lHistoryCounter.value = newCount
            }
        }

        // 只响应一次的 Flow
        viewModelScope.launch {
            mInputTextStateFlow.value = repository.getNoteText()

            mOpenInputList.clear()
            mOpenInputList.addAll(repository.getOpendList())
            resumeOpenedData()

            mWlCounterStateFlow.value = repository.getWinLossCurCount()
        }

        viewModelScope.launch {
            val allBets = betDataDao.getTodayAndHistory().first()
            mBetResultList.clear()
            mBetResultList.addAll(allBets)
            resumeBetedData()
        }

        // 初始化时加载所有存在的日期
        loadAvailableDates()
    }

    /**
     * 从 InputDao 加载所有存在数据的日期
     */
    fun loadAvailableDates() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val timestamps = inputDataDao.getAllCurTimes()
                val dates = DateUtils.extractUniqueDates(timestamps)
                _availableDates.value = dates
                // 默认选中最新的日期（如果有）
                if (dates.isNotEmpty()) {
                    _selectedDate.value = dates.last()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _availableDates.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新选中的日期
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * 显示日期选择 Dialog
     */
    fun showDialog() {
        _isDialogVisible.value = true
    }

    /**
     * 隐藏日期选择 Dialog
     */
    fun dismissDialog() {
        _isDialogVisible.value = false
    }

    /**
     * 确认选择（可选：触发查询等后续操作）
     */
    fun confirmSelection(context: Context) {
        dismissDialog()
        _selectedDate.value ?: return

        // 此处可添加选择后的逻辑（如查询该日期的数据）
        val dayStartAndEnd = DateUtils.getDayStartAndEnd(_selectedDate.value!!)
        val intent = Intent(context, HistoryActivity::class.java)
        // 将 LocalDate 转为 ISO 字符串并传递
        intent.putExtra(KEY_SELECTED_START_TIME, dayStartAndEnd.first)
        intent.putExtra(KEY_SELECTED_END_TIME, dayStartAndEnd.second)
        // 启动 Activity
        context.startActivity(intent)
    }

    fun pauseOrResumeTimer() {
        if (_timerStatus.value == TimerStatus.Running) {
            _timerStatus.value = TimerStatus.Paused
            _timerJob?.cancel()
            _timerJob = null
        } else if (_timerStatus.value == TimerStatus.Paused) {
            _timerStatus.value = TimerStatus.Running
            createAndStartNewTimerJob()
        }
    }

    fun dismissReminder() {
        _showReminder.value = false
    }

    fun startTimer() {
        _elapsedTime.value = 0
        _showReminder.value = false
        _timerStatus.value = TimerStatus.Running
        createAndStartNewTimerJob()
    }

    private fun createAndStartNewTimerJob() {
        _timerJob = viewModelScope.launch {
            while (_timerStatus.value == TimerStatus.Running && _elapsedTime.value < MAX_SECONDS) {
                delay(1000)
                // 使用原子 update，避免竞态
                _elapsedTime.update { it + 1 }
            }
            if (_elapsedTime.value >= MAX_SECONDS) {
                _timerStatus.value = TimerStatus.Finished
                _showReminder.value = true
                _soundEvent.emit(Unit)
            }
        }
    }

    fun stopTimerJob() {
        _elapsedTime.value = 0
        _showReminder.value = false
        _timerStatus.value = TimerStatus.Idle
        _timerJob?.cancel()
        _timerJob = null
    }

    fun openB() {
        mOpenInputList.add(InputData.createB())
        updateOpendList(mOpenInputList)
        updateOpenData()
    }

    fun openP() {
        mOpenInputList.add(InputData.createP())
        updateOpendList(mOpenInputList)
        updateOpenData()
    }

    private fun updateOpenData() {
        // 所有预测，
        updateAllPredictions()
        updateCompareResultList()
        // BPPC 表格和策略
        val lastInput = mOpenInputList.last()
        val last3Inputs = mOpenInputList.takeLast(MIX_CONBINATION_ITEM_COUNT)
        updateBppcAndStrantegy(lastInput, last3Inputs)
        //  WL 表格
        updateWlTable()
    }

    private fun updateCompareResultList() {
        if (mOpenInputList.size < 2) return
        val last2Inputs = mOpenInputList.takeLast(2)
        mCompareResultList.add(last2Inputs[0].inputType == last2Inputs[1].inputType)
    }

    private fun updateBppcAndStrantegy(lastInput: InputData, last3Inputs: List<InputData>) {
        if (mOpenInputList.isNotEmpty()) {
            updateBppcCounter(lastInput)
        }
        if (last3Inputs.size < MIX_CONBINATION_ITEM_COUNT) {
            return
        }

        updateBppcTable(last3Inputs)?.let { filledColumn ->
            update3WayStrategy(filledColumn)
            updateGridStrategy(last3Inputs, filledColumn)
        }
    }

    /**
     * 更新 BPPC 计数器
     */
    private fun updateBppcCounter(lastInput: InputData) {
        _bppcCounterStateFlow.update { currentCounter ->
            when (lastInput.inputType) {
                InputType.B -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                InputType.P -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    /**
     * 更新 BPPC 表格
     */
    private fun updateBppcTable(last3Inputs: List<InputData>): ColumnType? {
        val inputCombination = last3Inputs.map { it.inputType }.joinToString("")
        val result = bppcCombinationToResult[inputCombination] ?: return null
        val columnType = updateTableStageFlow(_bppcTableStateFlow, result, last3Inputs.last().curTime)
        return columnType
    }

    private fun updateAllPredictions() {
        // 如果没有输入，则不更新预测
        if (mOpenInputList.isEmpty()) return

        _predictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }
        val lastIndex = mOpenInputList.lastIndex

        when (lastIndex % 3) {
            ColumnType.A.value -> {
                if (mOpenInputList.size > 3) _predictionStateFlowList[ColumnType.C.value].value = predictNextStrategyValue("3", mOpenInputList)
                _predictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("2", mOpenInputList)
            }

            ColumnType.B.value -> {
                _predictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("3", mOpenInputList)
                _predictionStateFlowList[ColumnType.B.value].value = predictNextStrategyValue("2", mOpenInputList)
            }

            ColumnType.C.value -> {
                _predictionStateFlowList[ColumnType.B.value].value = predictNextStrategyValue("3", mOpenInputList)
                _predictionStateFlowList[ColumnType.C.value].value = predictNextStrategyValue("2", mOpenInputList)
            }
        }
    }

    /**
     * 将预测逻辑做少量简化，便于阅读
     */
    private fun predictNextStrategyValue(title: String, inputHistory: MutableList<InputData>): PredictedStrategy3WaysValue {
        val lastInput = inputHistory.last().inputType
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
        val inputType = _curBeltInputStageFlow.value
        if (inputType == null) {
            return
        }
        if (mOpenInputList.last().inputType == inputType.inputType) {
            val element = BetData.createW()
            mBetResultList.add(element)
            viewModelScope.launch { betDataDao.insert(element) }

        } else {
            val element = BetData.createL()
            mBetResultList.add(element)
            viewModelScope.launch { betDataDao.insert(element) }
        }

        val last3Inputs = mBetResultList.takeLast(3)
        if (last3Inputs.size < 3) {
            _curBeltInputStageFlow.update { null }
            return
        }

        val inputCombination = last3Inputs.map { it.type }.joinToString("")
        val result = wlCombinationToResult[inputCombination] ?: run {
            _curBeltInputStageFlow.update { null }
            return
        }

        Log.d("InputViewModel", "Current Inputs: $last3Inputs")
        val lastResult = mBetResultList.last()
        updateWlCounter(lastResult)
        viewModelScope.launch { repository.updateWlCount(lastResult, OperationType.INCREMENT) }

        updateTableStageFlow(mWlTableStateFlow, result, lastResult.curTime)
        _curBeltInputStageFlow.update { null }
    }

    private fun updateTableStageFlow(tableStateFlow: MutableStateFlow<List<TableDisplayItem>>, result: Int, curTime: Long): ColumnType? {
        var filledColumn: ColumnType? = null
        val isSameDayLegacy = isSameDayLegacy(System.currentTimeMillis(), curTime)
        val data = Pair(!isSameDayLegacy, result)

        tableStateFlow.update { currentList ->
            val updatedList = currentList.toMutableList()
            val lastRealIndex = updatedList.indexOfLast { it is TableDisplayItem.Real }

            if (lastRealIndex == -1) {
                updatedList[0] = TableDisplayItem.Real(TableItem(dataA = data))
                filledColumn = ColumnType.A
            } else {
                val lastRealItem = updatedList[lastRealIndex] as TableDisplayItem.Real
                val currentData = lastRealItem.data

                when {
                    currentData.dataA == null -> {
                        updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataA = data))
                        filledColumn = ColumnType.A
                    }

                    currentData.dataB == null -> {
                        updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataB = data))
                        filledColumn = ColumnType.B
                    }

                    currentData.dataC == null -> {
                        updatedList[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataC = data))
                        filledColumn = ColumnType.C
                    }

                    else -> {
                        val insertIndex = lastRealIndex + 1
                        updatedList.add(insertIndex, TableDisplayItem.Real(TableItem(dataA = data)))
                        filledColumn = ColumnType.A
                        if (updatedList.size > MIN_TABLE_COLUMN_COUNT && updatedList.last() is TableDisplayItem.Empty) {
                            updatedList.removeLastOrNull()
                        }
                    }
                }
            }
            updatedList
        }
        return filledColumn
    }

    private fun isSameDayLegacy(time1: Long, time2: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 每次创建新实例（避免线程安全问题）
        val dateStr1 = sdf.format(time1)
        val dateStr2 = sdf.format(time2)
        return dateStr1 == dateStr2
    }


    private fun updateWlCounter(lastResult: BetData) {
        mWlCounterStateFlow.update { currentCounter ->
            when (lastResult.type) {
                BetResultType.W -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                BetResultType.L -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    /**
     * 更新 3 种策略
     */
    private fun update3WayStrategy(filledColumn: ColumnType) {
        _strategy3WaysStateFlowList[filledColumn.value].update { currentStrategyData ->
            val compareResultList = mCompareResultList.takeLast(2)
            val compareResultPair = Pair(compareResultList[0], compareResultList[1])
            currentStrategyData.copy(
                strategy12 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_12, currentStrategyData.strategy12, compareResultPair),
                strategy34 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_34, currentStrategyData.strategy34, compareResultPair),
                strategy56 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_56, currentStrategyData.strategy56, compareResultPair),
                strategy78 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_78, currentStrategyData.strategy78, compareResultPair)
            )
        }

        val columnType = RELEVANCY_MAP[filledColumn] ?: return
        val compareResultList = mCompareResultList.takeLast(3)
        val compareResultPair = if (filledColumn == ColumnType.A && mIsFirstAFor3Ways) {
            mIsFirstAFor3Ways = false
            null
        } else if (filledColumn == ColumnType.B && mIsFirstBFor3Ways) {
            mIsFirstBFor3Ways = false
            null
        } else {
            if (compareResultList.size >= 3) {
                Pair(compareResultList[0], compareResultList[2])
            } else {
                null
            }
        }

        _strategy3WaysStateFlowList[columnType.value].update { currentStrategyData ->
            currentStrategyData.copy(
                strategy12 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_12, currentStrategyData.strategy12, compareResultPair),
                strategy34 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_34, currentStrategyData.strategy34, compareResultPair),
                strategy56 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_56, currentStrategyData.strategy56, compareResultPair),
                strategy78 = updateSingleStrategyListFor3Ways(StrategyType.STRATEGY_78, currentStrategyData.strategy78, compareResultPair),
            )
        }
    }

    private fun updateSingleStrategyListFor3Ways(
        strategyType: StrategyType,
        currentList: List<Strategy3WyasDisplayItem>,
        compareResultPair: Pair<Boolean, Boolean>?
    ): List<Strategy3WyasDisplayItem> {
        val newValue = if (compareResultPair == null) -1 else computeStrategyValue(strategyType, compareResultPair)
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
                    if (updatedList.size > MIN_TABLE_COLUMN_COUNT && updatedList.last() is Strategy3WyasDisplayItem.Empty) {
                        updatedList.removeLastOrNull()
                    }
                }
            }
        }
        return updatedList
    }

    private fun computeStrategyValue(type: StrategyType, compareResultPair: Pair<Boolean, Boolean>): Int {
        return when (type) {
            StrategyType.STRATEGY_12 -> strategy12Map[compareResultPair] ?: 4
            StrategyType.STRATEGY_34 -> strategy34Map[compareResultPair] ?: 4
            StrategyType.STRATEGY_56 -> strategy56Map[compareResultPair] ?: 4
            StrategyType.STRATEGY_78 -> strategy78Map[compareResultPair] ?: 4
        }
    }

    /**
     * 更新 BPPC 表格和策略
     */
    private fun updateGridStrategy(last3Inputs: List<InputData>, filledColumn: ColumnType) {
        // 收集已经出现的且不重复的组合
        val currentConbinations = _uniqueBppcConbinationList[filledColumn.value]
        val inputCombination = last3Inputs.map { it.inputType }.joinToString("")
        currentConbinations.add(inputCombination)

        _strategyGridStateMap.forEach { it ->
            if (it.value == true) {
                updateGridStrategyData(mOpenInputList.last(), it.key)
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

    private fun updateGridStrategyData(inputData: InputData?, filledColumn: ColumnType) {
        _stragetyGridStateFlow[filledColumn.value].update { currentData ->
            var result = currentData.copy()

            // 1. 更新 actualOpenedList
            val actualOpenedList = result.actualOpenedList.toMutableList().apply {
                if (size >= MAX_COLUMN_COUNT) clear()
                inputData?.let { add(it.inputType.toString()) }
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
        mOpenInputList.removeLastOrNull() ?: return
        updateOpendList(mOpenInputList)
        // 重置展示相关状态
        resumeOpenedData()
    }

    protected fun resumeOpenedData() {
        clearAllStateFlow()
        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        for (i in mOpenInputList.indices) {
            if (i >= 1) {
                mCompareResultList.add(mOpenInputList[i - 1].inputType == mOpenInputList[i].inputType)
            }
            if (i >= 2) {
                val last3Inputs = mOpenInputList.subList(0, i + 1).takeLast(3)
                val lastInput = mOpenInputList[i]
                updateBppcAndStrantegy(lastInput, last3Inputs)
            }
        }

        if (mOpenInputList.isNotEmpty()) {
            updateAllPredictions()
        }
    }

    fun betB() {
        _curBeltInputStageFlow.update { InputData.createB() }
    }

    fun betP() {
        _curBeltInputStageFlow.update { InputData.createP() }
    }

    fun removeLastBet() {
        if (_curBeltInputStageFlow.value != null) {
            _curBeltInputStageFlow.update { null }
        } else {
            val last = mBetResultList.removeLastOrNull()
            last ?: return
            viewModelScope.launch {
                repository.updateWlCount(last, OperationType.DECREMENT)
                betDataDao.deleteByTime(last.curTime)
            }
        }

        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        resumeBetedData()
    }

    protected fun resumeBetedData() {
        // 重置展示相关状态
        mWlTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        mWlCounterStateFlow.value = DEFAULT_BPCOUNTER

        for (i in mBetResultList.indices) {
            if (i >= 2) {
                val last3Inputs = mBetResultList.subList(0, i + 1).takeLast(3)
                updateWlCounter(mBetResultList[i])
                val inputCombination = last3Inputs.map { it.type }.joinToString("")
                wlCombinationToResult[inputCombination]?.let { result ->
                    updateTableStageFlow(mWlTableStateFlow, result, last3Inputs.last().curTime)
                }
            }
        }
    }

    // 更新并保存数据
    fun updateOpendList(newList: List<InputData>) {
        viewModelScope.launch {
            repository.saveOpendList(newList)
        }
    }

    fun updateInputText(text: String) {
        // 存储在用户点击 新牌、保持之前输入的内容
        viewModelScope.launch { repository.saveNoteText(text) }
        mInputTextStateFlow.value = text
    }

    fun clearAllStateFlow() {
        _bppcTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        _bppcCounterStateFlow.value = DEFAULT_BPCOUNTER

        _strategy3WaysStateFlowList.forEach { it.value = DEFAULT_STRATEGY_3WAY }
        _stragetyGridStateFlow.forEach { it.value = DEFAULT_STRANTYGE_GRID }
        _predictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }

        _uniqueBppcConbinationList.forEach { it.clear() }

        mWlCounterStateFlow.value = DEFAULT_BPCOUNTER
        mWlTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST

        mInputTextStateFlow.value = ""
    }

    fun newGame() {
        clearAllStateFlow()
        mOpenInputList.clear()
        mBetResultList.clear()
        mCompareResultList.clear()
        viewModelScope.launch {
            repository.saveNoteText("")
            repository.saveOpendList(mOpenInputList)
            repository.clearCurWinLossCount()
        }
    }

    fun save() {
        viewModelScope.launch {
            inputDataDao.insertAll(mOpenInputList)
        }
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

        val RELEVANCY_MAP = mapOf(ColumnType.A to ColumnType.B, ColumnType.B to ColumnType.C, ColumnType.C to ColumnType.A)
        const val KEY_SELECTED_START_TIME = "key_selected_start_time"
        const val KEY_SELECTED_END_TIME = "key_selected_END_time"
    }
}