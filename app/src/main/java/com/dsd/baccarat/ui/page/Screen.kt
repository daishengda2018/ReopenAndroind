package com.dsd.baccarat.ui.page

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.data.ColumnType
import com.dsd.baccarat.data.Counter
import com.dsd.baccarat.data.InputType
import com.dsd.baccarat.data.InputViewModel
import com.dsd.baccarat.data.InputViewModel.Companion.MAX_COLUMN_COUNT
import com.dsd.baccarat.data.InputViewModel.Companion.MIN_TABLE_COLUMN_COUNT
import com.dsd.baccarat.data.PredictedStrategy3WaysValue
import com.dsd.baccarat.data.Strategy3WaysData
import com.dsd.baccarat.data.Strategy3WyasDisplayItem
import com.dsd.baccarat.data.Strategy3WyasItem
import com.dsd.baccarat.data.StrategyGridItem
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TableItem
import com.dsd.baccarat.data.TimerStatus
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

private val RED_COLOR_VALUES = setOf(1, 4, 6, 7)

/**
 * 应用的主屏幕可组合函数。
 */
@Composable
fun Screen(viewModel: InputViewModel) {
    val synchronizedListState = rememberLazyListState()
    val elapsedTime = viewModel.elapsedTime.collectAsStateWithLifecycle().value
    val timerStatus = viewModel.timerStatus.collectAsStateWithLifecycle().value
    val showReminder = viewModel.showReminder.collectAsStateWithLifecycle().value
    val bppcTableData = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
    val bppcCounter = viewModel.bppcCounterStateFlow.collectAsStateWithLifecycle().value
    val wlCounter = viewModel.wlCounterStateFlow.collectAsStateWithLifecycle().value
    val wlTableData = viewModel.wlTableStateFlow.collectAsStateWithLifecycle().value
    val beltInputState = viewModel.beltInputStageFlow.collectAsStateWithLifecycle().value

    val strategyGridList = viewModel.stragetyGridStateFlow.map { it.collectAsStateWithLifecycle().value }
    val strategy3WaysList = viewModel.strategy3WaysStateFlowList.map { it.collectAsStateWithLifecycle().value }
    val predicted3WaysList = viewModel.predictedStateFlowList.map { it.collectAsStateWithLifecycle().value }

    // 使用独立的可组合函数来管理提示音的创建/释放与播放
    NotificationSoundEffect(soundFlow = viewModel.soundEvent)

    // 优化自动滚动逻辑
//    LaunchedEffect(bppcTableData) { // 监听 items 的变化
//        val lastRealIndex = bppcTableData.indexOfLast { it is BppcDisplayItem.Real }
//        if (lastRealIndex != -1) {
//            // 使用 animateScrollToItem 获得更平滑的滚动动画效果
//            synchronizedListState.animateScrollToItem(bppcTableData.lastIndex)
//        }
//    }

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
                synchronizedListState,
                strategyGridList,
                strategy3WaysList,
                predicted3WaysList
            )

            // 右侧列
            RightSide(
                wlCounter,
                wlTableData,
                synchronizedListState,
                strategy3WaysList,
                predicted3WaysList,
                viewModel,
                timerStatus,
                beltInputState
            )
        }
    }
}

