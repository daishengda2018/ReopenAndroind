package com.dsd.baccarat.ui.compose

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.data.ColumnType
import com.dsd.baccarat.data.Counter
import com.dsd.baccarat.data.InputType
import com.dsd.baccarat.data.PredictedStrategy3WaysValue
import com.dsd.baccarat.data.Strategy3WaysData
import com.dsd.baccarat.data.Strategy3WyasDisplayItem
import com.dsd.baccarat.data.StrategyGridInfo
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TimerStatus
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import com.dsd.baccarat.data.room.entity.InputEntity
import com.dsd.baccarat.model.DefaultViewModel
import com.dsd.baccarat.model.DefaultViewModel.Companion.MAX_COLUMN_COUNT
import com.dsd.baccarat.ui.theme.PurpleGrey80
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

//  常量在顶部统一组织，清晰明了。
private val ITEM_SIZE = 22.dp
private val SPACE_SIZE = 5.dp
private val TABLE_HEIGHT = ITEM_SIZE * 4
private val TITLE_WIDTH_SHORT = ITEM_SIZE * 2
private val TITLE_WIDTH_LONG = ITEM_SIZE * 4
private val BORDER_SIZE = 0.3.dp
private const val MAX_VALUE = 8
private val TEXT_COLOR_B = Color.Red

private val TEXT_COLOR_P = Color.Blue

private val TEXT_COLOR_W = Color.Red

private val TEXT_COLOR_L = Color.Blue
private val TEXT_COLOR_NEUTRAL = Color.Black

private val RED_COLOR_VALUES1 = setOf(1, 4, 6, 7)
private val RED_COLOR_VALUES2 = setOf(1, 2, 5, 6)
private lateinit var sharedScrollStates: SnapshotStateList<LazyListState>

private val dateFormatter = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.CHINESE)

// 日期显示格式（如：2025-11-05）
private var mCurDateStr = ""
private var mIsHistoryModel = false

/**
 * 应用的主屏幕可组合函数。
 */
@Composable
fun Screen(viewModel: DefaultViewModel, isHistoryModel: Boolean = false) {
    mIsHistoryModel = isHistoryModel

    val elapsedTime = viewModel.elapsedTime.collectAsStateWithLifecycle().value
    val timerStatus = viewModel.timerStatus.collectAsStateWithLifecycle().value
    val showReminder = viewModel.showReminder.collectAsStateWithLifecycle().value
    val bppcTableData = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
    val bppcCounter = viewModel.bppcCounterStateFlow.collectAsStateWithLifecycle().value
    val wlCounter = viewModel.wlCounterStateFlow.collectAsStateWithLifecycle().value
    val wlTableData = viewModel.wlTableStateFlow.collectAsStateWithLifecycle().value
    val beltInputState = viewModel.curBeltInputStageFlow.collectAsStateWithLifecycle().value

    val strategyGridList = viewModel.stragetyGridStateFlow.map { it.collectAsStateWithLifecycle().value }
    val strategy3WaysList = viewModel.strategy3WaysStateFlowList.map { it.collectAsStateWithLifecycle().value }
    val predicted3WaysList = viewModel.predictedStateFlowList.map { it.collectAsStateWithLifecycle().value }
    val wHistoryCount = viewModel.wHistoryCounter.collectAsStateWithLifecycle().value
    val lHistoryCount = viewModel.lHistoryCounter.collectAsStateWithLifecycle().value
    val inputText = viewModel.inputText.collectAsStateWithLifecycle().value
    val onlyShowNewGame = viewModel.isOnlyShowNewGameStateFlow.collectAsStateWithLifecycle().value

    mCurDateStr = remember {
        if (mIsHistoryModel) {
            val data = (bppcTableData.firstOrNull() as? TableDisplayItem.Real)?.data
            dateFormatter.format(data?.dataA?.first ?: System.currentTimeMillis())
        } else {
            dateFormatter.format(System.currentTimeMillis())
        }
    }

    // 使用独立的可组合函数来管理提示音的创建/释放与播放
    NotificationSoundEffect(soundFlow = viewModel.soundEvent)
    sharedScrollStates = remember { mutableStateListOf() }
    LaunchedEffect(sharedScrollStates) {
        snapshotFlow {
            sharedScrollStates.firstOrNull()?.let {
                it.firstVisibleItemScrollOffset + it.firstVisibleItemIndex * 1000f
            }
        }.collect {
            val main = sharedScrollStates.firstOrNull() ?: return@collect
            val index = main.firstVisibleItemIndex
            val offset = main.firstVisibleItemScrollOffset
            sharedScrollStates.drop(1).forEach { state ->
                3
                if (state.firstVisibleItemIndex != index || state.firstVisibleItemScrollOffset != offset) {
                    state.scrollToItem(index, offset)
                }
            }
        }
    }
    HandleLayRowScroll(bppcTableData, sharedScrollStates.firstOrNull())

    Scaffold { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 左侧列
            LeftSide(
                bppcCounter,
                elapsedTime,
                timerStatus,
                showReminder,
                viewModel,
                bppcTableData,
                strategyGridList,
                strategy3WaysList,
                predicted3WaysList
            )

            // 右侧列
            RightSide(
                wlCounter,
                wlTableData,
                wHistoryCount,
                lHistoryCount,
                strategy3WaysList,
                predicted3WaysList,
                viewModel,
                timerStatus,
                inputText,
                beltInputState,
                onlyShowNewGame
            )
        }
    }
}

