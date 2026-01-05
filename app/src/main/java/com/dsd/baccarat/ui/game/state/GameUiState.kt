package com.dsd.baccarat.ui.game.state

import androidx.compose.runtime.Immutable
import com.dsd.baccarat.data.ColumnType
import com.dsd.baccarat.data.Counter
import com.dsd.baccarat.data.InputType
import com.dsd.baccarat.data.PredictedStrategy3WaysValue
import com.dsd.baccarat.data.Strategy3WaysData
import com.dsd.baccarat.data.StrategyGridInfo
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TimerStatus
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import com.dsd.baccarat.data.room.entity.InputEntity

/**
 * 游戏主屏幕的 UI 状态
 * 遵循 Google UDF 最佳实践：单一不可变状态类
 */
@Immutable
data class GameUiState(
    // ========== 计时器状态 ==========
    val timerState: TimerState = TimerState(),

    // ========== BP 计数器 ==========
    val bppcCounter: Counter = Counter(),

    // ========== WL 计数器 ==========
    val wlCounter: Counter = Counter(),

    // ========== BPPC 表格数据 ==========
    val bppcTableData: List<TableDisplayItem> = emptyList(),

    // ========== WL 表格数据 ==========
    val wlTableData: List<TableDisplayItem> = emptyList(),

    // ========== 策略 3 路数据 ==========
    val strategy3WaysList: List<Strategy3WaysData> = emptyList(),

    // ========== 策略网格数据 ==========
    val strategyGridList: List<StrategyGridInfo> = emptyList(),

    // ========== 预测数据 ==========
    val predictedList: List<PredictedStrategy3WaysValue> = emptyList(),

    // ========== 当前押注状态 ==========
    val currentBetInput: InputEntity? = null,

    // ========== 历史计数器 ==========
    val historyWCount: Int = 0,
    val historyLCount: Int = 0,

    // ========== 输入文本 ==========
    val inputText: String = "",

    // ========== 只显示新游戏标记 ==========
    val isOnlyShowNewGame: Boolean = false,

    // ========== 日期选择对话框状态 ==========
    val dateSelectionState: DateSelectionState = DateSelectionState(),

    // ========== 历史模式标记 ==========
    val isHistoryMode: Boolean = false,
    val historyStartTime: Long = 0L,
    val historyEndTime: Long = 0L
) {
    /**
     * 获取指定列的 3 路策略数据
     */
    fun getStrategy3Ways(columnType: ColumnType): Strategy3WaysData {
        return strategy3WaysList.getOrNull(columnType.value) ?: Strategy3WaysData()
    }

    /**
     * 获取指定列的网格策略数据
     */
    fun getStrategyGrid(columnType: ColumnType): StrategyGridInfo {
        return strategyGridList.getOrNull(columnType.value) ?: StrategyGridInfo()
    }

    /**
     * 获取指定列的预测数据
     */
    fun getPredicted(columnType: ColumnType): PredictedStrategy3WaysValue {
        return predictedList.getOrNull(columnType.value) ?: PredictedStrategy3WaysValue()
    }

    /**
     * 是否有押注输入
     */
    val hasBetInput: Boolean
        get() = currentBetInput != null

    /**
     * 当前押注类型
     */
    val currentBetType: InputType?
        get() = currentBetInput?.inputType

    /**
     * 是否启用押注 B
     */
    val isBetBEnabled: Boolean
        get() = !isOnlyShowNewGame && currentBetType != InputType.B

    /**
     * 是否启用押注 P
     */
    val isBetPEnabled: Boolean
        get() = !isOnlyShowNewGame && currentBetType != InputType.P

    /**
     * 是否启用输入操作（开 B/P、撤销等）
     */
    val isInputEnabled: Boolean
        get() = !isOnlyShowNewGame
}

/**
 * 计时器状态
 */
@Immutable
data class TimerState(
    val status: TimerStatus = TimerStatus.Idle,
    val elapsedSeconds: Int = 0,
    val showReminder: Boolean = false
) {
    val isRunning: Boolean
        get() = status == TimerStatus.Running

    val isPaused: Boolean
        get() = status == TimerStatus.Paused

    val isIdle: Boolean
        get() = status == TimerStatus.Idle

    val isFinished: Boolean
        get() = status == TimerStatus.Finished

    val minutes: Int
        get() = elapsedSeconds / 60

    val seconds: Int
        get() = elapsedSeconds % 60

    val canStart: Boolean
        get() = !isRunning

    val canPauseOrResume: Boolean
        get() = isRunning || isPaused

    val canStop: Boolean
        get() = isRunning || isPaused
}

/**
 * 日期选择对话框状态
 */
@Immutable
data class DateSelectionState(
    val isDialogVisible: Boolean = false,
    val availableDates: List<GameSessionEntity> = emptyList(),
    val selectedDate: GameSessionEntity? = null,
    val isLoading: Boolean = false
) {
    val hasDates: Boolean
        get() = availableDates.isNotEmpty()

    val hasSelectedDate: Boolean
        get() = selectedDate != null
}
