package com.dsd.baccarat.ui.game.state

import com.dsd.baccarat.data.TableType
import com.dsd.baccarat.data.room.entity.GameSessionEntity

/**
 * 游戏主屏幕的 UI 事件
 * 遵循 Google UDF 最佳实践：使用密封接口表示所有用户操作
 */
sealed interface GameUiEvent {
    // ==================== 输入事件 ====================

    /**
     * 开 B（庄）
     */
    data object OpenB : GameUiEvent

    /**
     * 开 P（闲）
     */
    data object OpenP : GameUiEvent

    /**
     * 撤销最后一个开
     */
    data object RemoveLastOpen : GameUiEvent

    // ==================== 押注事件 ====================

    /**
     * 押 B（庄）
     */
    data object BetB : GameUiEvent

    /**
     * 押 P（闲）
     */
    data object BetP : GameUiEvent

    /**
     * 撤销最后一个押注
     */
    data object RemoveLastBet : GameUiEvent

    // ==================== 计时器事件 ====================

    /**
     * 开始计时
     */
    data object StartTimer : GameUiEvent

    /**
     * 暂停或继续计时
     */
    data object PauseOrResumeTimer : GameUiEvent

    /**
     * 停止计时
     */
    data object StopTimer : GameUiEvent

    /**
     * 关闭休息提醒
     */
    data object DismissReminder : GameUiEvent

    // ==================== 游戏管理事件 ====================

    /**
     * 新游戏
     */
    data object NewGame : GameUiEvent

    /**
     * 保存游戏
     */
    data object SaveGame : GameUiEvent

    // ==================== 文本输入事件 ====================

    /**
     * 更新输入文本
     */
    data class UpdateInputText(val text: String) : GameUiEvent

    // ==================== 日期选择事件 ====================

    /**
     * 显示日期选择对话框
     */
    data object ShowDateSelectDialog : GameUiEvent

    /**
     * 关闭日期选择对话框
     */
    data object DismissDialog : GameUiEvent

    /**
     * 选择日期
     */
    data class SelectDate(val date: GameSessionEntity) : GameUiEvent

    /**
     * 确认日期选择
     */
    data object ConfirmDateSelection : GameUiEvent

    // ==================== 历史记录事件 ====================

    /**
     * 加载历史记录
     * @param gameId 游戏 ID
     * @param startTime 开始时间
     */
    data class LoadHistory(val gameId: String, val startTime: Long) : GameUiEvent

    // ========== V2: 套圈事件 ==========

    /**
     * 切换正反套圈（相邻）
     * @param tableType 表格类型（BP 或 WL）
     */
    data class ToggleCircleZF(val tableType: TableType) : GameUiEvent

    /**
     * 切换正反套圈（间隔）
     * @param tableType 表格类型（BP 或 WL）
     */
    data class ToggleCircleZFSep(val tableType: TableType) : GameUiEvent
}
