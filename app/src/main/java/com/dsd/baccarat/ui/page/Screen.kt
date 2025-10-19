package com.dsd.baccarat.ui.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem
import com.dsd.baccarat.data.InputViewModel
import com.dsd.baccarat.data.StrategyDisplayItem
import com.dsd.baccarat.data.StrategyItem
import com.dsd.baccarat.ui.theme.PurpleGrey80
import java.text.DecimalFormat

// [优化点] 常量在顶部统一组织，清晰明了。
const val MIN_TABLE_COLUMN_COUNT = 30

private val ITEM_SIZE = 22.dp
private val SPACE_SIZE = 5.dp
private val TABLE_HEIGHT = ITEM_SIZE * 4
private val TITLE_WIDTH_SHORT = ITEM_SIZE * 3
private val TITLE_WIDTH_LONG = ITEM_SIZE * 4
private val BORDER = 0.3.dp
private const val MAX_VALUE = 8
private val TEXT_COLOR_B = Color.Red
private val TEXT_COLOR_P = Color.Blue
private val TEXT_COLOR_NEUTRAL = Color.Black

private val RED_COLOR_VALUES = setOf(1, 4, 6, 7)

/**
 * 应用的主屏幕可组合函数。
 */
@Composable
fun Screen(viewModel: InputViewModel) {
    // [优化点] 在此处进行状态提升 (State Hoisting)。这个唯一的 listState 实例将被传递给所有
    // LazyRow，从而确保它们同步滚动。这是解决同步滚动的关键。
    val synchronizedListState = rememberLazyListState()
    // [优化点] 在顶层统一收集所有 StateFlow 状态。
    val bppcTableData = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
    val bppcCounter = viewModel.bppcCounterStateFlow.collectAsStateWithLifecycle().value
    val aStrategyData = viewModel.aStrategyStateFlow.collectAsStateWithLifecycle().value
    val bStrategyData = viewModel.bStrategyStateFlow.collectAsStateWithLifecycle().value
    val cStrategyData = viewModel.cStrategyStateFlow.collectAsStateWithLifecycle().value
    val aPredictedValue = viewModel.aPredictionStateFlow.collectAsStateWithLifecycle().value
    val bPredictedValue = viewModel.bPredictionStateFlow.collectAsStateWithLifecycle().value
    val cPredictedValue = viewModel.cPredictionStateFlow.collectAsStateWithLifecycle().value

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
            Column(
                Modifier
                    .weight(1f) // 使用 weight 实现灵活的权重布局
                    .padding(horizontal = 5.dp)
            ) {
                CounterDisplay(
                    label1 = "B", value1 = bppcCounter.bCount, color1 = TEXT_COLOR_B,
                    label2 = "P", value2 = bppcCounter.pCount, color2 = TEXT_COLOR_P
                )
                Row(Modifier.fillMaxWidth()) {
                    BppcTableTitles()
                    Spacer(Modifier.width(SPACE_SIZE))
                    Table(
                        items = bppcTableData,
                        listState = synchronizedListState,
                        showCharts = true // 这一列显示图表
                    )
                }
                // 左侧列的策略区块
                StrategySection(
                    titles = listOf("A", "12", "56"),
                    predictedIndex = aPredictedValue.predictionIndex,
                    predictedValue1 = aPredictedValue.strategy12,
                    predictedValue2 = aPredictedValue.strategy56,
                    displayItems1 = aStrategyData.strategy12,
                    displayItems2 = aStrategyData.strategy56,
                    listState = synchronizedListState
                )
                StrategySection(
                    titles = listOf("B", "12", "56"),
                    predictedIndex = bPredictedValue.predictionIndex,
                    predictedValue1 = bPredictedValue.strategy12,
                    predictedValue2 = bPredictedValue.strategy56,
                    displayItems1 = bStrategyData.strategy12,
                    displayItems2 = bStrategyData.strategy56,
                    listState = synchronizedListState,
                )
                StrategySection(
                    titles = listOf("C", "12", "56"),
                    predictedIndex = cPredictedValue.predictionIndex,
                    predictedValue1 = cPredictedValue.strategy12,
                    predictedValue2 = cPredictedValue.strategy56,
                    displayItems1 = cStrategyData.strategy12,
                    displayItems2 = cStrategyData.strategy56,
                    listState = synchronizedListState,
                )
            }

            // 右侧列
            Column(
                Modifier
                    .weight(1f) // 使用 weight 实现灵活的权重布局
                    .padding(horizontal = 5.dp)
            ) {
                CounterDisplay(
                    label1 = "W", value1 = bppcCounter.bCount, color1 = TEXT_COLOR_B,
                    label2 = "L", value2 = bppcCounter.pCount, color2 = TEXT_COLOR_P,
                    true
                )
                // [优化点] 复用 BppcTable 组件。
                Table(
                    items = bppcTableData,
                    listState = synchronizedListState,
                    showCharts = false // 这一列不显示图表
                )
                // 用 Spacer 来与左侧的图表区域在布局上对齐
                Spacer(Modifier.height(TABLE_HEIGHT * 3 + SPACE_SIZE * 3))

                // 右侧列的策略区块
                StrategySection(
                    titles = listOf("", "34", "78"),
                    predictedIndex = aPredictedValue.predictionIndex,
                    predictedValue1 = aPredictedValue.strategy12,
                    predictedValue2 = aPredictedValue.strategy56,
                    displayItems1 = aStrategyData.strategy34,
                    displayItems2 = aStrategyData.strategy78,
                    listState = synchronizedListState,
                )
                StrategySection(
                    titles = listOf("", "34", "78"),
                    predictedIndex = bPredictedValue.predictionIndex,
                    predictedValue1 = bPredictedValue.strategy12,
                    predictedValue2 = bPredictedValue.strategy56,
                    displayItems1 = bStrategyData.strategy34,
                    displayItems2 = bStrategyData.strategy78,
                    listState = synchronizedListState,
                )
                StrategySection(
                    titles = listOf("", "34", "78"),
                    predictedIndex = cPredictedValue.predictionIndex,
                    predictedValue1 = cPredictedValue.strategy12,
                    predictedValue2 = cPredictedValue.strategy56,
                    displayItems1 = cStrategyData.strategy34,
                    displayItems2 = cStrategyData.strategy78,
                    listState = synchronizedListState,
                )

                // 使用一个带权重的 Spacer 将按钮推到底部
                Spacer(Modifier.weight(1f))
                InputButtons(
                    onOpenB = { viewModel.openB() },
                    onOpenP = { viewModel.openP() },
                    onBetB = { viewModel.betB() },
                    onBetP = { viewModel.betP() }
                )
            }
        }
    }
}

