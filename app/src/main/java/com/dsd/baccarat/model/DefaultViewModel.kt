// kotlin
package com.dsd.baccarat.model

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.baccarat.HistoryActivity
import com.dsd.baccarat.data.BetResultType
import com.dsd.baccarat.data.ColumnType
import com.dsd.baccarat.data.Counter
import com.dsd.baccarat.data.InputType
import com.dsd.baccarat.data.OperationType
import com.dsd.baccarat.data.PredictedStrategy3WaysValue
import com.dsd.baccarat.data.Strategy3WaysData
import com.dsd.baccarat.data.Strategy3WyasDisplayItem
import com.dsd.baccarat.data.Strategy3WyasItem
import com.dsd.baccarat.data.StrategyGridInfo
import com.dsd.baccarat.data.StrategyGridItem
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TableItem
import com.dsd.baccarat.data.TemporaryStorageRepository
import com.dsd.baccarat.data.TimerStatus
import com.dsd.baccarat.data.room.dao.BetDataDao
import com.dsd.baccarat.data.room.dao.GameSessionDao
import com.dsd.baccarat.data.room.dao.InputDataDao
import com.dsd.baccarat.data.room.dao.NoteDataDao
import com.dsd.baccarat.data.room.entity.BetEntity
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import com.dsd.baccarat.data.room.entity.InputEntity
import com.dsd.baccarat.data.room.entity.NoteEntity
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
import java.util.Locale

/**
 * 输入 ViewModel
 */