@Composable
private fun HandleLayRowScroll(tableData: List<TableDisplayItem>, mainState: LazyListState?) {
    mainState ?: return

    LaunchedEffect(tableData) {
        // 找到最后一个 Real 项的索引
        val lastRealIndex = tableData.indexOfLast { it is TableDisplayItem.Real }

        if (lastRealIndex == -1) return@LaunchedEffect

        // 获取当前 layoutInfo 快照
        val layoutInfo = mainState.layoutInfo
        val viewportStart = layoutInfo.viewportStartOffset
        val viewportEnd = layoutInfo.viewportEndOffset

        // 尝试在可见项中找到目标 item 的信息
        val targetInfo = layoutInfo.visibleItemsInfo.find { it.index == lastRealIndex }

        // 如果目标 item 在可见项中，则判断是否完全可见
        val isFullyVisible = targetInfo != null &&
                targetInfo.offset >= viewportStart &&
                (targetInfo.offset + targetInfo.size) <= viewportEnd

        // 如果没有可见信息（即目标不在可见项）或未完全可见，则滚动到最后
        if (!isFullyVisible) {
            // 平滑滚动到最后一项（suspend，可直接在 LaunchedEffect 中调用）
            mainState.animateScrollToItem(lastRealIndex)

            // 如果你仍然需要同步其它 registered states（之前的同步逻辑），
            // 下面这段会把主状态的位置同步给其它 states（根据你现有逻辑可选执行）：
            val newIndex = mainState.firstVisibleItemIndex
            val newOffset = mainState.firstVisibleItemScrollOffset
            sharedScrollStates.drop(1).forEach { state ->
                if (state.firstVisibleItemIndex != newIndex || state.firstVisibleItemScrollOffset != newOffset) {
                    // 这里用 scrollToItem 避开循环（若想更平滑可改为 animate，但注意循环触发）
                    state.scrollToItem(newIndex, newOffset)
                }
            }
        }
    }
}