/**
 * [优化点] 将 BPPCounter 和 WLCounter 合并为一个可复用的 CounterDisplay 可组合函数。
 */
@Composable
private fun CounterDisplay(
    label1: String, value1: Int, color1: Color,
    label2: String, value2: Int, color2: Color,
    isShowWsr: Boolean = false
) {
    val total = value1 + value2
    val df = remember { DecimalFormat("0.00%") }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = ITEM_SIZE + SPACE_SIZE)
            .height(ITEM_SIZE)
    ) {
        TextItem("$label1$value1", color1, width = TITLE_WIDTH_SHORT)
        TextItem("$label2$value2", color2, width = TITLE_WIDTH_SHORT)
        TextItem("Total $total", Color.Black, width = TITLE_WIDTH_LONG)
        if (isShowWsr) {
            val wsr = value1 / total.toFloat()
            TextItem("WSR ${df.format(wsr)}", Color.Magenta, width = TITLE_WIDTH_LONG)
        }
    }
}

@Composable
private fun BppcTableTitles() {
    Column(Modifier.width(ITEM_SIZE)) {
        listOf("\\", "A", "B", "C").forEach { TextItem(it, TEXT_COLOR_NEUTRAL) }
    }
}

/**
 * [优化点] 合并了 TableAndChart 和 WsrTable。现在通过参数控制是否显示图表，增强了复用性。
 */
@Composable
private fun Table(
    items: List<BppcDisplayItem>,
    listState: LazyListState,
    showCharts: Boolean
) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(
            items = items,
            key = { index, item -> "$index-${item.hashCode()}" }
        ) { idx, item ->
            val data = (item as? BppcDisplayItem.Real)?.data
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
 * [修复] 一个全新的、独立的“策略区块”可组合函数，用于显示一组策略。
 */
@Composable
private fun StrategySection(
    titles: List<String>,
    predictedIndex: String?,
    predictedValue1: String?,
    predictedValue2: String?,
    displayItems1: List<StrategyDisplayItem>,
    displayItems2: List<StrategyDisplayItem>,
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
                1 -> StrategyTable(listState, displayItems1)
                2 -> StrategyTable(listState, displayItems2)
            }
        }
    }
}

/**
 * [修复] 这是 StrategyMap 完全重写和修复后的版本。
 * 它现在能够正确地显示每个数据列对应的两行策略数据。
 */
