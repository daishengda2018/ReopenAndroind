package com.dsd.baccarat.ui.game.components.layout

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.HistoryActivity
import com.dsd.baccarat.data.ColumnType
import com.dsd.baccarat.data.PredictedStrategy3WaysValue
import com.dsd.baccarat.data.Strategy3WaysData
import com.dsd.baccarat.ui.game.components.common.CounterDisplay
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.SPACE_SIZE
import com.dsd.baccarat.ui.game.components.common.TABLE_HEIGHT
import com.dsd.baccarat.ui.game.components.common.TEXT_COLOR_LOSE
import com.dsd.baccarat.ui.game.components.common.TEXT_COLOR_WIN
import com.dsd.baccarat.ui.game.components.input.DateSelectDialog
import com.dsd.baccarat.ui.game.components.input.InputButtons
import com.dsd.baccarat.ui.game.components.strategy.Strategy3WaysDisplay
import com.dsd.baccarat.ui.game.components.table.GameTable
import com.dsd.baccarat.ui.game.state.GameSideEffect
import com.dsd.baccarat.ui.game.state.GameUiEvent
import com.dsd.baccarat.ui.game.state.GameUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * 右侧策略数据的缓存容器，避免重复计算
 */
@Immutable
private data class RightSideStrategyData(
    val predicted: PredictedStrategy3WaysValue,
    val strategy3Ways: Strategy3WaysData
)

/**
 * 右侧区域组件
 *
 * @param uiState UI 状态
 * @param onEvent 事件处理回调
 * @param sideEffectFlow 副作用流
 * @param context Android Context
 * @param modifier 修饰符
 */
@Composable
fun RightSideSection(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit,
    sideEffectFlow: SharedFlow<GameSideEffect>,
    context: Context,
    modifier: Modifier = Modifier
) {
    // 缓存频繁调用的 getter 方法结果，避免重复计算
    val strategyA = remember(uiState.strategy3WaysList, uiState.predictedList) {
        RightSideStrategyData(
            predicted = uiState.getPredicted(ColumnType.A),
            strategy3Ways = uiState.getStrategy3Ways(ColumnType.A)
        )
    }
    val strategyB = remember(uiState.strategy3WaysList, uiState.predictedList) {
        RightSideStrategyData(
            predicted = uiState.getPredicted(ColumnType.B),
            strategy3Ways = uiState.getStrategy3Ways(ColumnType.B)
        )
    }
    val strategyC = remember(uiState.strategy3WaysList, uiState.predictedList) {
        RightSideStrategyData(
            predicted = uiState.getPredicted(ColumnType.C),
            strategy3Ways = uiState.getStrategy3Ways(ColumnType.C)
        )
    }

    Column(
        modifier
            .padding(horizontal = SPACE_SIZE)
    ) {
        Row {
            CounterDisplay(
                value1 = uiState.wlCounter.count1,
                color1 = TEXT_COLOR_WIN,
                value2 = uiState.wlCounter.count2,
                color2 = TEXT_COLOR_LOSE,
                isShowWsr = true,
                isHistory = false
            )
            Spacer(Modifier.width(ITEM_SIZE))
            CounterDisplay(
                value1 = uiState.historyWCount,
                color1 = TEXT_COLOR_WIN,
                value2 = uiState.historyLCount,
                color2 = TEXT_COLOR_LOSE,
                isShowWsr = true,
                isHistory = true
            )
        }

        // WL 表格
        val listState = rememberLazyListState()
        HandleAutoScroll(
            tableData = uiState.wlTableData,
            listState = listState,
            isHistoryMode = uiState.isHistoryMode
        )

        GameTable(
            items = uiState.wlTableData,
            listState = listState,
            showCharts = false,
            showHistory = true
        )

        // 输入文本框
        val dateStr = remember {
            if (uiState.isHistoryMode) {
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd EEEE",
                    java.util.Locale.CHINESE
                ).format(uiState.historyStartTime)
            } else {
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd EEEE",
                    java.util.Locale.CHINESE
                ).format(System.currentTimeMillis())
            }
        }

        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = { newText -> onEvent(GameUiEvent.UpdateInputText(newText)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(TABLE_HEIGHT * 3 + SPACE_SIZE * 3),
            label = { Text(dateStr) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp)
        )

        // A C 策略 (34, 78)
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.C.name, "34", "78"),
            mainTitleIndex = 0,
            titleStr = strategyA.predicted.titleStr,
            predictedValue1 = strategyA.predicted.strategy34,
            predictedValue2 = strategyA.predicted.strategy78,
            displayItems1 = strategyA.strategy3Ways.strategy34,
            displayItems2 = strategyA.strategy3Ways.strategy78,
            isHistoryMode = uiState.isHistoryMode,
            listStateProvider = { rememberLazyListState() }
        )

        // A B 策略 (34, 78)
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.B.name, "34", "78"),
            mainTitleIndex = 1,
            titleStr = strategyB.predicted.titleStr,
            predictedValue1 = strategyB.predicted.strategy34,
            predictedValue2 = strategyB.predicted.strategy78,
            displayItems1 = strategyB.strategy3Ways.strategy34,
            displayItems2 = strategyB.strategy3Ways.strategy78,
            isHistoryMode = uiState.isHistoryMode,
            listStateProvider = { rememberLazyListState() }
        )

        // B C 策略 (34, 78)
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.B.name, ColumnType.C.name, "34", "78"),
            mainTitleIndex = 1,
            titleStr = strategyC.predicted.titleStr,
            predictedValue1 = strategyC.predicted.strategy34,
            predictedValue2 = strategyC.predicted.strategy78,
            displayItems1 = strategyC.strategy3Ways.strategy34,
            displayItems2 = strategyC.strategy3Ways.strategy78,
            isHistoryMode = uiState.isHistoryMode,
            listStateProvider = { rememberLazyListState() }
        )

        Spacer(Modifier.height(SPACE_SIZE))

        if (uiState.isHistoryMode) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "当前为历史记录",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
        } else {
            InputButtons(uiState = uiState, onEvent = onEvent)
        }
    }

    // 处理副作用
    HandleSideEffects(sideEffectFlow = sideEffectFlow, context = context)

    // 显示日期选择对话框
    DateSelectDialog(uiState = uiState, onEvent = onEvent)
}

