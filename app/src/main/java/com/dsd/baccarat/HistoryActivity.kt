package com.dsd.baccarat

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.dsd.baccarat.ui.game.GameScreen
import com.dsd.baccarat.ui.game.state.GameUiEvent
import com.dsd.baccarat.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 历史记录 Activity
 *
 * 使用统一的 GameViewModel 和 GameScreen 架构
 * Create by Shengda 2025/11/4 20:05
 */
@AndroidEntryPoint
class HistoryActivity : BaseActivity() {

    companion object {
        const val KEY_GAME_ID = "key_game_id"
        const val KEY_START_TIME = "key_start_time"
        const val KEY_END_TIME = "key_end_time"
    }

    private val viewModel: GameViewModel by viewModels()

    @Composable
    override fun Content() {
        // 从 Intent 中获取参数
        val gameID = intent?.getStringExtra(KEY_GAME_ID) ?: ""
        val startTime = intent?.getLongExtra(KEY_START_TIME, 0) ?: 0

        // 只在首次加载时触发历史数据加载
        LaunchedEffect(Unit) {
            viewModel.onEvent(GameUiEvent.LoadHistory(gameId = gameID, startTime = startTime))
        }

        // 使用统一的 GameScreen
        GameScreen(viewModel = viewModel)
    }
}