@Composable
private fun RowScope.LeftSide(
    bppcCounter: Counter,
    elapsedTime: Int,
    timerStatus: TimerStatus,
    showReminder: Boolean,
    viewModel: DefaultViewModel,
    bppcTableData: List<TableDisplayItem>,
    strategyGridList: List<StrategyGridInfo>,
    strategy3WaysList: List<Strategy3WaysData>,
    predicted3WaysList: List<PredictedStrategy3WaysValue>,
) {
    Column(
        Modifier
            .weight(1f) // 使用 weight 实现灵活的权重布局
            .padding(horizontal = 5.dp)
    ) {
        Row {
            CounterDisplay(
                value1 = bppcCounter.count1, color1 = TEXT_COLOR_B,
                value2 = bppcCounter.count2, color2 = TEXT_COLOR_P
            )
            Spacer(Modifier.width(ITEM_SIZE))
            // 显示当前时间，需要动态更新
            CurrentTimeDisplay(
                elapsedTime = elapsedTime,
                timerStatus = timerStatus,
                showReminder = showReminder,
                onDismissReminder = { viewModel.dismissReminder() })
        }

        Row(Modifier.fillMaxWidth()) {
            BppcTableTitles()
            Spacer(Modifier.width(SPACE_SIZE))
            Table(
                items = bppcTableData,
                listState = rememberSyncedLazyListState(),
                showCharts = true // 这一列显示图表

            )
        }
        // A C
        val idxA = ColumnType.A.value
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.C.name, "12", "56"),
            predictedIndex = predicted3WaysList[idxA].predictionIndex,
            predictedValue1 = predicted3WaysList[idxA].strategy12,
            predictedValue2 = predicted3WaysList[idxA].strategy56,
            displayItems1 = strategy3WaysList[idxA].strategy12,
            displayItems2 = strategy3WaysList[idxA].strategy56,
        )

        // A B
        val idxB = ColumnType.B.value
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.B.name, "12", "56"),
            predictedIndex = predicted3WaysList[idxB].predictionIndex,
            predictedValue1 = predicted3WaysList[idxB].strategy12,
            predictedValue2 = predicted3WaysList[idxB].strategy56,
            displayItems1 = strategy3WaysList[idxB].strategy12,
            displayItems2 = strategy3WaysList[idxB].strategy56,
        )

        // B C
        val idxC = ColumnType.C.value
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.B.name, ColumnType.C.name, "12", "56"),
            predictedIndex = predicted3WaysList[idxC].predictionIndex,
            predictedValue1 = predicted3WaysList[idxC].strategy12,
            predictedValue2 = predicted3WaysList[idxC].strategy56,
            displayItems1 = strategy3WaysList[idxC].strategy12,
            displayItems2 = strategy3WaysList[idxC].strategy56,
        )

        Spacer(Modifier.height(SPACE_SIZE))
        Row {
            StrategyGridDisplay(ColumnType.A.toString(), strategyGridList[ColumnType.A.value])
            Spacer(Modifier.width(ITEM_SIZE))
            StrategyGridDisplay(ColumnType.B.toString(), strategyGridList[ColumnType.B.value])
            Spacer(Modifier.width(ITEM_SIZE))
            StrategyGridDisplay(ColumnType.C.toString(), strategyGridList[ColumnType.C.value])
        }
    }
}

@Composable
private fun StrategyGridDisplay(title: String, strategyItem: StrategyGridInfo) {
    Spacer(Modifier.height(SPACE_SIZE))
    // 使用一个占位的 Box 来确保布局一致性
    Row {
        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            TextItem(title)
        }
        Spacer(Modifier.width(SPACE_SIZE))
        for (i in 0 until MAX_COLUMN_COUNT) {
            val gridItem = strategyItem.itemList.getOrNull(i)
            val isObslate = gridItem?.status ?: false
            val dataList = gridItem?.items
            val conbinationTitle = gridItem?.title
            Column(Modifier.width(ITEM_SIZE)) {
                TextItem(
                    conbinationTitle ?: "",
                    determineColor(conbinationTitle?.toInt() ?: 0),
                    isObslate = isObslate,
                    isShowBorder = false,
                    width = TITLE_WIDTH_SHORT
                )

                val data2 = dataList?.getOrNull(0)
                TextItem(data2 ?: "", determineColor(data2), isObslate = isObslate, width = TITLE_WIDTH_SHORT)

                val data3 = dataList?.getOrNull(1)
                TextItem(data3 ?: "", determineColor(data3), isObslate = isObslate, width = TITLE_WIDTH_SHORT)

                val data4 = dataList?.getOrNull(2)
                TextItem(data4 ?: "", determineColor(data4), isObslate = isObslate, width = TITLE_WIDTH_SHORT)
            }
        }

        Spacer(Modifier.width(ITEM_SIZE))

        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            for (i in 0 until MAX_COLUMN_COUNT) {
                TextItem(strategyItem.predictedList.getOrNull(i) ?: "")
            }
        }

        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            for (i in 0 until MAX_COLUMN_COUNT) {
                TextItem(strategyItem.actualOpenedList.getOrNull(i) ?: "")
            }
        }
    }
}