@Composable
private fun RowScope.LeftSide(
    bppcCounter: Counter,
    elapsedTime: Int,
    timerStatus: TimerStatus,
    showReminder: Boolean,
    viewModel: InputViewModel,
    bppcTableData: List<TableDisplayItem>,
    synchronizedListState: LazyListState,
    strategyGridList: List<StrategyGridItem>,
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
                label1 = "B", value1 = bppcCounter.count1, color1 = TEXT_COLOR_B,
                label2 = "P", value2 = bppcCounter.count2, color2 = TEXT_COLOR_P
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
                listState = synchronizedListState,
                showCharts = true // 这一列显示图表
            )
        }
        ColumnType.entries.forEach { type ->
            val idx = type.value
            if (idx < predicted3WaysList.size && idx < strategy3WaysList.size) {
                Strategy3WaysDisplay(
                    titles = listOf(type.name, "12", "56"),
                    predictedIndex = predicted3WaysList[idx].predictionIndex,
                    predictedValue1 = predicted3WaysList[idx].strategy12,
                    predictedValue2 = predicted3WaysList[idx].strategy56,
                    displayItems1 = strategy3WaysList[idx].strategy12,
                    displayItems2 = strategy3WaysList[idx].strategy56,
                    listState = synchronizedListState
                )
            }
        }
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
private fun StrategyGridDisplay(title: String, strategyItem: StrategyGridItem) {
    Spacer(Modifier.height(SPACE_SIZE))
    // 使用一个占位的 Box 来确保布局一致性
    Row {
        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            TextItem(title)
        }
        Spacer(Modifier.width(SPACE_SIZE))
        for (i in 0 until MAX_COLUMN_COUNT) {
            val isObslate = strategyItem.itemList.getOrNull(i)?.first ?: false
            val dataList = strategyItem.itemList.getOrNull(i)?.second
            dataList?.first()
            Column(Modifier.width(ITEM_SIZE)) {
                val data1 = dataList?.getOrNull(0)
                TextItem(
                    data1 ?: "",
                    determineColor(data1?.toInt() ?: 0),
                    isObslate = isObslate,
                    isShowBorder = false,
                    width = TITLE_WIDTH_SHORT
                )

                val data2 = dataList?.getOrNull(1)
                TextItem(data2 ?: "", determineColor(data2), isObslate = isObslate, width = TITLE_WIDTH_SHORT)

                val data3 = dataList?.getOrNull(2)
                TextItem(data3 ?: "", determineColor(data3), isObslate = isObslate, width = TITLE_WIDTH_SHORT)

                val data4 = dataList?.getOrNull(3)
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
    synchronizedListState: LazyListState,
    strategy3WaysList: List<Strategy3WaysData>,
    predicted3WaysList: List<PredictedStrategy3WaysValue>,
    viewModel: InputViewModel,
    timerStatus: TimerStatus,
    beltInputState: InputType?
) {
    Column(
        Modifier
            .weight(1f) // 使用 weight 实现灵活的权重布局
            .padding(horizontal = 5.dp)
    ) {

        CounterDisplay(
            label1 = "W", value1 = counter.count1, color1 = TEXT_COLOR_W,
            label2 = "L", value2 = counter.count2, color2 = TEXT_COLOR_L,
            padding = 0.dp,
            isShowWsr = true,
            isHistory = false
        )

        //  复用 BppcTable 组件。
        Table(
            items = tableData,
            listState = synchronizedListState,
            showCharts = false // 这一列不显示图表
        )

        CounterDisplay(
            label1 = "W", value1 = counter.count1, color1 = TEXT_COLOR_W,
            label2 = "L", value2 = counter.count2, color2 = TEXT_COLOR_L,
            padding = 0.dp,
            isShowWsr = true,
            isHistory = true
        )

        // 用 Spacer 来与左侧的图表区域在布局上对齐
        Spacer(Modifier.height(TABLE_HEIGHT * 3 + SPACE_SIZE * 3 - ITEM_SIZE))

        // 右侧列的策略区块
        ColumnType.entries.forEach { type ->
            val idx = type.value
            if (idx < predicted3WaysList.size && idx < strategy3WaysList.size) {
                Strategy3WaysDisplay(
                    titles = listOf(type.name, "34", "78"),
                    predictedIndex = predicted3WaysList[idx].predictionIndex,
                    predictedValue1 = predicted3WaysList[idx].strategy12,
                    predictedValue2 = predicted3WaysList[idx].strategy56,
                    displayItems1 = strategy3WaysList[idx].strategy12,
                    displayItems2 = strategy3WaysList[idx].strategy56,
                    listState = synchronizedListState
                )
            }
        }
        Spacer(Modifier.height(SPACE_SIZE))
        InputButtons(viewModel, timerStatus, beltInputState)
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
    label1: String, value1: Int, color1: Color,
    label2: String, value2: Int, color2: Color,
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
        TextItem("$label1$value1", color1, isHistory = isHistory, width = TITLE_WIDTH_SHORT)
        TextItem("$label2$value2", color2, isHistory = isHistory, width = TITLE_WIDTH_SHORT)
        TextItem("$label1 - $label2 =  ${value1 - value2}", color2, isHistory = isHistory, width = TITLE_WIDTH_LONG)
        TextItem("Total $total", Color.Black, isHistory = isHistory, width = TITLE_WIDTH_LONG)

        if (isShowWsr) {
            val wsr = if (total != 0) value1 / total.toFloat() else 0
            TextItem("WSR ${if (wsr == 1f) "100%" else df.format(wsr)}", Color.Magenta, isHistory = isHistory, width = TITLE_WIDTH_LONG)
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
    val timeString = remember {
        SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault()).format(System.currentTimeMillis())
    }
    val textStyle = remember {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(ITEM_SIZE)) {
        Text(
            text = "$timeString",
            style = textStyle,
            color = Color.Black
        )

        Spacer(Modifier.width(ITEM_SIZE))
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
            },
            style = textStyle.copy(fontSize = 12.sp),
            color = Color.Gray
        )
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
    showCharts: Boolean
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
                    text = data?.dataA?.toString() ?: "",
                    color = determineColor(data?.dataA)
                )
                TextItem(
                    text = data?.dataB?.toString() ?: "",
                    color = determineColor(data?.dataB)
                )
                TextItem(
                    text = data?.dataC?.toString() ?: "",
                    color = determineColor(data?.dataC)
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
    listState: LazyListState,

    ) {
    val selectedOption = remember { mutableIntStateOf(1) }

    Spacer(Modifier.height(SPACE_SIZE))

    Row(Modifier.fillMaxWidth()) {
        val mainTitle = titles[0]
        if (mainTitle.isNotEmpty()) {
            Column(Modifier.width(ITEM_SIZE)) {
                Spacer(Modifier.height(ITEM_SIZE)) // 与标题行对齐
                TextItem(mainTitle, TEXT_COLOR_NEUTRAL)
            }
        }

        Spacer(Modifier.width(SPACE_SIZE))

        Column {
            Row {
                TextItem(titles[1], width = TITLE_WIDTH_SHORT, isSelected = (selectedOption.intValue == 1))
                {
                    selectedOption.intValue = 1
                }
                TextItem(titles[2], width = TITLE_WIDTH_SHORT, isSelected = (selectedOption.intValue == 2))
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
                1 -> Strategy3WyasTable(listState, displayItems1)
                2 -> Strategy3WyasTable(listState, displayItems2)
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
    listState: LazyListState,
    displayItems: List<Strategy3WyasDisplayItem>
) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        items(
            count = displayItems.size,
            key = { index -> index }
        ) { idx ->
            val item = (displayItems.getOrNull(idx) as? Strategy3WyasDisplayItem.Real)?.data

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem(
                    text = item?.first?.toString() ?: "",
                    color = determineColor(item?.first)
                )
                TextItem(
                    text = item?.second?.toString() ?: "",
                    color = determineColor(item?.second)
                )
            }
        }
    }
}

/**
 *  将 Canvas 绘制逻辑提取到独立的可组合函数中，使 BppcTable 更简洁。
 */
@Composable
fun VerticalBarChart(value: Int?) {
    val color = determineColor(value)
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember(color) {
        TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = color)
    }

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
            if (value != null && value > 0) {
                val barHeight = (value.toFloat() / MAX_VALUE) * size.height
                drawRect(
                    color = color,
                    topLeft = Offset(size.width / 2 - 1.dp.toPx(), size.height - barHeight),
                    size = Size(2.dp.toPx(), barHeight)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = AnnotatedString("$value"),
                    topLeft = Offset(6f, size.height - barHeight),
                    style = textStyle
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
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
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
        } else if (isObslate) {
            Color.Gray
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
        if (isObslate) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Black,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

/**
 *  输入按钮现在通过 lambda 表达式接收回调，从而与 ViewModel 解耦，提升了组件的独立性。
 */
@Composable
private fun InputButtons(viewModel: InputViewModel, timerStatus: TimerStatus, beltInputState: InputType?) {
    @Composable
    fun ColumnScope.DefaultButtonModifier(): Modifier = remember {
        Modifier
            .padding(3.dp)
            .fillMaxWidth()
            .weight(1f)
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

            when (beltInputState) {
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
                enabled = isBeltBEnable,
                onClick = { viewModel.betB() }) {
                Text(text = if (isInputB) "押 B 中" else "押 B")
            }

            Button(
                modifier = DefaultButtonModifier(),
                enabled = isBeltPEnable,
                onClick = { viewModel.betP() }) {
                Text(text = if (isInputP) "押 P 中" else "押 P")
            }

            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.removeLastBet() }) { Text(text = "撤销") }
        }

        Column(Modifier.weight(1f)) {
            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.openB() }) { Text(text = "开 B") }
            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.openP() }) { Text(text = "开 P") }
            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.removeLastOpen() }) { Text(text = "撤销") }
        }

        Spacer(Modifier.weight(1f))

        Column(Modifier.weight(1f)) {
            // 计时按钮现在控制传入的计时器
            Button(modifier = DefaultButtonModifier(), onClick = { viewModel.toggleTimer() }) {
                Text(
                    text = when (timerStatus) {
                        TimerStatus.Idle -> "开始记时"
                        TimerStatus.Running -> "结束记时"
                    }
                )
            }
            Button(modifier = DefaultButtonModifier(), onClick = { /* TODO: 实现撤销逻辑 */ }) { Text(text = "保存") }
            Button(modifier = DefaultButtonModifier(), onClick = { /* TODO: 实现撤销逻辑 */ }) { Text(text = "新牌") }
        }

    }
}

