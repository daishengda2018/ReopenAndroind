package com.dsd.baccarat.ui.game.state

import android.content.Context

/**
 * 游戏主屏幕的副作用（一次性事件）
 * 这些事件需要由 UI 层处理，例如：导航、Toast、声音播放等
 *
 * 注意：副作用不应该改变 UI State，而是触发一次性操作
 */
sealed interface GameSideEffect {
    /**
     * 播放提示音
     */
    data object PlaySound : GameSideEffect

    /**
     * 显示 Toast 消息
     */
    data class ShowToast(val message: String) : GameSideEffect

    /**
     * 导航到历史记录界面
     * @param gameId 游戏 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param context Android Context（用于启动 Activity）
     */
    data class NavigateToHistory(
        val gameId: String,
        val startTime: Long,
        val endTime: Long
    ) : GameSideEffect

    /**
     * 显示错误消息
     */
    data class ShowError(val error: Throwable) : GameSideEffect
}