@Composable
private fun RowScope.RightSide(
    counter: Counter,
    tableData: List<TableDisplayItem>,
    wHistoryCount: Int,
    lHistoryCount: Int,
    strategy3WaysList: List<Strategy3WaysData>,
    predicted3WaysList: List<PredictedStrategy3WaysValue>,
    viewModel: DefaultViewModel,
    timerStatus: TimerStatus,
    inputText: String,
    beltInputState: InputEntity?,
    isOnlyShowNewGame: Boolean
) {
    Column(
        Modifier
            .weight(1f) // 使用 weight 实现灵活的权重布局
            .padding(horizontal = 5.dp)
    ) {
        Row {
            CounterDisplay(
                value1 = counter.count1, color1 = TEXT_COLOR_W,
                value2 = counter.count2, color2 = TEXT_COLOR_L,
                padding = 0.dp,
                isShowWsr = true,
                isHistory = false
            )

            CounterDisplay(
                value1 = wHistoryCount, color1 = TEXT_COLOR_W,
                value2 = lHistoryCount, color2 = TEXT_COLOR_L,
                padding = 0.dp,
                isShowWsr = true,
                isHistory = true
            )
        }

        // 处理自动滑动
        val listState = rememberLazyListState()
        HandleLayRowScroll(tableData, listState)

        //  WL 表
        Table(
            items = tableData,
            listState = listState,
            showCharts = false, // 这一列不显示图表
            showHistory = true
        )

        // 可输入内容的文本框
        OutlinedTextField(
            value = inputText,
            // 更新 ViewModel 中的文字
            onValueChange = { newText -> viewModel.updateInputText(newText) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .height(TABLE_HEIGHT * 3 + SPACE_SIZE * 3),
            label = { Text(mCurDateStr) },
        )

        // A C
        val idxA = ColumnType.A.value
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.C.name, "34", "78"),
            predictedIndex = predicted3WaysList[idxA].predictionIndex,
            predictedValue1 = predicted3WaysList[idxA].strategy34,
            predictedValue2 = predicted3WaysList[idxA].strategy78,
            displayItems1 = strategy3WaysList[idxA].strategy34,
            displayItems2 = strategy3WaysList[idxA].strategy78,
        )

        // A B
        val idxB = ColumnType.B.value
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.A.name, ColumnType.B.name, "34", "78"),
            predictedIndex = predicted3WaysList[idxB].predictionIndex,
            predictedValue1 = predicted3WaysList[idxB].strategy34,
            predictedValue2 = predicted3WaysList[idxB].strategy78,
            displayItems1 = strategy3WaysList[idxB].strategy34,
            displayItems2 = strategy3WaysList[idxB].strategy78,
        )

        // B C
        val idxC = ColumnType.C.value
        Strategy3WaysDisplay(
            titles = listOf(ColumnType.B.name, ColumnType.C.name, "34", "78"),
            predictedIndex = predicted3WaysList[idxC].predictionIndex,
            predictedValue1 = predicted3WaysList[idxC].strategy34,
            predictedValue2 = predicted3WaysList[idxC].strategy78,
            displayItems1 = strategy3WaysList[idxC].strategy34,
            displayItems2 = strategy3WaysList[idxC].strategy78,
        )
        Spacer(Modifier.height(SPACE_SIZE))
        if (mIsHistoryModel) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TextItem("当前为历史记录", isShowBorder = false, color = Color.Gray, fontSize = 18.sp, width = TITLE_WIDTH_LONG * 2)
            }
        } else {
            InputButtons(viewModel, timerStatus, beltInputState, isOnlyShowNewGame)
        }
    }
}

@Composable
private fun NotificationSoundEffect(soundFlow: SharedFlow<Unit>) {
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    LaunchedEffect(soundFlow) {
        soundFlow.collectLatest {
            // 播放 1 秒提示音（1000ms）
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 10000)
        }
    }
}

/**
 *  将 BPPCounter 和 WLCounter 合并为一个可复用的 CounterDisplay 可组合函数。
 */
