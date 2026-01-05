package com.dsd.baccarat.ui.game

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.ui.game.components.layout.LeftSideSection
import com.dsd.baccarat.ui.game.components.layout.RightSideSection
import com.dsd.baccarat.ui.game.components.common.NotificationSoundEffect
import com.dsd.baccarat.viewmodel.GameViewModel

// 滚动位置计算乘数
// 用于将索引转换为大致的像素位置，以便比较滚动位置
private const val SCROLL_POSITION_MULTIPLIER = 1000f

/**
 * 游戏主屏幕 - 遵循 Google UDF 最佳实践
 *
 * 架构特点：
 * - 单一状态源：StateFlow<GameUiState>
 * - 事件驱动：所有用户操作通过 GameUiEvent 传递
 * - 组件化：UI 拆分为可复用的小组件
 * - 无副作用：副作用通过 SharedFlow 单独处理
 *
 * @param viewModel 游戏 ViewModel
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel
) {
    // 收集 UI 状态
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // 同步滚动状态管理
    val sharedScrollStates = remember { mutableStateListOf<LazyListState>() }

    // 主滚动同步逻辑
    LaunchedEffect(sharedScrollStates) {
        snapshotFlow {
            sharedScrollStates.firstOrNull()?.let {
                it.firstVisibleItemScrollOffset + it.firstVisibleItemIndex * SCROLL_POSITION_MULTIPLIER
            }
        }.collect {
            val main = sharedScrollStates.firstOrNull() ?: return@collect
            val index = main.firstVisibleItemIndex
            val offset = main.firstVisibleItemScrollOffset
            sharedScrollStates.drop(1).forEach { state ->
                if (state.firstVisibleItemIndex != index || state.firstVisibleItemScrollOffset != offset) {
                    state.scrollToItem(index, offset)
                }
            }
        }
    }

    // 提供滚动状态的函数
    val provideListState = @Composable {
        val state = rememberLazyListState()
        LaunchedEffect(Unit) { sharedScrollStates.add(state) }
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose { sharedScrollStates.remove(state) }
        }
        state
    }

    // 处理副作用（声音播放等）
    NotificationSoundEffect(soundEffectFlow = viewModel.sideEffect)

    Scaffold { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 左侧区域
            LeftSideSection(
                uiState = uiState,
                onEvent = { event -> viewModel.onEvent(event) },
                tableListStateProvider = provideListState,
                modifier = Modifier.weight(1f)
            )

            // 右侧区域
            RightSideSection(
                uiState = uiState,
                onEvent = { event -> viewModel.onEvent(event) },
                sideEffectFlow = viewModel.sideEffect,
                context = context,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