/**
 * 处理自动滚动
 */
@Composable
private fun HandleAutoScroll(
    tableData: List<com.dsd.baccarat.data.TableDisplayItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isHistoryMode: Boolean
) {
    // 历史数据不自动滑动到尾部
    if (isHistoryMode) return

    LaunchedEffect(tableData) {
        val lastRealIndex = tableData.indexOfLast { it is com.dsd.baccarat.data.TableDisplayItem.Real }
        if (lastRealIndex == -1) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset

        val targetInfo = layoutInfo.visibleItemsInfo.find { it.index == lastRealIndex }
        val isFullyVisible = targetInfo != null &&
                targetInfo.offset >= viewportStart &&
                (targetInfo.offset + targetInfo.size) <= viewportEnd

        if (!isFullyVisible) {
            listState.animateScrollToItem(lastRealIndex)
        }
    }
}

/**
 * 处理副作用（导航等）
 */
@Composable
private fun HandleSideEffects(
    sideEffectFlow: SharedFlow<GameSideEffect>,
    context: Context
) {
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collectLatest { sideEffect ->
            when (sideEffect) {
                is GameSideEffect.NavigateToHistory -> {
                    val intent = Intent(context, HistoryActivity::class.java).apply {
                        putExtra(HistoryActivity.KEY_GAME_ID, sideEffect.gameId)
                        putExtra(HistoryActivity.KEY_START_TIME, sideEffect.startTime)
                        putExtra(HistoryActivity.KEY_END_TIME, sideEffect.endTime)
                    }
                    context.startActivity(intent)
                }

                else -> {} // 其他副作用在主屏幕处理
            }
        }
    }
}