@Composable
private fun CounterDisplay(
    value1: Int, color1: Color,
    value2: Int, color2: Color,
    padding: Dp = 5.dp,
    isShowWsr: Boolean = false,
    isHistory: Boolean = false,
) {
    val total = value1 + value2
    val df = remember { DecimalFormat("0.0%") }
    val backgroundColor = remember(isHistory)
    {
        if (isHistory) {
            Color.LightGray // 选中时，使用主题色的淡色作为背景
        } else {
            Color.Transparent // 未选中时，背景透明
        }
    }

    Row(
        Modifier
            .wrapContentWidth()
            .padding(start = (ITEM_SIZE + padding))
            .height(ITEM_SIZE)
            .alpha(if (isHistory) 0.7f else 1f)
            .background(backgroundColor),
        horizontalArrangement = Arrangement.Start

    ) {
        TextItem("$value1", color1, isHistory = isHistory, width = TITLE_WIDTH_SHORT)
        TextItem("$value2", color2, isHistory = isHistory, width = TITLE_WIDTH_SHORT)
        TextItem("Total $total", Color.Black, isHistory = isHistory, width = TITLE_WIDTH_LONG)
        TextItem("${value1 - value2}", color2, isHistory = isHistory, width = TITLE_WIDTH_SHORT)


        if (isShowWsr) {
            val wsr = if (total != 0) value1 / total.toFloat() else 0
            TextItem(
                "WSR ${if (wsr == 1f) "100%" else df.format(wsr)}",
                Color.Magenta,
                isHistory = isHistory,
                width = TITLE_WIDTH_LONG
            )
        }
    }
}

@Composable
private fun CurrentTimeDisplay(
    elapsedTime: Int,
    timerStatus: TimerStatus,
    showReminder: Boolean,
    onDismissReminder: () -> Unit
) {
    val textStyle = remember {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(ITEM_SIZE)) {
        Text(
            text = mCurDateStr,
            style = textStyle,
            color = Color.Black
        )

        Spacer(Modifier.width(ITEM_SIZE))

        if (!mIsHistoryModel) {
            Text(
                text = String.format(Locale.getDefault(), "计时: %02d:%02d", minutes, seconds),
                style = textStyle,
                color = Color.Black
            )

            Spacer(Modifier.width(8.dp))
            // 显示状态
            Text(
                text = when (timerStatus) {
                    TimerStatus.Idle -> "未开始"
                    TimerStatus.Running -> "运行中"
                    TimerStatus.Paused -> "已暂停"
                    TimerStatus.Finished -> "已完成"
                },
                style = textStyle.copy(fontSize = 12.sp),
                color = Color.Gray
            )
        }
    }

    if (showReminder) {
        AlertDialog(
            onDismissRequest = onDismissReminder,
            confirmButton = {
                Button(onClick = onDismissReminder) {
                    Text("确定")
                }
            },
            text = { Text("该休息了") }
        )
    }
}

@Composable
private fun BppcTableTitles() {
    Column(Modifier.width(ITEM_SIZE)) {
        val mainTitles = remember { listOf("\\", "A", "B", "C") }
        val subTitles = remember { listOf("A", "B", "C") }

        mainTitles.forEach { TextItem(it, TEXT_COLOR_NEUTRAL) }
        Spacer(Modifier.height(SPACE_SIZE))
        subTitles.forEach {
            TextItem(it, TEXT_COLOR_NEUTRAL)
            Spacer(Modifier.height(ITEM_SIZE * 3 + SPACE_SIZE))
        }
    }
}

/**
 *  合并了 TableAndChart 和 WsrTable。现在通过参数控制是否显示图表，增强了复用性。
 */
@Composable
private fun Table(
    items: List<TableDisplayItem>,
    listState: LazyListState,
    showCharts: Boolean,
    showHistory: Boolean = false
) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(
            items = items,
            key = { index, item -> "$index-${item.hashCode()}" }
        ) { idx, item ->
            val data = (item as? TableDisplayItem.Real)?.data
            Column(Modifier.width(ITEM_SIZE)) {
                // 索引行
                TextItem("${idx + 1}", TEXT_COLOR_NEUTRAL, fontSize = 10.sp)
                // 数据行 (A, B, C)
                TextItem(
                    text = data?.dataA?.second?.toString() ?: "",
                    color = determineColor(data?.dataA?.second),
                    isHistory = if (showHistory) {
                        data?.dataA?.first ?: false
                    } else false
                )
                TextItem(
                    text = data?.dataB?.second?.toString() ?: "",
                    color = determineColor(data?.dataB?.second),
                    isHistory = if (showHistory) {
                        data?.dataB?.first ?: false
                    } else false
                )
                TextItem(
                    text = data?.dataC?.second?.toString() ?: "",
                    color = determineColor(data?.dataC?.second),
                    isHistory = if (showHistory) {
                        data?.dataC?.first ?: false
                    } else false
                )

                if (showCharts) {
                    Spacer(Modifier.height(SPACE_SIZE))
                    VerticalBarChart(value = data?.dataA)
                    Spacer(Modifier.height(SPACE_SIZE))
                    VerticalBarChart(value = data?.dataB)
                    Spacer(Modifier.height(SPACE_SIZE))
                    VerticalBarChart(value = data?.dataC)
                }
            }
        }
    }
}