/**
 * 根据数值确定文本颜色的辅助函数。已优化，可以安全处理 null 值。
 */
private fun determineColor(value: Int?): Color {
    if (value == null) return TEXT_COLOR_NEUTRAL
    return if (value in RED_COLOR_VALUES) TEXT_COLOR_B else TEXT_COLOR_P
}

private fun determineColor(value: String?): Color {
    if (value == null) return TEXT_COLOR_NEUTRAL
    return if (value == "B") TEXT_COLOR_B else TEXT_COLOR_P
}

fun Modifier.conditionalBorder(showBorder: Boolean, isWhiteBorder: Boolean): Modifier {
    return if (showBorder) {
        this.border(BorderStroke(BORDER_SIZE, if (isWhiteBorder) Color.White else Color.LightGray))
    } else {
        this
    }
}

// 预览1：预览计数器
@Preview(showBackground = true)
@Composable
private fun CounterDisplayPreview() {
    CounterDisplay(
        label1 = "W", value1 = 15, color1 = TEXT_COLOR_B,
        label2 = "L", value2 = 20, color2 = TEXT_COLOR_P,
        isShowWsr = true,
        isHistory = true
    )
}

// 预览2：预览带图表的 Bppc 表格
@Preview(showBackground = true)
@Composable
private fun BppcTableWithChartsPreview() {
    val synchronizedListState = rememberLazyListState()
    val mockBppcData = remember {
        val realItems = listOf(
            TableDisplayItem.Real(TableItem(dataA = 1, dataB = 2, dataC = 3)),
            TableDisplayItem.Real(TableItem(dataA = 4, dataB = 5, dataC = null)),
            TableDisplayItem.Real(TableItem(dataA = 7, dataB = 8, dataC = 1))
        )
        val emptyItems = List(MIN_TABLE_COLUMN_COUNT - realItems.size) { TableDisplayItem.Empty }
        realItems + emptyItems
    }
    Table(items = mockBppcData, listState = synchronizedListState, showCharts = true)
}


