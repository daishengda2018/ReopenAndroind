package com.dsd.baccarat.ui.game.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.ColumnType
import com.dsd.baccarat.data.PredictedStrategy3WaysValue
import com.dsd.baccarat.data.Strategy3WaysData
import com.dsd.baccarat.data.StrategyGridInfo
import com.dsd.baccarat.ui.game.state.GameUiEvent
import com.dsd.baccarat.ui.game.state.GameUiState
import com.dsd.baccarat.ui.game.components.common.CounterDisplay
import com.dsd.baccarat.ui.game.components.common.CurrentTimeDisplay
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.SPACE_SIZE
import com.dsd.baccarat.ui.game.components.common.TEXT_COLOR_BANKER
import com.dsd.baccarat.ui.game.components.common.TEXT_COLOR_PLAYER
import com.dsd.baccarat.ui.game.components.strategy.Strategy3WaysDisplay
import com.dsd.baccarat.ui.game.components.strategy.StrategyGridDisplay
import com.dsd.baccarat.ui.game.components.table.GameTable
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 策略数据的缓存容器，避免重复计算
 */
@Immutable
private data class StrategyData(
    val predicted: PredictedStrategy3WaysValue,
    val strategy3Ways: Strategy3WaysData,
    val strategyGrid: StrategyGridInfo
)

/**
 * 左侧区域组件
 *
 * @param uiState UI 状态
 * @param onEvent 事件处理回调
 * @param tableListStateProvider 表格滚动状态提供者
 * @param modifier 修饰符
 */
@Composable
fun LeftSideSection(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit,
    tableListStateProvider: @Composable () -> LazyListState,
    modifier: Modifier = Modifier
) {
    // 缓存频繁调用的 getter 方法结果，避免重复计算
    val strategyA = remember(uiState.strategy3WaysList, uiState.predictedList) {
        StrategyData(
            predicted = uiState.getPredicted(ColumnType.A),
            strategy3Ways = uiState.getStrategy3Ways(ColumnType.A),
            strategyGrid = uiState.getStrategyGrid(ColumnType.A)
        )
    }
    val strategyB = remember(uiState.strategy3WaysList, uiState.predictedList) {
        StrategyData(
            predicted = uiState.getPredicted(ColumnType.B),
            strategy3Ways = uiState.getStrategy3Ways(ColumnType.B),
            strategyGrid = uiState.getStrategyGrid(ColumnType.B)
        )
    }
    val strategyC = remember(uiState.strategy3WaysList, uiState.predictedList) {
        StrategyData(
            predicted = uiState.getPredicted(ColumnType.C),
            strategy3Ways = uiState.getStrategy3Ways(ColumnType.C),
            strategyGrid = uiState.getStrategyGrid(ColumnType.C)
        )
    }

    Column(
        modifier
            .padding(horizontal = SPACE_SIZE)
    ) {
        Row {
            Spacer(Modifier.width(ITEM_SIZE + SPACE_SIZE))
            CounterDisplay(
                value1 = uiState.bppcCounter.count1,
                color1 = TEXT_COLOR_BANKER,
                value2 = uiState.bppcCounter.count2,
                color2 = TEXT_COLOR_PLAYER
            )
            Spacer(Modifier.width(ITEM_SIZE))

            if (uiState.isHistoryMode) {
                val detailDateFormatter = remember {
                    SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.CHINESE)
                }
                val text = "${detailDateFormatter.format(uiState.historyStartTime)} ~ " +
                        "${detailDateFormatter.format(uiState.historyEndTime)}"
                TextItem(
                    "游戏时间: $text",
                    isShowBorder = false,
                    color = Color.Black,
                    fontSize = 15.sp,
                    width = TITLE_WIDTH_LONG * 4
                )
            } else {
                // 移除不必要的 remember，每次都获取最新时间
                val currentDateStr = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.CHINESE).format(System.currentTimeMillis())
                CurrentTimeDisplay(
                    dateStr = currentDateStr,
                    elapsedTime = uiState.timerState.elapsedSeconds,
                    timerStatus = uiState.timerState.status,
                    showReminder = uiState.timerState.showReminder,
                    onDismissReminder = { onEvent(GameUiEvent.DismissReminder) }
                )
            }
        }

        Row(Modifier.fillMaxWidth()) {
            BppcTableTitles()
            Spacer(Modifier.width(SPACE_SIZE))
            GameTable(
                items = uiState.bppcTableData,
                listState = tableListStateProvider(),
                showCharts = true
            )
        }

        // A C 策略
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.C.name, "12", "56"),
            mainTitleIndex = 0,
            titleStr = strategyA.predicted.titleStr,
            predictedValue1 = strategyA.predicted.strategy12,
            predictedValue2 = strategyA.predicted.strategy56,
            displayItems1 = strategyA.strategy3Ways.strategy12,
            displayItems2 = strategyA.strategy3Ways.strategy56,
            isHistoryMode = uiState.isHistoryMode,
            listStateProvider = tableListStateProvider
        )

        // A B 策略
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.B.name, "12", "56"),
            mainTitleIndex = 1,
            titleStr = strategyB.predicted.titleStr,
            predictedValue1 = strategyB.predicted.strategy12,
            predictedValue2 = strategyB.predicted.strategy56,
            displayItems1 = strategyB.strategy3Ways.strategy12,
            displayItems2 = strategyB.strategy3Ways.strategy56,
            isHistoryMode = uiState.isHistoryMode,
            listStateProvider = tableListStateProvider
        )

        // B C 策略
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.B.name, ColumnType.C.name, "12", "56"),
            mainTitleIndex = 1,
            titleStr = strategyC.predicted.titleStr,
            predictedValue1 = strategyC.predicted.strategy12,
            predictedValue2 = strategyC.predicted.strategy56,
            displayItems1 = strategyC.strategy3Ways.strategy12,
            displayItems2 = strategyC.strategy3Ways.strategy56,
            isHistoryMode = uiState.isHistoryMode,
            listStateProvider = tableListStateProvider
        )

        Spacer(Modifier.height(SPACE_SIZE))
        Row {
            StrategyGridDisplay(ColumnType.A.toString(), strategyA.strategyGrid)
            Spacer(Modifier.width(ITEM_SIZE))
            StrategyGridDisplay(ColumnType.B.toString(), strategyB.strategyGrid)
            Spacer(Modifier.width(ITEM_SIZE))
            StrategyGridDisplay(ColumnType.C.toString(), strategyC.strategyGrid)
        }
    }
}

@Composable
private fun BppcTableTitles() {
    Column(Modifier.width(ITEM_SIZE)) {
        val titles = remember { listOf("A", "B", "C") }

        Spacer(Modifier.height(ITEM_SIZE))
        titles.forEach { com.dsd.baccarat.ui.game.components.common.TextItem(text = it, color = Color.Black) }
        Spacer(Modifier.width(SPACE_SIZE))
        titles.forEach {
            com.dsd.baccarat.ui.game.components.common.TextItem(text = it, color = Color.Black)
            Spacer(Modifier.height(ITEM_SIZE * 3 + SPACE_SIZE))
        }
    }
}

private val TITLE_WIDTH_LONG = com.dsd.baccarat.ui.game.components.common.TITLE_WIDTH_LONG

@Composable
private fun TextItem(
    text: String,
    isShowBorder: Boolean,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    width: androidx.compose.ui.unit.Dp
) {
    com.dsd.baccarat.ui.game.components.common.TextItem(
        text = text,
        color = color,
        width = width,
        fontSize = fontSize,
        isShowBorder = isShowBorder
    )
}