/**
 * 一个全新的、独立的“策略区块”可组合函数，用于显示一组策略。
 */
@Composable
private fun Strategy3WaysDisplay(
    titles: List<String>,
    predictedIndex: String?,
    predictedValue1: String?,
    predictedValue2: String?,
    displayItems1: List<Strategy3WyasDisplayItem>,
    displayItems2: List<Strategy3WyasDisplayItem>,
    ) {
    val selectedOption = remember { mutableIntStateOf(1) }

    Spacer(Modifier.height(SPACE_SIZE))

    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(ITEM_SIZE)) {
            Spacer(Modifier.height(ITEM_SIZE)) // 与标题行对齐
            TextItem(titles[0], TEXT_COLOR_NEUTRAL)
            TextItem(titles[1], TEXT_COLOR_NEUTRAL)
        }

        Spacer(Modifier.width(SPACE_SIZE))

        Column {
            Row {
                TextItem(titles[2], width = TITLE_WIDTH_SHORT, isSelected = (selectedOption.intValue == 1))
                {
                    selectedOption.intValue = 1
                }
                Spacer(Modifier.width(ITEM_SIZE)) // 与标题行对齐
                TextItem(titles[3], width = TITLE_WIDTH_SHORT, isSelected = (selectedOption.intValue == 2))
                {
                    selectedOption.intValue = 2
                }
                when (selectedOption.intValue) {
                    1 -> TextItem(
                        "预测第 ${predictedIndex ?: "-"} 位为 ${predictedValue1 ?: "-"}",
                        color = if (predictedValue1.isNullOrEmpty()) Color.Gray else Color.Magenta,
                        width = (TITLE_WIDTH_LONG * 2)
                    )

                    2 -> TextItem(
                        "预测第 ${predictedIndex ?: "-"} 位为 ${predictedValue2 ?: "-"}",
                        color = if (predictedValue2.isNullOrEmpty()) Color.Gray else Color.Magenta,
                        width = (TITLE_WIDTH_LONG * 2)
                    )
                }
            }
            // 步骤 2: 根据当前选中的状态，决定要显示哪个数据列表。
            when (selectedOption.intValue) {
                1 -> Strategy3WyasTable(displayItems1)
                2 -> Strategy3WyasTable(displayItems2)
            }
        }
    }
}

/**
 *这是 StrategyMap 完全重写和修复后的版本。
 * 它现在能够正确地显示每个数据列对应的两行策略数据。
 */
@Composable
private fun Strategy3WyasTable(
    displayItems: List<Strategy3WyasDisplayItem>
) {
    LazyRow(state = rememberSyncedLazyListState(), modifier = Modifier.fillMaxWidth()) {
        items(
            count = displayItems.size,
            key = { index -> index }
        ) { idx ->
            val item = (displayItems.getOrNull(idx) as? Strategy3WyasDisplayItem.Real)?.data

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem(
                    text = if (item?.first == -1) "/" else item?.first?.toString() ?: "",
                    color = determineColorFor3Ways(item?.first)
                )
                TextItem(
                    text = if (item?.second == -1) "/" else item?.second?.toString() ?: "",
                    color = determineColorFor3Ways(item?.second)
                )
            }
        }
    }
}

/**
 *  将 Canvas 绘制逻辑提取到独立的可组合函数中，使 BppcTable 更简洁。
 */