// 预览3：预览策略区块
@Preview(showBackground = true)
@Composable
private fun Strategy3WaysPreview() {
    val synchronizedListState = rememberLazyListState()
    val displayItems1 = remember {
        listOf(
            Strategy3WyasDisplayItem.Real(Strategy3WyasItem(first = 1, second = 2)),
            Strategy3WyasDisplayItem.Real(Strategy3WyasItem(first = 3, second = null))
        )
    }
    val displayItems2 = remember {
        listOf(
            Strategy3WyasDisplayItem.Real(Strategy3WyasItem(first = 3, second = 2)),
            Strategy3WyasDisplayItem.Real(Strategy3WyasItem(first = 1, second = null))
        )
    }
    Strategy3WaysDisplay(
        titles = listOf("A", "12", "56"),
        predictedIndex = "2",
        predictedValue1 = "P",
        predictedValue2 = "B",
        displayItems1 = displayItems1,
        displayItems2 = displayItems2,
        listState = synchronizedListState,
    )
}

@Preview(showBackground = true)
@Composable
private fun TextItemPreveiw() {
    Column {
        TextItem("B", determineColor("B"), isObslate = true, width = TITLE_WIDTH_SHORT)
        TextItem("B", determineColor("B"), isObslate = true, width = TITLE_WIDTH_SHORT)
        TextItem("B", determineColor("B"), isObslate = true, width = TITLE_WIDTH_SHORT)
    }
}