@Composable
private fun StrategyTable(
    listState: LazyListState,
    displayItems: List<StrategyDisplayItem>
) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        items(
            count = displayItems.size,
            key = { index -> index }
        ) { idx ->
            val item = (displayItems.getOrNull(idx) as? StrategyDisplayItem.Real)?.data

            Column(Modifier.width(ITEM_SIZE)) {
                // 第一行 (例如 12 或 34)
                TextItem(
                    text = item?.strategy1?.toString() ?: "",
                    color = determineColor(item?.strategy1)
                )
                TextItem(
                    text = item?.strategy2?.toString() ?: "",
                    color = determineColor(item?.strategy2)
                )
            }
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

/**
 * [优化点] 将 Canvas 绘制逻辑提取到独立的可组合函数中，使 BppcTable 更简洁。
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
            val gridWidth = BORDER.toPx()
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
    onClick: (() -> Unit)? = null
) {
    // 步骤 1: 根据 isSelected 状态决定背景色和文字颜色
    val backgroundColor = remember(isSelected)
    {
        if (isSelected) {
            PurpleGrey80 // 选中时，使用主题色的淡色作为背景
        } else {
            Color.Transparent // 未选中时，背景透明
        }
    }

    Box(
        Modifier
            .width(width)
            .height(ITEM_SIZE)
            .border(BorderStroke(BORDER, Color.LightGray))
            .background(backgroundColor)
            .clickable {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, fontSize = fontSize, color = color, fontWeight = fontWeight
        )
    }
}

/**
 * [优化点] 输入按钮现在通过 lambda 表达式接收回调，从而与 ViewModel 解耦，提升了组件的独立性。
 */
@Composable
fun InputButtons(
    onOpenB: () -> Unit,
    onOpenP: () -> Unit,
    onBetB: () -> Unit,
    onBetP: () -> Unit,
    // onUndo: () -> Unit // 如果需要，后续可添加 onUndo 回调
) {
    @Composable
    fun RowScope.DefaultButtonModifier(): Modifier = Modifier
        .padding(3.dp)
        .height(40.dp)
        .weight(1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            Button(modifier = DefaultButtonModifier(), onClick = onOpenB) { Text(text = "开 B") }
            Button(modifier = DefaultButtonModifier(), onClick = onOpenP) { Text(text = "开 P") }
            Button(modifier = DefaultButtonModifier(), onClick = { /* TODO: 实现撤销逻辑 */ }) { Text(text = "撤销") }
        }
        Row {
            Button(modifier = DefaultButtonModifier(), onClick = onBetB) { Text(text = "押 B") }
            Button(modifier = DefaultButtonModifier(), onClick = onBetP) { Text(text = "押 P") }
            Button(modifier = DefaultButtonModifier(), onClick = { /* TODO: 实现撤销逻辑 */ }) { Text(text = "撤销") }
        }
    }
}

// 预览1：预览计数器
@Preview(showBackground = true)
@Composable
private fun CounterDisplayPreview() {
    CounterDisplay(
        label1 = "W", value1 = 15, color1 = TEXT_COLOR_B,
        label2 = "L", value2 = 20, color2 = TEXT_COLOR_P,
        true
    )
}

// 预览2：预览带图表的 Bppc 表格
@Preview(showBackground = true)
@Composable
private fun BppcTableWithChartsPreview() {
    val synchronizedListState = rememberLazyListState()
    val mockBppcData = remember {
        val realItems = listOf(
            BppcDisplayItem.Real(BppcItem(dataA = 1, dataB = 2, dataC = 3)),
            BppcDisplayItem.Real(BppcItem(dataA = 4, dataB = 5, dataC = null)),
            BppcDisplayItem.Real(BppcItem(dataA = 7, dataB = 8, dataC = 1))
        )
        val emptyItems = List(MIN_TABLE_COLUMN_COUNT - realItems.size) { BppcDisplayItem.Empty }
        realItems + emptyItems
    }
    Table(items = mockBppcData, listState = synchronizedListState, showCharts = true)
}


// 预览3：预览策略区块
@Preview(showBackground = true)
@Composable
private fun StrategySectionPreview() {
    val synchronizedListState = rememberLazyListState()
    val displayItems1 = remember {
        listOf(
            StrategyDisplayItem.Real(StrategyItem(strategy1 = 1, strategy2 = 2)),
            StrategyDisplayItem.Real(StrategyItem(strategy1 = 3, strategy2 = null))
        )
    }
    val displayItems2 = remember {
        listOf(
            StrategyDisplayItem.Real(StrategyItem(strategy1 = 3, strategy2 = 2)),
            StrategyDisplayItem.Real(StrategyItem(strategy1 = 1, strategy2 = null))
        )
    }
    StrategySection(
        titles = listOf("A", "12", "56"),
        predictedIndex = "2",
        predictedValue1 = "P",
        predictedValue2 = "B",
        displayItems1 = displayItems1,
        displayItems2 = displayItems2,
        listState = synchronizedListState,
    )
}