@Composable
fun VerticalBarChart(value: Pair<Boolean, Int?>?) {
    val color = determineColor(value?.second)

    Box(
        Modifier
            .width(ITEM_SIZE)
            .height(TABLE_HEIGHT)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val gridColor = Color.LightGray
            val gridWidth = BORDER_SIZE.toPx()
            val interval = size.height / MAX_VALUE
            // 绘制网格线和边框
            for (i in 0..MAX_VALUE) {
                drawLine(
                    color = if (i == 4) Color.Black else gridColor,
                    start = Offset(0f, i * interval),
                    end = Offset(size.width, i * interval),
                    strokeWidth = gridWidth
                )
            }
            // 绘制边框
            drawLine(
                gridColor,
                Offset(size.width, 0f),
                Offset(size.width, size.height),
                gridWidth * 1.8f
            )
            drawLine(
                gridColor,
                Offset(0f, 0f),
                Offset(0f, size.height),
                gridWidth * 1.8f
            )

            // 底部边框加粗
            drawLine(
                Color.Black,
                Offset(0f, size.height),
                Offset(size.width, size.height),
                gridWidth
            )

            // 如果有值，则绘制柱状图和数值文本
            val value = value?.second ?: 0
            if (value > 0) {
                val barHeight = (value.toFloat() / MAX_VALUE) * size.height
                drawRect(
                    color = color,
                    topLeft = Offset(size.width / 2 - 1.dp.toPx(), size.height - barHeight),
                    size = Size(2.dp.toPx(), barHeight)
                )
            }
        }
    }
}

/**
 * 一个可复用的、带边框的文本项。
 */
@Composable
fun TextItem(
    text: String,
    color: Color = Color.Black,
    width: Dp = ITEM_SIZE,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    isSelected: Boolean = false,
    isObslate: Boolean = false,
    isShowBorder: Boolean = true,
    isHistory: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // 步骤 1: 根据 isSelected 状态决定背景色和文字颜色
    val backgroundColor = remember(isSelected, isObslate)
    {
        if (isSelected) {
            PurpleGrey80 // 选中时，使用主题色的淡色作为背景
        } else if (isObslate || isHistory) {
            Color.LightGray
        } else {
            Color.Transparent // 未选中时，背景透明
        }
    }

    Box(
        Modifier
            .width(width)
            .height(ITEM_SIZE)
            .background(backgroundColor)
            .clickable { onClick?.invoke() }
            .alpha(if (isObslate) 0.5f else 1.0f)
            .conditionalBorder(isShowBorder, isObslate || isHistory),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, fontSize = fontSize, color = color, fontWeight = fontWeight
        )
    }
}

/**
 *  输入按钮现在通过 lambda 表达式接收回调，从而与 ViewModel 解耦，提升了组件的独立性。
 */
