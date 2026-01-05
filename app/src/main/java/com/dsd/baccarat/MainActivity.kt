package com.dsd.baccarat

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.dsd.baccarat.ui.game.GameScreen
import com.dsd.baccarat.viewmodel.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity - 游戏主界面
 *
 * 使用新的 UDF (Unidirectional Data Flow) 架构
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    // 使用新的 UDF 架构的 ViewModel
    private val viewModel: GameViewModel by viewModels()


    @Composable
    override fun Content() {
        // 使用新的 GameScreen（UDF 架构）
        GameScreen(viewModel = viewModel)
    }
}