@HiltViewModel
open class DefaultViewModel @Inject constructor(
    open val repository: TemporaryStorageRepository,
    open val betDataDao: BetDataDao,
    open val inputDataDao: InputDataDao,
    open val noteDataDao: NoteDataDao,

    open val gameSessionDao: GameSessionDao,
) : ViewModel() {

    protected var mOpenInputList: MutableList<InputEntity> = mutableListOf()
    protected var mBetResultList: MutableList<BetEntity> = mutableListOf()
    protected var m12CompareResultList: MutableList<Boolean> = mutableListOf()
    protected var m34CompareResultList: MutableMap<ColumnType, MutableList<Boolean>> = mutableMapOf()

    protected val mWlTableStateFlow = MutableStateFlow<List<TableDisplayItem>>(DEFAULT_TABLE_DISPLAY_LIST)
    val wlTableStateFlow: StateFlow<List<TableDisplayItem>> = mWlTableStateFlow.asStateFlow()

    protected val mWlCounterStateFlow = MutableStateFlow(Counter())
    val wlCounterStateFlow: StateFlow<Counter> = mWlCounterStateFlow.asStateFlow()

    protected val mBppcTableStateFlow = MutableStateFlow<List<TableDisplayItem>>(DEFAULT_TABLE_DISPLAY_LIST)
    val bppcTableStateFlow: StateFlow<List<TableDisplayItem>> = mBppcTableStateFlow.asStateFlow()

    protected val mBppcCounterStateFlow = MutableStateFlow(Counter())
    val bppcCounterStateFlow: StateFlow<Counter> = mBppcCounterStateFlow.asStateFlow()

    protected val mStrategy3WaysStateFlowList = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_STRATEGY_3WAYS) }
    val strategy3WaysStateFlowList: List<StateFlow<Strategy3WaysData>> = mStrategy3WaysStateFlowList.map { it.asStateFlow() }

    protected val mStragetyGridStateFlow: List<MutableStateFlow<StrategyGridInfo>> =
        List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_STRANTYGE_GRID) }
    val stragetyGridStateFlow: List<StateFlow<StrategyGridInfo>> = mStragetyGridStateFlow.map { it.asStateFlow() }

    // 每列的动态预告 StateFlow（null 表示未知）
    protected val mPredictionStateFlowList = List(MAX_COLUMN_COUNT) { MutableStateFlow(DEFAULT_PREDICTED_3WAYS) }
    val predictedStateFlowList: List<StateFlow<PredictedStrategy3WaysValue>> = mPredictionStateFlowList.map { it.asStateFlow() }

    protected val mCurBeltInputStageFlow: MutableStateFlow<InputEntity?> = MutableStateFlow(null)
    val curBeltInputStageFlow = mCurBeltInputStageFlow.asStateFlow()

    protected val mIsOnlyShowNewGameStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOnlyShowNewGameStateFlow: StateFlow<Boolean> = mIsOnlyShowNewGameStateFlow.asStateFlow()

    // Timer state moved to ViewModel
    protected val mTimerStatus = MutableStateFlow(TimerStatus.Idle)
    val timerStatus: StateFlow<TimerStatus> = mTimerStatus.asStateFlow()

    protected val mElapsedTime = MutableStateFlow(0) // 秒
    val elapsedTime: StateFlow<Int> = mElapsedTime.asStateFlow()

    protected val mShowReminder = MutableStateFlow(false)
    val showReminder: StateFlow<Boolean> = mShowReminder.asStateFlow()

    // 用于通知 UI 播放提示音（UI 层收集并调用 Android API）
    protected val mSoundEvent = MutableSharedFlow<Unit>()
    val soundEvent = mSoundEvent.asSharedFlow()

    protected var mTimerJob: Job? = null

    // 用于存储去重后的key（自动去重）
    protected val mUniqueBppcConbinationList = MutableList<MutableSet<String>>(MAX_COLUMN_COUNT, { mutableSetOf() })
    protected val mStrategyGridStateMap: MutableMap<ColumnType, Boolean?> = HashMap(MAX_COLUMN_COUNT)

    // 1. 定义 W 类型的热流（MutableStateFlow 作为容器，初始值 0）
    protected val mWHistoryCounter = MutableStateFlow(0)
    val wHistoryCounter: StateFlow<Int> = mWHistoryCounter.asStateFlow() // 暴露不可变的 StateFlow

    // 2. 定义 L 类型的热流
    protected val mLHistoryCounter = MutableStateFlow(0)
    val lHistoryCounter: StateFlow<Int> = mLHistoryCounter.asStateFlow()

    // 输入文字的 StateFlow
    protected val mInputTextStateFlow = MutableStateFlow("")
    val inputText: StateFlow<String> = mInputTextStateFlow.asStateFlow()

    // 所有存在数据的日期（去重后）
    private val _availableDates = MutableStateFlow<List<GameSessionEntity>>(emptyList())
    val availableDates: StateFlow<List<GameSessionEntity>> = _availableDates

    // 当前选中的日期
    private val _selectedDate = MutableStateFlow<GameSessionEntity?>(null)
    val selectedDate: StateFlow<GameSessionEntity?> = _selectedDate

    // 日期选择 Dialog 是否显示
    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private var mGameId = ""

    var mTestIndex = 0

    init {
        setup()
    }

    protected open fun setup() {
        // 每次数据变化都会响应的 Flow
        viewModelScope.launch {
            // 收集 TemporaryStorageRepository 的冷流（wCountFlow）
            repository.wHistoryCountFlow.collect { newCount ->
                // 将新值发射到热流（_wCount）
                mWHistoryCounter.value = newCount
            }
        }

        viewModelScope.launch {
            repository.lHistoryCountFlow.collect { newCount ->
                mLHistoryCounter.value = newCount
            }
        }

        // 只响应一次的 Flow
        viewModelScope.launch {
            // 恢复未经处理的 Input 数据
            mOpenInputList.clear()
            inputDataDao.getInputsByGameId(mGameId)
            mOpenInputList.addAll(repository.getOpendList())
            resumeOpenedData()

            mWlCounterStateFlow.value = repository.getWinLossCurCount()

            mInputTextStateFlow.value = repository.getNoteText()

            mGameId = gameSessionDao.getActiveSession()?.gameId ?: ""
            mIsOnlyShowNewGameStateFlow.value = mGameId.isEmpty()

            val historyList = betDataDao.loadHistory().map { it.copy().apply { isHistory = true } }
            val curBetList = if (mGameId.isNotEmpty()) {
                betDataDao.loadDataWithGameId(mGameId)
            } else {
                emptyList()
            }
            recordBetDataOnStartup(historyList, curBetList)
        }
    }

    protected fun recordBetDataOnStartup(historyList: List<BetEntity>, curBetList: List<BetEntity>) {
        // 恢复最近的 65 手记录
        mBetResultList.clear()
        mBetResultList.addAll(historyList)
        // 寻找上一局还没有结束的游戏数据

        mBetResultList.addAll(curBetList)

        // 恢复押注数据
        resumeBetedData(false)
        // 恢复上一次游戏的 Counter
        curBetList.forEach { updateWlCounter(it) }
    }

    /**
     * 更新选中的日期
     */
    fun selectDate(date: GameSessionEntity) {
        _selectedDate.value = date
    }

    /**
     * 显示日期选择 Dialog
     */
    fun showSelectHistoryDialog() {
        // 从 InputDao 加载所有存在数据的日期
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val allHistorySessions = gameSessionDao.getAllHistorySessions().first()
                _availableDates.value = allHistorySessions
                // 默认选中最新的日期（如果有）
                if (allHistorySessions.isNotEmpty()) {
                    _selectedDate.value = allHistorySessions.first()
                }
                _isDialogVisible.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _availableDates.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
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

        val intent = Intent(context, HistoryActivity::class.java)
        intent.putExtra(KEY_GAME_ID, _selectedDate.value!!.gameId)
        intent.putExtra(KEY_START_TIME, _selectedDate.value!!.startTime)
        intent.putExtra(KEY_END_TIME, _selectedDate.value!!.endTime)
        // 启动 Activity
        context.startActivity(intent)
    }

    fun pauseOrResumeTimer() {
        if (mTimerStatus.value == TimerStatus.Running) {
            mTimerStatus.value = TimerStatus.Paused
            mTimerJob?.cancel()
            mTimerJob = null
        } else if (mTimerStatus.value == TimerStatus.Paused) {
            mTimerStatus.value = TimerStatus.Running
            createAndStartNewTimerJob()
        }
    }

    fun dismissReminder() {
        mShowReminder.value = false
    }

    fun startTimer() {
        mElapsedTime.value = 0
        mShowReminder.value = false
        mTimerStatus.value = TimerStatus.Running
        createAndStartNewTimerJob()
    }

    private fun createAndStartNewTimerJob() {
        mTimerJob = viewModelScope.launch {
            while (mTimerStatus.value == TimerStatus.Running && mElapsedTime.value < MAX_SECONDS) {
                delay(1000)
                // 使用原子 update，避免竞态
                mElapsedTime.update { it + 1 }
            }
            if (mElapsedTime.value >= MAX_SECONDS) {
                mTimerStatus.value = TimerStatus.Finished
                mShowReminder.value = true
                mSoundEvent.emit(Unit)
            }
        }
    }

    fun stopTimerJob() {
        mElapsedTime.value = 0
        mShowReminder.value = false
        mTimerStatus.value = TimerStatus.Idle
        mTimerJob?.cancel()
        mTimerJob = null
    }

    fun openB() {
        mOpenInputList.add(InputEntity.createB(mGameId))
        updateOpendList(mOpenInputList)
        updateOpenData()
    }

    fun openP() {
        mOpenInputList.add(InputEntity.createP(mGameId))
        updateOpendList(mOpenInputList)
        updateOpenData()
    }

    private fun updateOpenData() {
        // 所有预测，
        updateAllPredictions()
        if (mOpenInputList.size >= 2) {
            val last2Inputs = mOpenInputList.takeLast(2)
            val firstInputType = last2Inputs[0].inputType
            val secondInputType = last2Inputs[1].inputType
            updateCompareResultList(firstInputType, secondInputType, mOpenInputList.size)
        }

        // BPPC 表格和策略
        val lastInput = mOpenInputList.last()
        val last3Inputs = mOpenInputList.takeLast(MIX_CONBINATION_ITEM_COUNT)
        updateBppcAndStrantegy(lastInput, last3Inputs)
        //  WL 表格
        updateWlTable()
    }

    private fun updateCompareResultList(
        firstInputType: InputType,
        secondInputType: InputType,
        inputListSize: Int
    ) {
        // m12 Compare
        m12CompareResultList.add(firstInputType == secondInputType)

        if (inputListSize < 2) return

        val eq = (firstInputType == secondInputType)
        val neq = !eq

        // inputListSize == 2 单独处理
        if (inputListSize == 2) {
            m34CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(neq)
            return
        }

        // inputListSize >= 3 后三类按 n%3 走
        when (inputListSize % 3) {

            // ==========================
            // n % 3 == 0  → (A,B) = (==, !=)
            // ==========================
            0 -> {
                m34CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(eq)
                m34CompareResultList.getOrPut(ColumnType.B) { mutableListOf() }.add(neq)
            }

            // ==========================
            // n % 3 == 1  → (B,C) = (==, !=)
            // n % 3 == 1  → (B,C) = (==, !=)
            // ==========================
            1 -> {
                m34CompareResultList.getOrPut(ColumnType.B) { mutableListOf() }.add(eq)
                m34CompareResultList.getOrPut(ColumnType.C) { mutableListOf() }.add(neq)
            }

            // ==========================
            // n % 3 == 2  → (A,C) = (!=, ==)
            // ==========================
            2 -> {
                m34CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(neq)
                m34CompareResultList.getOrPut(ColumnType.C) { mutableListOf() }.add(eq)
            }
        }
    }

    private fun updateBppcAndStrantegy(lastInput: InputEntity, last3Inputs: List<InputEntity>) {
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
    private fun updateBppcCounter(lastInput: InputEntity) {
        mBppcCounterStateFlow.update { currentCounter ->
            when (lastInput.inputType) {
                InputType.B -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                InputType.P -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    /**
     * 更新 BPPC 表格
     */
    private fun updateBppcTable(last3Inputs: List<InputEntity>): ColumnType? {
        val inputCombination = last3Inputs.map { it.inputType }.joinToString("")
        val result = bppcCombinationToResult[inputCombination] ?: return null
        val columnType = updateTableStageFlow(mBppcTableStateFlow, result, isHistory(last3Inputs.last().curTime))
        return columnType
    }

    private fun updateAllPredictions() {
        // 如果没有输入，则不更新预测
        if (mOpenInputList.isEmpty()) return

        mPredictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }
        val lastIndex = mOpenInputList.lastIndex

        when (lastIndex % 3) {
            ColumnType.A.value -> {
                if (mOpenInputList.size > 3) {
                    mPredictionStateFlowList[ColumnType.C.value].value = predictNextStrategyValue("C", mOpenInputList)
                    mPredictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("C", mOpenInputList)
                } else {
                    mPredictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("A", mOpenInputList)
                }
            }

            ColumnType.B.value -> {
                mPredictionStateFlowList[ColumnType.A.value].value = predictNextStrategyValue("A", mOpenInputList)
                mPredictionStateFlowList[ColumnType.B.value].value = predictNextStrategyValue("A", mOpenInputList)
            }

            ColumnType.C.value -> {
                mPredictionStateFlowList[ColumnType.B.value].value = predictNextStrategyValue("B", mOpenInputList)
                mPredictionStateFlowList[ColumnType.C.value].value = predictNextStrategyValue("B", mOpenInputList)
            }
        }
    }

    /**
     * 将预测逻辑做少量简化，便于阅读
     */
    private fun predictNextStrategyValue(title: String, inputHistory: MutableList<InputEntity>): PredictedStrategy3WaysValue {
        val lastInput = inputHistory.last().inputType
        val isOddNumber = (inputHistory.lastIndex % 2 != 0)
        fun flip(input: InputType) = if (input == InputType.B) InputType.P else InputType.B

        val strategy12 = lastInput.value
        val strategy56 = flip(lastInput).value

        val strategy34 = (if (isOddNumber) lastInput else flip(lastInput)).value
        val strategy78 = (if (isOddNumber) flip(lastInput) else lastInput).value

        return PredictedStrategy3WaysValue(title, strategy12, strategy34, strategy56, strategy78)
    }

    /**
     * 更新 WL 表格
     */
    private fun updateWlTable() {
        val inputType = mCurBeltInputStageFlow.value ?: return
        if (mOpenInputList.last().inputType == inputType.inputType) {
            val element = BetEntity.createW(mGameId)
            mBetResultList.add(element)
            viewModelScope.launch { betDataDao.insert(element) }

        } else {
            val element = BetEntity.createL(mGameId)
            mBetResultList.add(element)
            viewModelScope.launch { betDataDao.insert(element) }
        }

        val last3Inputs = mBetResultList.takeLast(3)
        if (last3Inputs.size < 3) {
            mCurBeltInputStageFlow.update { null }
            return
        }

        val inputCombination = last3Inputs.map { it.type }.joinToString("")
        val result = wlCombinationToResult[inputCombination] ?: run {
            mCurBeltInputStageFlow.update { null }
            return
        }

        Log.d("InputViewModel", "Current Inputs: $last3Inputs")
        val lastResult = mBetResultList.last()
        updateWlCounter(lastResult)
        viewModelScope.launch { repository.updateWlCount(lastResult, OperationType.INCREMENT) }

        updateTableStageFlow(mWlTableStateFlow, result, isHistory(lastResult.curTime))
        mCurBeltInputStageFlow.update { null }
    }

    private fun updateTableStageFlow(tableStateFlow: MutableStateFlow<List<TableDisplayItem>>, result: Int, isHistory: Boolean): ColumnType? {
        var filledColumn: ColumnType? = null
        val data = Pair(isHistory, result)

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

    protected fun isHistory(time: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // 每次创建新实例（避免线程安全问题）
        val dateStr1 = sdf.format(System.currentTimeMillis())
        val dateStr2 = sdf.format(time)
        return dateStr1 != dateStr2
    }

    private fun updateWlCounter(lastResult: BetEntity) {
        mWlCounterStateFlow.update { currentCounter ->
            when (lastResult.type) {
                BetResultType.W -> currentCounter.copy(count1 = currentCounter.count1 + 1)
                BetResultType.L -> currentCounter.copy(count2 = currentCounter.count2 + 1)
            }
        }
    }

    /**
     * 更新 3 路策略 根据是否需要 getCompareResultPair 取 2 或 3 条历史
     */
    private fun update3WayStrategy(filledColumn: ColumnType) {
        // 1) 更新 filledColumn —— 原实现对 filledColumn 用的是直接最近 2 条（看你原逻辑）
        updateColumnStrategy(
            column = filledColumn,
            // 取 compare12：第一次与第二次 update 都使用同一来源，但长度需求不同
            rawCompare12 = m12CompareResultList.takeLast(2),
            rawCompare34 = m34CompareResultList[filledColumn]?.takeLast(2) ?: emptyList(),
            is2ndRow = false
        )

        // 2) 更新关联列 —— 原实现第二次 update 用的是可能需要 "第1/第3" 的逻辑（需要最近 3 条）
        val relatedColumn = RELEVANCY_MAP[filledColumn] ?: return
        // 对 compare12: 如果 getCompareResultPair 需要第1与第3，则给它最近三条
        updateColumnStrategy(
            column = relatedColumn,
            rawCompare12 = m12CompareResultList.takeLast(3),    // 如果需要 pair(0,2),
            rawCompare34 = m34CompareResultList[relatedColumn]?.takeLast(2) ?: emptyList(),
            is2ndRow = true
        )
    }

    private fun updateColumnStrategy(
        column: ColumnType,
        rawCompare12: List<Boolean>,
        rawCompare34: List<Boolean>,
        is2ndRow: Boolean
    ) {
        mStrategy3WaysStateFlowList[column.value].update { current ->
            val p12 = if (is2ndRow) rawCompare12.pairFirstThirdOrNull() else rawCompare12.pairFirstTwoOrNull()
            val p34 = rawCompare34.pairFirstTwoOrNull()
            if (column == ColumnType.A) {
                mTestIndex += 1
                if (is2ndRow) {
                    Log.d("### 34", "2nd num$mTestIndex $p34 ")
                } else {
                    Log.d("### 34", "1st num$mTestIndex $p34 ")
                }
            }

            current.copy(
                strategy12 = updateSingleStrategyListFor3Ways(current.strategy12, p12),
                strategy34 = updateSingleStrategyListFor3Ways(current.strategy34, p34),
                strategy56 = updateSingleStrategyListFor3Ways(current.strategy56, p12.inverted()),
                strategy78 = updateSingleStrategyListFor3Ways(current.strategy78, p34.inverted()),
            )
        }
    }

    // 辅助：当 list 大小足够时返回 Pair(0,1) 或 Pair(0,2)（由需要决定）
    private fun List<Boolean>.pairFirstTwoOrNull(): Pair<Boolean, Boolean>? {
        return if (size >= 2) Pair(this[0], this[1]) else null
    }

    // 取 first(older) 和 third(older) —— 这里按原逻辑 getCompareResultPair 需要 (0,2)
    private fun List<Boolean>.pairFirstThirdOrNull(): Pair<Boolean, Boolean>? {
        return if (size >= 3) Pair(this[0], this[2]) else null
    }

    private fun Pair<Boolean, Boolean>?.inverted(): Pair<Boolean, Boolean>? {
        return this?.let { Pair(!it.first, !it.second) }
    }

    private fun updateSingleStrategyListFor3Ways(
        currentList: List<Strategy3WyasDisplayItem>,
        compareResultPair: Pair<Boolean, Boolean>?
    ): List<Strategy3WyasDisplayItem> {
        val newValue = if (compareResultPair == null) -1 else strategyMap[compareResultPair]
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

    /**
     * 更新九宫格策略
     */
    private fun updateGridStrategy(last3Inputs: List<InputEntity>, filledColumn: ColumnType) {
        // 收集已经出现的且不重复的组合
        val currentConbinations = mUniqueBppcConbinationList[filledColumn.value]
        val inputCombination = last3Inputs.map { it.inputType }.joinToString("")
        currentConbinations.add(inputCombination)

        mStrategyGridStateMap.forEach {
            // true 表示已存在三个未曾出现的组合, 直接更新
            if (it.value == true) {
                updateGridStrategyData(last3Inputs.last(), it.key)
            }
        }

        // 判断是否正好存在三个未曾出现的组合：创建 + 更新
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
        mStragetyGridStateFlow[filledColumn.value].update { currentData ->
            // 将形态转换为字母： “PPB” -> “P", "P", "B"
            val itemList = missingWithAntonyms.map { word ->
                // 第一个 item 为形态标号
                val title = bppcCombinationToResult[word].toString()
                val wordList = word.map { it.toString() }
                // 获取组合的形态和状态：是否已经开过
                StrategyGridItem(false, title, wordList)
            }

            mStrategyGridStateMap[filledColumn] = true
            currentData.copy(itemList = itemList)
        }
    }

    private fun updateGridStrategyData(inputData: InputEntity?, filledColumn: ColumnType) {
        mStragetyGridStateFlow[filledColumn.value].update { currentData ->
            var result = currentData.copy()

            // 1. 更新 actualOpenedList
            val actualOpenedList = result.actualOpenedList.toMutableList().apply {
                if (size >= MAX_COLUMN_COUNT) clear()
                if (inputData != null) {
                    this.add(inputData.inputType.toString())
                }
            }
            // 2. 准备数据
            val predictedList = result.predictedList.toMutableList().apply {
                if (size >= MAX_COLUMN_COUNT) clear()
            }

            if (actualOpenedList.size >= MAX_COLUMN_COUNT) {
                // 删除需要排除的形态
                val openedSize = actualOpenedList.size
                val itemList = result.itemList.toMutableList().map { item ->
                    // 检查 item.items 中是否「没有任何元素」满足匹配条件
                    val isAllItemsMismatch = item.items.withIndex().none { (index, value) ->
                        // 匹配条件：索引在有效范围内，且值相等
                        index < openedSize && value == actualOpenedList[index]
                    }

                    if (isAllItemsMismatch) {
                        // 所有 item 都不匹配时，标记 isObslate = true
                        item.copy(isObslate = true)
                    } else {
                        item
                    }
                }
                actualOpenedList.clear()
                // 返回更新后的数据
                result = result.copy(predictedList = predictedList, actualOpenedList = actualOpenedList, itemList = itemList)
            }

            // 如果所有形态都被删除了，不再预测
            if (result.itemList.none { !it.isObslate }) {
                return@update result
            }

            val currentIndex = actualOpenedList.size
            val aRowItemList = if (actualOpenedList.isEmpty()) {
                // 情况1: actualOpenedList 为空，直接提取当前索引的值
                result.itemList.filterNot { it.isObslate }.map { it.items[currentIndex] }
            } else {
                // 情况2: 过滤掉与 actualOpenedList 完全匹配的项，提取当前索引的值
                result.itemList
                    // 过滤掉已经排除的形态
                    .filterNot { it.isObslate }
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
        clearBppcStateFlow()
        Log.d("###", "resumeOpenedData ${mOpenInputList.map { it.inputType }}")
        // 从头重建（仅在 i >= 2 时触发表格/策略更新）
        for (i in mOpenInputList.indices) {
            if (i >= 1) {
                updateCompareResultList(mOpenInputList[i - 1].inputType, mOpenInputList[i].inputType, i + 1)
            }
            if (i >= 2) {
                val last3Inputs = mOpenInputList.subList(0, i + 1).takeLast(3)
                val lastInput = mOpenInputList[i]
                updateBppcAndStrantegy(lastInput, last3Inputs)
            } else {
                updateBppcCounter(mOpenInputList[i])
            }
        }

        if (mOpenInputList.isNotEmpty()) {
            updateAllPredictions()
        }
    }

    fun betB() {
        mCurBeltInputStageFlow.update { InputEntity.createB(mGameId) }
    }

    fun betP() {
        mCurBeltInputStageFlow.update { InputEntity.createP(mGameId) }
    }

    fun removeLastBet() {
        if (mCurBeltInputStageFlow.value != null) {
            mCurBeltInputStageFlow.update { null }
        } else {
            // 历史数据不能撤销
            if (mBetResultList.lastOrNull()?.isHistory == true) return

            // 尝试删除本次游戏的数据
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

    protected fun resumeBetedData(shouldCalculateCounter: Boolean = true) {
        // 重置展示相关状态
        mWlTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        mWlCounterStateFlow.value = DEFAULT_BPCOUNTER

        for (i in mBetResultList.indices) {
            if (i >= 2) {
                val last3Inputs = mBetResultList.subList(0, i + 1).takeLast(3)
                if (shouldCalculateCounter) {
                    updateWlCounter(mBetResultList[i])
                }
                val inputCombination = last3Inputs.map { it.type }.joinToString("")
                wlCombinationToResult[inputCombination]?.let { result ->
                    updateTableStageFlow(mWlTableStateFlow, result, isBetHistory(last3Inputs))
                }
            } else {
                if (shouldCalculateCounter) {
                    updateWlCounter(mBetResultList[i])
                }
            }
        }
    }

    protected open fun isBetHistory(last3Inputs: List<BetEntity>): Boolean = (last3Inputs.last().isHistory || isHistory(last3Inputs.last().curTime))

    // 更新并保存数据
    fun updateOpendList(newList: List<InputEntity>) {
        viewModelScope.launch {
            repository.saveOpendList(newList)
        }
    }

    fun updateInputText(text: String) {
        // 存储在用户点击 新牌、保持之前输入的内容
        viewModelScope.launch { repository.saveNoteText(text) }
        mInputTextStateFlow.value = text
    }

    private fun clearBppcStateFlow() {
        mBppcTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST
        mBppcCounterStateFlow.value = DEFAULT_BPCOUNTER

        mStrategy3WaysStateFlowList.forEach { it.value = DEFAULT_STRATEGY_3WAY }
        mStragetyGridStateFlow.forEach { it.value = DEFAULT_STRANTYGE_GRID }
        mPredictionStateFlowList.forEach { it.value = DEFAULT_PREDICTION }

        mUniqueBppcConbinationList.forEach { it.clear() }
    }

    fun newGame() {
        viewModelScope.launch {
            if (mGameId.isNotEmpty()) {
                gameSessionDao.deleteByGameId(mGameId)
            }

            // 自动生成 gameId 和 startTime
            val session = GameSessionEntity.create()
            gameSessionDao.insert(session)
            mGameId = session.gameId
            mIsOnlyShowNewGameStateFlow.value = mGameId.isEmpty()

            onSaveOrNewGame()
        }
    }

    fun save() {
        viewModelScope.launch {
            inputDataDao.insertAll(mOpenInputList)
            noteDataDao.insert(NoteEntity.create(mGameId, mInputTextStateFlow.value))

            val session = gameSessionDao.getActiveSession()
            if (session != null && session.gameId == mGameId) {
                session.endTime = System.currentTimeMillis()
                session.isActive = false
                gameSessionDao.update(session)
                mGameId = ""
                mIsOnlyShowNewGameStateFlow.value = true
            }
            onSaveOrNewGame()
        }
    }

    fun onSaveOrNewGame() {
        clearBppcStateFlow()
        mWlCounterStateFlow.value = DEFAULT_BPCOUNTER
        mWlTableStateFlow.value = DEFAULT_TABLE_DISPLAY_LIST

        mInputTextStateFlow.value = ""
        mOpenInputList.clear()
        m12CompareResultList.clear()
        m34CompareResultList.clear()
        viewModelScope.launch {
            repository.saveNoteText("")
            repository.saveOpendList(mOpenInputList)
            repository.clearCurWinLossCount()

            // 恢复最近的 65 手 WL 记录
            val allBets = betDataDao.loadHistory().map { it.copy().apply { isHistory = true } }
            mBetResultList.clear()
            mBetResultList.addAll(allBets)
            resumeBetedData(false)
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

        private val strategyMap = mapOf(
            Pair(true, true) to 1,
            Pair(false, false) to 2,
            Pair(true, false) to 3,
            Pair(false, true) to 4
        )

        val RELEVANCY_MAP = mapOf(ColumnType.A to ColumnType.B, ColumnType.B to ColumnType.C, ColumnType.C to ColumnType.A)
        const val KEY_GAME_ID = "key_game_id"
        const val KEY_START_TIME = "key_start_time"
        const val KEY_END_TIME = "key_end_time"
    }
}