@Composable
private fun InputButtons(viewModel: DefaultViewModel, timerStatus: TimerStatus, beltInputState: InputEntity?, isOnlyShowNewGame: Boolean) {
    val context = LocalContext.current
    val isDialogVisible by viewModel.isDialogVisible.collectAsState()

    @Composable
    fun ColumnScope.DefaultButtonModifier(): Modifier = remember {
        Modifier
            .padding(3.dp)
            .fillMaxWidth()
            .weight(1f)
    }

    // 显示日期选择 Dialog
    if (isDialogVisible) {
        DateSelectDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { viewModel.confirmSelection(context) }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            var isBeltPEnable = true
            var isBeltBEnable = true
            var isInputB = false
            var isInputP = false

            when (beltInputState?.inputType) {
                InputType.B -> {
                    isInputB = true
                    isBeltPEnable = false
                }

                InputType.P -> {
                    isInputP = true
                    isBeltBEnable = false
                }

                null -> {}
            }

            Button(
                modifier = DefaultButtonModifier(),
                enabled = isBeltBEnable && !isOnlyShowNewGame,
                onClick = { viewModel.betB() }) {
                Text(text = if (isInputB) "押 B 中" else "押 B")
            }

            Button(
                modifier = DefaultButtonModifier(),
                enabled = isBeltPEnable && !isOnlyShowNewGame,
                onClick = { viewModel.betP() }) {
                Text(text = if (isInputP) "押 P 中" else "押 P")
            }

            Button(modifier = DefaultButtonModifier(), enabled = !isOnlyShowNewGame, onClick = { viewModel.removeLastBet() }) { Text(text = "撤销") }
        }

        Column(Modifier.weight(1f)) {
            Button(modifier = DefaultButtonModifier(), enabled = !isOnlyShowNewGame, onClick = { viewModel.openB() }) { Text(text = "开 B") }
            Button(modifier = DefaultButtonModifier(), enabled = !isOnlyShowNewGame, onClick = { viewModel.openP() }) { Text(text = "开 P") }
            Button(modifier = DefaultButtonModifier(), enabled = !isOnlyShowNewGame, onClick = { viewModel.removeLastOpen() }) { Text(text = "撤销") }
        }

        Spacer(Modifier.weight(0.5f))

        Column(Modifier.weight(1f)) {
            // 计时按钮现在控制传入的计时器
            val isRunting = (timerStatus == TimerStatus.Running)
            val isStartedEnable = (timerStatus == TimerStatus.Running || timerStatus == TimerStatus.Paused)

            Button(modifier = DefaultButtonModifier(), enabled = !isRunting && !isOnlyShowNewGame, onClick = { viewModel.startTimer() }) {
                Text(
                    text = when (timerStatus) {
                        TimerStatus.Running -> "正在运行"
                        TimerStatus.Paused -> "重新开始"
                        else -> "开始记时"
                    }
                )
            }
            Button(
                modifier = DefaultButtonModifier(),
                enabled = isStartedEnable && !isOnlyShowNewGame,
                onClick = { viewModel.pauseOrResumeTimer() }) {
                Text(
                    text = when (timerStatus) {
                        TimerStatus.Running -> "暂停记时"
                        TimerStatus.Paused -> "继续记时"
                        else -> "未开始"
                    }
                )
            }
            Button(
                modifier = DefaultButtonModifier(),
                enabled = isStartedEnable && !isOnlyShowNewGame,
                onClick = { viewModel.stopTimerJob() }) { Text(text = "结束记时") }
        }

        Column(Modifier.weight(1f)) {
            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.showSelectHistoryDialog() }) { Text(text = "历史") }
            Button(modifier = DefaultButtonModifier(), enabled = !isOnlyShowNewGame, onClick = { viewModel.save() }) { Text(text = "保存") }
            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.newGame() }) { Text(text = "新牌") }
        }
    }
}

/**
 * 日期选择 Dialog（仅显示 InputDao 中存在的日期）
 */
@Composable
private fun DateSelectDialog(
    viewModel: DefaultViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val availableDates by viewModel.availableDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // 固定高度，避免内容过多时拉伸
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    // 加载中显示进度条
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (availableDates.isEmpty()) {
                    // 无数据时提示
                    Text(
                        text = "暂无数据",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // 显示可选日期列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(availableDates) { date ->
                            val isSelected = (date == selectedDate)
                            GameSessionItem(
                                gameSession = date,
                                isSelected = isSelected,
                                onClick = { viewModel.selectDate(date) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 单个 GameSession 项（可点击选择）
 */
@Composable
private fun GameSessionItem(
    gameSession: GameSessionEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val format = remember { SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.CHINESE) }
    Text(
        text = "${format.format(gameSession.startTime)} ~ ${format.format(gameSession.endTime)}",
        fontSize = 16.sp,
        color = if (isSelected) {
            Color.Blue // 选中状态高亮
        } else {
            Color.Black
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
@Composable
fun rememberSyncedLazyListState(): LazyListState {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { sharedScrollStates.add(state) }
    DisposableEffect(Unit) { onDispose { sharedScrollStates.remove(state) } }
    return state
}

/**
 * 根据数值确定文本颜色的辅助函数。已优化，可以安全处理 null 值。
 */
private fun determineColor(value: Int?): Color {
    if (value == null) return TEXT_COLOR_NEUTRAL
    return if (value in RED_COLOR_VALUES1) TEXT_COLOR_B else TEXT_COLOR_P
}

private fun determineColor(value: String?): Color {
    if (value == null) return TEXT_COLOR_NEUTRAL
    return if (value == "B") TEXT_COLOR_B else TEXT_COLOR_P
}

private fun determineColorFor3Ways(value: Int?): Color {
    if (value == null) return TEXT_COLOR_NEUTRAL
    return if (value in RED_COLOR_VALUES2) Color.Red else Color.Blue
}

fun Modifier.conditionalBorder(showBorder: Boolean, isWhiteBorder: Boolean): Modifier {
    return if (showBorder) {
        this.border(BorderStroke(BORDER_SIZE, if (isWhiteBorder) Color.White else Color.LightGray))
    } else {
        this
    }
}
