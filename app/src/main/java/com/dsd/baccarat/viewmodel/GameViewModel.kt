package com.dsd.baccarat.viewmodel

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
import com.dsd.baccarat.ui.game.state.DateSelectionState
import com.dsd.baccarat.ui.game.state.GameSideEffect
import com.dsd.baccarat.ui.game.state.GameUiEvent
import com.dsd.baccarat.ui.game.state.GameUiState
import com.dsd.baccarat.ui.game.state.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 游戏 ViewModel - 遵循单向数据流（UDF）模式
 *
 * 架构设计：
 * - 单一状态源：StateFlow<GameUiState>
 * - 事件处理：onEvent(GameUiEvent)
 * - 副作用：SharedFlow<GameSideEffect>
 * - 不可变状态：所有状态更新通过 copy() 创建新实例
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: TemporaryStorageRepository,
    private val betDataDao: BetDataDao,
    private val inputDataDao: InputDataDao,
    private val noteDataDao: NoteDataDao,
    private val gameSessionDao: GameSessionDao,
) : ViewModel() {

    // ==================== 状态管理 ====================

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ==================== 副作用流 ====================

    private val _sideEffect = MutableSharedFlow<GameSideEffect>()
    val sideEffect: SharedFlow<GameSideEffect> = _sideEffect.asSharedFlow()

    // ==================== 私有数据（用于业务逻辑） ====================

    private var mOpenInputList: MutableList<InputEntity> = mutableListOf()
    private var mBetResultList: MutableList<BetEntity> = mutableListOf()
    private var m12CompareResultList: MutableMap<ColumnType, MutableList<Boolean>> = mutableMapOf()
    private var m34CompareResultList: MutableMap<ColumnType, MutableList<Boolean>> = mutableMapOf()

    private var mUniqueBppcConbinationList = MutableList<MutableSet<String>>(MAX_COLUMN_COUNT) { mutableSetOf() }
    private var mStrategyGridStateMap: MutableMap<ColumnType, Boolean?> = HashMap(MAX_COLUMN_COUNT)

    private var mTimerJob: Job? = null
    private var mGameId = ""

    // ==================== 初始化 ====================

    init {
        setup()
    }

    private fun setup() {
        // 收集历史计数器
        viewModelScope.launch {
            repository.wHistoryCountFlow.collect { newCount ->
                _uiState.update { it.copy(historyWCount = newCount) }
            }
        }

        viewModelScope.launch {
            repository.lHistoryCountFlow.collect { newCount ->
                _uiState.update { it.copy(historyLCount = newCount) }
            }
        }

        // 恢复数据
        viewModelScope.launch {
            mOpenInputList.clear()
            inputDataDao.getInputsByGameId(mGameId)
            mOpenInputList.addAll(repository.getOpendList())
            resumeOpenedData()

            val wlCounter = repository.getWinLossCurCount()
            _uiState.update { it.copy(wlCounter = wlCounter) }

            val inputText = repository.getNoteText()
            _uiState.update { it.copy(inputText = inputText) }

            mGameId = gameSessionDao.getActiveSession()?.gameId ?: ""
            _uiState.update { it.copy(isOnlyShowNewGame = mGameId.isEmpty()) }

            val historyList = betDataDao.loadHistory().map { it.copy().apply { isHistory = true } }
            val curBetList = if (mGameId.isNotEmpty()) {
                betDataDao.loadDataWithGameId(mGameId)
            } else {
                emptyList()
            }
            recordBetDataOnStartup(historyList, curBetList)
        }
    }

    // ==================== 事件处理 ====================

    /**
     * 处理 UI 事件
     * 这是 ViewModel 的主要入口点，所有用户操作都通过此方法
     */
    fun onEvent(event: GameUiEvent) {
        when (event) {
            // 输入事件
            is GameUiEvent.OpenB -> handleOpenB()
            is GameUiEvent.OpenP -> handleOpenP()
            is GameUiEvent.RemoveLastOpen -> handleRemoveLastOpen()

            // 押注事件
            is GameUiEvent.BetB -> handleBetB()
            is GameUiEvent.BetP -> handleBetP()
            is GameUiEvent.RemoveLastBet -> handleRemoveLastBet()

            // 计时器事件
            is GameUiEvent.StartTimer -> handleStartTimer()
            is GameUiEvent.PauseOrResumeTimer -> handlePauseOrResumeTimer()
            is GameUiEvent.StopTimer -> handleStopTimer()
            is GameUiEvent.DismissReminder -> handleDismissReminder()

            // 游戏管理事件
            is GameUiEvent.NewGame -> handleNewGame()
            is GameUiEvent.SaveGame -> handleSaveGame()

            // 文本输入事件
            is GameUiEvent.UpdateInputText -> handleUpdateInputText(event.text)

            // 日期选择事件
            is GameUiEvent.ShowDateSelectDialog -> handleShowDateSelectDialog()
            is GameUiEvent.DismissDialog -> handleDismissDialog()
            is GameUiEvent.SelectDate -> handleSelectDate(event.date)
            is GameUiEvent.ConfirmDateSelection -> handleConfirmDateSelection()

            // 历史记录事件
            is GameUiEvent.LoadHistory -> handleLoadHistory(event.gameId, event.startTime)
        }
    }

    // ==================== 输入处理 ====================

    private fun handleOpenB() {
        mOpenInputList.add(InputEntity.createB(mGameId))
        updateOpendList(mOpenInputList)
        updateOpenData()
    }

    private fun handleOpenP() {
        mOpenInputList.add(InputEntity.createP(mGameId))
        updateOpendList(mOpenInputList)
        updateOpenData()
    }

    private fun handleRemoveLastOpen() {
        mOpenInputList.removeLastOrNull() ?: return
        updateOpendList(mOpenInputList)
        resumeOpenedData()
    }

    private fun updateOpenData() {
        updateAllPredictions()
        if (mOpenInputList.size >= 2) {
            val last2Inputs = mOpenInputList.takeLast(2)
            val firstInputType = last2Inputs[0].inputType
            val secondInputType = last2Inputs[1].inputType
            updateCompareResultList(firstInputType, secondInputType, mOpenInputList.size)
        }

        val lastInput = mOpenInputList.last()
        val last3Inputs = mOpenInputList.takeLast(MIX_CONBINATION_ITEM_COUNT)
        updateBppcAndStrantegy(lastInput, last3Inputs)
        updateWlTable()
    }

    // ==================== 押注处理 ====================

    private fun handleBetB() {
        _uiState.update { it.copy(currentBetInput = InputEntity.createB(mGameId)) }
    }

    private fun handleBetP() {
        _uiState.update { it.copy(currentBetInput = InputEntity.createP(mGameId)) }
    }

    private fun handleRemoveLastBet() {
        val currentState = _uiState.value
        if (currentState.currentBetInput != null) {
            _uiState.update { it.copy(currentBetInput = null) }
        } else {
            // 历史数据不能撤销
            if (mBetResultList.lastOrNull()?.isHistory == true) return

            val last = mBetResultList.removeLastOrNull()
            last ?: return
            viewModelScope.launch {
                repository.updateWlCount(last, OperationType.DECREMENT)
                betDataDao.deleteByTime(last.curTime)
            }
        }

        resumeBetedData()
    }

    // ==================== 计时器处理 ====================

    private fun handleStartTimer() {
        _uiState.update {
            it.copy(
                timerState = TimerState(
                    status = TimerStatus.Running,
                    elapsedSeconds = 0,
                    showReminder = false
                )
            )
        }
        createAndStartNewTimerJob()
    }

    private fun handlePauseOrResumeTimer() {
        val currentStatus = _uiState.value.timerState.status
        if (currentStatus == TimerStatus.Running) {
            _uiState.update {
                it.copy(timerState = it.timerState.copy(status = TimerStatus.Paused))
            }
            mTimerJob?.cancel()
            mTimerJob = null
        } else if (currentStatus == TimerStatus.Paused) {
            _uiState.update {
                it.copy(timerState = it.timerState.copy(status = TimerStatus.Running))
            }
            createAndStartNewTimerJob()
        }
    }

    private fun handleStopTimer() {
        _uiState.update {
            it.copy(timerState = TimerState())
        }
        mTimerJob?.cancel()
        mTimerJob = null
    }

    private fun handleDismissReminder() {
        _uiState.update {
            it.copy(timerState = it.timerState.copy(showReminder = false))
        }
    }

    private fun createAndStartNewTimerJob() {
        mTimerJob = viewModelScope.launch {
            while (_uiState.value.timerState.status == TimerStatus.Running &&
                _uiState.value.timerState.elapsedSeconds < MAX_SECONDS
            ) {
                delay(1000)
                _uiState.update {
                    it.copy(
                        timerState = it.timerState.copy(
                            elapsedSeconds = it.timerState.elapsedSeconds + 1
                        )
                    )
                }
            }
            if (_uiState.value.timerState.elapsedSeconds >= MAX_SECONDS) {
                _uiState.update {
                    it.copy(
                        timerState = it.timerState.copy(
                            status = TimerStatus.Finished,
                            showReminder = true
                        )
                    )
                }
                _sideEffect.emit(GameSideEffect.PlaySound)
            }
        }
    }

    // ==================== 游戏管理处理 ====================

    private fun handleNewGame() {
        viewModelScope.launch {
            if (mGameId.isNotEmpty()) {
                gameSessionDao.deleteByGameId(mGameId)
            }

            val session = GameSessionEntity.create()
            gameSessionDao.insert(session)
            mGameId = session.gameId
            _uiState.update { it.copy(isOnlyShowNewGame = mGameId.isEmpty()) }

            onSaveOrNewGame()
        }
    }

    private fun handleSaveGame() {
        viewModelScope.launch {
            inputDataDao.insertAll(mOpenInputList)
            noteDataDao.insert(NoteEntity.create(mGameId, _uiState.value.inputText))

            val session = gameSessionDao.getActiveSession()
            if (session != null && session.gameId == mGameId) {
                session.endTime = System.currentTimeMillis()
                session.isActive = false
                gameSessionDao.update(session)
                mGameId = ""
                _uiState.update { it.copy(isOnlyShowNewGame = true) }
            }
            onSaveOrNewGame()
        }
    }

    // ==================== 文本输入处理 ====================

    private fun handleUpdateInputText(text: String) {
        viewModelScope.launch {
            repository.saveNoteText(text)
        }
        _uiState.update { it.copy(inputText = text) }
    }

    // ==================== 日期选择处理 ====================

    private fun handleShowDateSelectDialog() {
        _uiState.update {
            it.copy(dateSelectionState = it.dateSelectionState.copy(isLoading = true))
        }

        viewModelScope.launch {
            try {
                val allHistorySessions = gameSessionDao.getAllHistorySessions().first()
                _uiState.update {
                    it.copy(
                        dateSelectionState = it.dateSelectionState.copy(
                            isLoading = false,
                            availableDates = allHistorySessions,
                            selectedDate = allHistorySessions.firstOrNull(),
                            isDialogVisible = true
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        dateSelectionState = it.dateSelectionState.copy(
                            isLoading = false,
                            availableDates = emptyList(),
                            isDialogVisible = true
                        )
                    )
                }
            }
        }
    }

    private fun handleDismissDialog() {
        _uiState.update {
            it.copy(
                dateSelectionState = it.dateSelectionState.copy(
                    isDialogVisible = false
                )
            )
        }
    }

    private fun handleSelectDate(date: GameSessionEntity) {
        _uiState.update {
            it.copy(
                dateSelectionState = it.dateSelectionState.copy(
                    selectedDate = date
                )
            )
        }
    }

    private fun handleConfirmDateSelection() {
        val selectedDate = _uiState.value.dateSelectionState.selectedDate
        if (selectedDate == null) {
            handleDismissDialog()
            return
        }

        _sideEffect.tryEmit(
            GameSideEffect.NavigateToHistory(
                gameId = selectedDate.gameId,
                startTime = selectedDate.startTime,
                endTime = selectedDate.endTime
            )
        )
        handleDismissDialog()
    }

    /**
     * 处理加载历史记录
     * 从数据库加载指定游戏的数据并进入历史模式
     */
    private fun handleLoadHistory(gameId: String, startTime: Long) {
        viewModelScope.launch {
            try {
                // 设置为历史模式
                _uiState.update {
                    it.copy(
                        isHistoryMode = true,
                        historyStartTime = startTime,
                        historyEndTime = System.currentTimeMillis()
                    )
                }

                // 收集历史计数数据
                repository.wHistoryCountFlow.collect { newCount ->
                    _uiState.update { state ->
                        state.copy(historyWCount = newCount)
                    }
                }

                repository.lHistoryCountFlow.collect { newCount ->
                    _uiState.update { state ->
                        state.copy(historyLCount = newCount)
                    }
                }

                // 加载游戏数据
                val noteText = noteDataDao.getNoteByGameId(gameId).joinToString { it.content }
                _uiState.update { it.copy(inputText = noteText) }

                val allInputs = inputDataDao.getInputsByGameId(gameId)
                mOpenInputList.clear()
                mOpenInputList.addAll(allInputs)
                updateOpendList(mOpenInputList)
                resumeOpenedData()

                val historyList = betDataDao.queryHistoryBefore(startTime)
                    .map { it.copy().apply { isHistory = true } }
                val curBetList = betDataDao.loadDataWithGameId(gameId)

                recordBetDataOnStartup(historyList, curBetList)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to load history", e)
            }
        }
    }

    // ==================== 业务逻辑 ====================

    private fun updateCompareResultList(
        firstInputType: InputType,
        secondInputType: InputType,
        inputListSize: Int
    ) {
        if (inputListSize < 2) return

        val eq = (firstInputType == secondInputType)
        val neq = !eq

        if (inputListSize == 2) {
            m12CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(eq)
            m34CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(neq)
            return
        }

        when (inputListSize % 3) {
            0 -> {
                m12CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(eq)
                m12CompareResultList.getOrPut(ColumnType.B) { mutableListOf() }.add(eq)
                m34CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(eq)
                m34CompareResultList.getOrPut(ColumnType.B) { mutableListOf() }.add(neq)
            }

            1 -> {
                m12CompareResultList.getOrPut(ColumnType.B) { mutableListOf() }.add(eq)
                m12CompareResultList.getOrPut(ColumnType.C) { mutableListOf() }.add(eq)
                m34CompareResultList.getOrPut(ColumnType.B) { mutableListOf() }.add(eq)
                m34CompareResultList.getOrPut(ColumnType.C) { mutableListOf() }.add(neq)
            }

            2 -> {
                m12CompareResultList.getOrPut(ColumnType.A) { mutableListOf() }.add(eq)
                m12CompareResultList.getOrPut(ColumnType.C) { mutableListOf() }.add(eq)
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

    private fun updateBppcCounter(lastInput: InputEntity) {
        _uiState.update { state ->
            state.copy(
                bppcCounter = when (lastInput.inputType) {
                    InputType.B -> state.bppcCounter.copy(count1 = state.bppcCounter.count1 + 1)
                    InputType.P -> state.bppcCounter.copy(count2 = state.bppcCounter.count2 + 1)
                }
            )
        }
    }

    private fun updateBppcTable(last3Inputs: List<InputEntity>): ColumnType? {
        val inputCombination = last3Inputs.map { it.inputType }.joinToString("")
        val result = bppcCombinationToResult[inputCombination] ?: return null
        val columnType = updateTableStageFlow(
            currentState = _uiState.value.bppcTableData,
            result = result,
            isHistory = isHistory(last3Inputs.last().curTime),
            onUpdate = { newData ->
                _uiState.update { it.copy(bppcTableData = newData) }
            }
        )
        return columnType
    }

    private fun updateAllPredictions() {
        if (mOpenInputList.isEmpty()) return

        val size = mOpenInputList.size
        val last = mOpenInputList.last().inputType

        if (size == 1) {
            val strategy12 = last
            val strategy34 = last.inverted()
            val newPredictedList = _uiState.value.predictedList.toMutableList()
            newPredictedList[ColumnType.A.value] = PredictedStrategy3WaysValue(
                "",
                strategy12.value,
                strategy34.value,
                strategy12.inverted().value,
                strategy34.inverted().value
            )
            _uiState.update { it.copy(predictedList = newPredictedList) }
            return
        }

        val pattern = listOf(
            listOf(ColumnType.A to true, ColumnType.B to false),
            listOf(ColumnType.B to true, ColumnType.C to false),
            listOf(ColumnType.A to false, ColumnType.C to true)
        )

        val indexInPattern = (size - 2) % 3

        val columnsToUpdate = pattern[indexInPattern].map { (col, isFirstLast) ->
            val strategy34 = if (isFirstLast) last else last.inverted()
            col to strategy34
        }

        val newPredictedList = MutableList(MAX_COLUMN_COUNT) { DEFAULT_PREDICTION }

        columnsToUpdate.forEach { (col, strategy34) ->
            newPredictedList[col.value] = PredictedStrategy3WaysValue(
                "",
                last.value,
                strategy34.value,
                last.inverted().value,
                strategy34.inverted().value
            )
        }
        _uiState.update { it.copy(predictedList = newPredictedList) }
    }

    private fun updateWlTable() {
        val inputType = _uiState.value.currentBetInput ?: return
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
            _uiState.update { it.copy(currentBetInput = null) }
            return
        }

        val inputCombination = last3Inputs.map { it.type }.joinToString("")
        val result = wlCombinationToResult[inputCombination] ?: run {
            _uiState.update { it.copy(currentBetInput = null) }
            return
        }

        Log.d("GameViewModel", "Current Inputs: $last3Inputs")
        val lastResult = mBetResultList.last()
        updateWlCounter(lastResult)
        viewModelScope.launch { repository.updateWlCount(lastResult, OperationType.INCREMENT) }

        updateTableStageFlow(
            currentState = _uiState.value.wlTableData,
            result = result,
            isHistory = isBetHistory(last3Inputs),
            onUpdate = { newData ->
                _uiState.update { it.copy(wlTableData = newData) }
            }
        )
        _uiState.update { it.copy(currentBetInput = null) }
    }

    private fun updateTableStageFlow(
        currentState: List<TableDisplayItem>,
        result: Int,
        isHistory: Boolean,
        onUpdate: (List<TableDisplayItem>) -> Unit
    ): ColumnType? {
        var filledColumn: ColumnType? = null
        val data = Pair(isHistory, result)

        onUpdate(currentState.toMutableList().apply {
            val lastRealIndex = indexOfLast { it is TableDisplayItem.Real }

            if (lastRealIndex == -1) {
                this[0] = TableDisplayItem.Real(TableItem(dataA = data))
                filledColumn = ColumnType.A
            } else {
                val lastRealItem = this[lastRealIndex] as TableDisplayItem.Real
                val currentData = lastRealItem.data

                when {
                    currentData.dataA == null -> {
                        this[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataA = data))
                        filledColumn = ColumnType.A
                    }

                    currentData.dataB == null -> {
                        this[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataB = data))
                        filledColumn = ColumnType.B
                    }

                    currentData.dataC == null -> {
                        this[lastRealIndex] = lastRealItem.copy(data = currentData.copy(dataC = data))
                        filledColumn = ColumnType.C
                    }

                    else -> {
                        val insertIndex = lastRealIndex + 1
                        this.add(insertIndex, TableDisplayItem.Real(TableItem(dataA = data)))
                        filledColumn = ColumnType.A
                        if (size > MIN_TABLE_COLUMN_COUNT && last() is TableDisplayItem.Empty) {
                            removeLastOrNull()
                        }
                    }
                }
            }
        })
        return filledColumn
    }

    private fun isHistory(time: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr1 = sdf.format(System.currentTimeMillis())
        val dateStr2 = sdf.format(time)
        return dateStr1 != dateStr2
    }

    private fun updateWlCounter(lastResult: BetEntity) {
        _uiState.update { state ->
            state.copy(
                wlCounter = when (lastResult.type) {
                    BetResultType.W -> state.wlCounter.copy(count1 = state.wlCounter.count1 + 1)
                    BetResultType.L -> state.wlCounter.copy(count2 = state.wlCounter.count2 + 1)
                }
            )
        }
    }

    private fun update3WayStrategy(filledColumn: ColumnType) {
        updateColumnStrategy(column = filledColumn)
        val relatedColumn = RELEVANCY_MAP[filledColumn] ?: return
        updateColumnStrategy(column = relatedColumn)
    }

    private fun updateColumnStrategy(column: ColumnType) {
        val rawCompare12 = m12CompareResultList[column]?.takeLast(2) ?: emptyList()
        val rawCompare34 = m34CompareResultList[column]?.takeLast(2) ?: emptyList()
        val p12 = rawCompare12.pairFirstTwoOrNull()
        val p34 = rawCompare34.pairFirstTwoOrNull()

        val currentList = _uiState.value.strategy3WaysList.toMutableList()
        currentList[column.value] = currentList[column.value].copy(
            strategy12 = updateSingleStrategyListFor3Ways(currentList[column.value].strategy12, p12),
            strategy34 = updateSingleStrategyListFor3Ways(currentList[column.value].strategy34, p34),
            strategy56 = updateSingleStrategyListFor3Ways(currentList[column.value].strategy56, p12.inverted()),
            strategy78 = updateSingleStrategyListFor3Ways(currentList[column.value].strategy78, p34.inverted()),
        )
        _uiState.update { it.copy(strategy3WaysList = currentList) }
    }

    private fun List<Boolean>.pairFirstTwoOrNull(): Pair<Boolean, Boolean>? {
        return if (size >= 2) Pair(this[0], this[1]) else null
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

    private fun updateGridStrategy(last3Inputs: List<InputEntity>, filledColumn: ColumnType) {
        val currentConbinations = mUniqueBppcConbinationList[filledColumn.value]
        val inputCombination = last3Inputs.map { it.inputType }.joinToString("")
        currentConbinations.add(inputCombination)

        mStrategyGridStateMap.forEach {
            if (it.value == true) {
                updateGridStrategyData(last3Inputs.last(), it.key)
            }
        }

        if (currentConbinations.size == THRESHOLD_HAS_SHOWED_CONBINATION) {
            createGridStrategyData(currentConbinations, filledColumn)
            updateGridStrategyData(null, filledColumn)
        }
    }

    private fun createGridStrategyData(currentConbinations: MutableSet<String>, filledColumn: ColumnType) {
        val missingKeys: List<String> = (allBppcConbinations - currentConbinations).sortedBy { key ->
            bppcCombinationToResult[key]
        }
        val missingWithAntonyms: List<String> = missingKeys.map { key ->
            antonymBppcCombination[key]!!
        }

        val itemList = missingWithAntonyms.map { word ->
            val title = bppcCombinationToResult[word].toString()
            val wordList = word.map { it.toString() }
            StrategyGridItem(false, title, wordList)
        }

        mStrategyGridStateMap[filledColumn] = true
        val currentList = _uiState.value.strategyGridList.toMutableList()
        currentList[filledColumn.value] = StrategyGridInfo(
            predictedList = emptyList(),
            actualOpenedList = emptyList(),
            itemList = itemList
        )
        _uiState.update { it.copy(strategyGridList = currentList) }
    }

    private fun updateGridStrategyData(inputData: InputEntity?, filledColumn: ColumnType) {
        val currentData = _uiState.value.strategyGridList[filledColumn.value]
        var result = currentData.copy()

        val actualOpenedList = result.actualOpenedList.toMutableList().apply {
            if (size >= MAX_COLUMN_COUNT) clear()
            if (inputData != null) {
                this.add(inputData.inputType.toString())
            }
        }

        val predictedList = result.predictedList.toMutableList().apply {
            if (size >= MAX_COLUMN_COUNT) clear()
        }

        if (actualOpenedList.size >= MAX_COLUMN_COUNT) {
            val openedSize = actualOpenedList.size
            val itemList = result.itemList.toMutableList().map { item ->
                val isAllItemsMismatch = item.items.withIndex().none { (index, value) ->
                    index < openedSize && value == actualOpenedList[index]
                }

                if (isAllItemsMismatch) {
                    item.copy(isObsolete = true)
                } else {
                    item
                }
            }
            actualOpenedList.clear()
            result = result.copy(predictedList = predictedList, actualOpenedList = actualOpenedList, itemList = itemList)
        }

        if (result.itemList.none { !it.isObsolete }) {
            val currentList = _uiState.value.strategyGridList.toMutableList()
            currentList[filledColumn.value] = result
            _uiState.update { it.copy(strategyGridList = currentList) }
            return
        }

        val currentIndex = actualOpenedList.size
        val aRowItemList = if (actualOpenedList.isEmpty()) {
            result.itemList.filterNot { it.isObsolete }.map { it.items[currentIndex] }
        } else {
            result.itemList
                .filterNot { it.isObsolete }
                .filterNot { item ->
                    item.items.withIndex().any { (index, value) ->
                        index < actualOpenedList.size && value == actualOpenedList[index]
                    }
                }
                .map { it.items[currentIndex] }
                .ifEmpty { emptyList() }
        }

        predictedList.add(if (aRowItemList.distinct().size == 1) aRowItemList.first() else "-")
        result = result.copy(
            predictedList = predictedList,
            actualOpenedList = actualOpenedList,
        )

        val currentList = _uiState.value.strategyGridList.toMutableList()
        currentList[filledColumn.value] = result
        _uiState.update { it.copy(strategyGridList = currentList) }
    }

    private fun resumeOpenedData() {
        clearBppcStateFlow()
        Log.d("###", "resumeOpenedData ${mOpenInputList.map { it.inputType }}")
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

    private fun resumeBetedData(shouldCalculateCounter: Boolean = true) {
        _uiState.update {
            it.copy(
                wlTableData = DEFAULT_TABLE_DISPLAY_LIST,
                wlCounter = DEFAULT_BPCOUNTER
            )
        }

        for (i in mBetResultList.indices) {
            if (i >= 2) {
                val last3Inputs = mBetResultList.subList(0, i + 1).takeLast(3)
                if (shouldCalculateCounter) {
                    updateWlCounter(mBetResultList[i])
                }
                val inputCombination = last3Inputs.map { it.type }.joinToString("")
                wlCombinationToResult[inputCombination]?.let { result ->
                    updateTableStageFlow(
                        currentState = _uiState.value.wlTableData,
                        result = result,
                        isHistory = isBetHistory(last3Inputs),
                        onUpdate = { newData ->
                            _uiState.update { it.copy(wlTableData = newData) }
                        }
                    )
                }
            } else {
                if (shouldCalculateCounter) {
                    updateWlCounter(mBetResultList[i])
                }
            }
        }
    }

    private fun isBetHistory(last3Inputs: List<BetEntity>): Boolean =
        (last3Inputs.last().isHistory || isHistory(last3Inputs.last().curTime))

    private fun updateOpendList(newList: List<InputEntity>) {
        viewModelScope.launch {
            repository.saveOpendList(newList)
        }
    }

    private fun clearBppcStateFlow() {
        _uiState.update {
            it.copy(
                bppcTableData = DEFAULT_TABLE_DISPLAY_LIST,
                bppcCounter = DEFAULT_BPCOUNTER,
                strategy3WaysList = List(MAX_COLUMN_COUNT) { DEFAULT_STRATEGY_3WAY },
                strategyGridList = List(MAX_COLUMN_COUNT) { DEFAULT_STRANTYGE_GRID },
                predictedList = List(MAX_COLUMN_COUNT) { DEFAULT_PREDICTION }
            )
        }
        mUniqueBppcConbinationList.forEach { it.clear() }
    }

    private fun recordBetDataOnStartup(historyList: List<BetEntity>, curBetList: List<BetEntity>) {
        mBetResultList.clear()
        mBetResultList.addAll(historyList)
        mBetResultList.addAll(curBetList)
        resumeBetedData(false)
        curBetList.forEach { updateWlCounter(it) }
    }

    private fun onSaveOrNewGame() {
        clearBppcStateFlow()
        _uiState.update {
            it.copy(
                wlCounter = DEFAULT_BPCOUNTER,
                wlTableData = DEFAULT_TABLE_DISPLAY_LIST,
                inputText = ""
            )
        }

        mOpenInputList.clear()
        m12CompareResultList.clear()
        m34CompareResultList.clear()
        viewModelScope.launch {
            repository.saveNoteText("")
            repository.saveOpendList(mOpenInputList)
            repository.clearCurWinLossCount()

            val allBets = betDataDao.loadHistory().map { it.copy().apply { isHistory = true } }
            mBetResultList.clear()
            mBetResultList.addAll(allBets)
            resumeBetedData(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mTimerJob?.cancel()
        mTimerJob = null
    }

    // ==================== 常量 ====================

    private companion object {
        const val THRESHOLD_HAS_SHOWED_CONBINATION = 5
        val DEFAULT_PREDICTION = PredictedStrategy3WaysValue()
        val DEFAULT_STRATEGY_3WAY = Strategy3WaysData()
        val DEFAULT_BPCOUNTER = Counter()
        val DEFAULT_TABLE_DISPLAY_LIST = List(MIN_TABLE_COLUMN_COUNT) { TableDisplayItem.Empty }
        private val DEFAULT_STRANTYGE_GRID = StrategyGridInfo()

        const val MAX_COLUMN_COUNT = 3
        const val MIN_TABLE_COLUMN_COUNT = 30
        private const val MAX_SECONDS = 45 * 60
        private const val MIX_CONBINATION_ITEM_COUNT = 3

        private val bppcCombinationToResult = mapOf(
            "BBB" to 1, "PPP" to 2, "BPP" to 3, "PBB" to 4,
            "PBP" to 5, "BPB" to 6, "PPB" to 7, "BBP" to 8
        )

        val antonymBppcCombination = mapOf(
            "BBB" to "PPP", "PPP" to "BBB",
            "BPP" to "PBB", "PBB" to "BPP",
            "PBP" to "BPB", "BPB" to "PBP",
            "PPB" to "BBP", "BBP" to "PPB"
        )

        val allBppcConbinations = bppcCombinationToResult.keys.toSet()

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

        val RELEVANCY_MAP = mapOf(
            ColumnType.A to ColumnType.B,
            ColumnType.B to ColumnType.C,
            ColumnType.C to ColumnType.A
        )
    }
}
