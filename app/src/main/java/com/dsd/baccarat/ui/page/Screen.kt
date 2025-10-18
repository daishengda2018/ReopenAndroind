package com.dsd.baccarat.ui.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
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
import com.dsd.baccarat.data.BpCounter
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem
import com.dsd.baccarat.data.InputViewModel
import com.dsd.baccarat.data.StrategeDisplayItem
import com.dsd.baccarat.data.StrategyData
import com.dsd.baccarat.data.StrategyItem

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

@Preview(showBackground = true, device = "id:pixel_tablet")
@Composable
private fun Demo() {
    val counter = remember { BpCounter(12, 13) }
    val bppcDisplayList = remember {
        // 1. 初始化原始 items 列表
        val originalItems = listOf(
            BppcItem(1, 2, 3),
            BppcItem(4, 5, 6),
            BppcItem(7, 8, 1),
        )
        // 2. 基于原始 items 计算 displayItems
        val emptyCount = (MIN_TABLE_COLUMN_COUNT - originalItems.size).coerceAtLeast(0)
        val derivedDisplayItems = originalItems
            .map { BppcDisplayItem.Real(it) } + List(emptyCount) { BppcDisplayItem.Empty }

        // 3. 返回组合结果（Pair）
        derivedDisplayItems
    }

    val strategyDisplayList = remember {
        // 1. 初始化原始 items 列表
        val strategyData = StrategyData(1, 2, 3, 4)
        val originalItems = listOf(
            StrategyItem(strategyData, strategyData),
            StrategyItem(strategyData, strategyData),
        )
        // 2. 基于原始 items 计算 displayItems
        val emptyCount = (MIN_TABLE_COLUMN_COUNT - originalItems.size).coerceAtLeast(0)
        val derivedDisplayItems = originalItems
            .map { StrategeDisplayItem.Real(it) } + List(emptyCount) { StrategeDisplayItem.Empty }

        // 3. 返回组合结果（Pair）
        derivedDisplayItems
    }

    val listState = rememberLazyListState()
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(Modifier.fillMaxSize()) {
                LeftSide(bppcDisplayList, strategyDisplayList, counter, listState)
                RightSide(bppcDisplayList, strategyDisplayList, counter, listState)
                {
                    InputButtons(null)
                }
            }
        }
    }
}

/**
 * 表示应用主屏幕的可组合函数。
 *
 * @param viewModel The [InputViewModel] that provides the state and business logic for the screen.
 * @param listState The [LazyListState] used to manage the state of a lazy list within the UI.
 */
@Composable
fun Screen(viewModel: InputViewModel, listState: LazyListState) {
    Scaffold { innerPadding ->
        val bppcDataList = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
        val strategeDataList = viewModel.aStrategyStateFlow.collectAsStateWithLifecycle().value
        val bppcCounter = viewModel.bppcCounterStateFlow.collectAsStateWithLifecycle().value

        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LeftSide(bppcDataList, strategeDataList, bppcCounter)
            RightSide(bppcDataList, strategeDataList, bppcCounter)
            {
                InputButtons(viewModel)
            }
        }
    }
}

@Composable
private fun LeftSide(
    bppcDisplayList: List<BppcDisplayItem>,
    strategyList: List<StrategeDisplayItem>,
    bppcCounter: BpCounter,
    listState: LazyListState = rememberLazyListState()
) {
    Column(
        Modifier
            .fillMaxWidth(0.5f)
            .padding(horizontal = 5.dp)
    ) {
        BPPCounter(bppcCounter)
        Row(Modifier.fillMaxWidth()) {
            Titles()
            Spacer(Modifier.width(SPACE_SIZE))
            TableAndChart(bppcDisplayList, listState)
        }

        val strategyA = remember { listOf("A", "12", "56") }
        val strategyB = remember { listOf("B", "12", "56") }
        val strategyC = remember { listOf("C", "12", "56") }

        Strategy(strategyA, strategyList)
        Strategy(strategyB, strategyList)
        Strategy(strategyC, strategyList)
    }
}

@Composable
fun RightSide(
    bppcDisplayList: List<BppcDisplayItem>,
    strategyList: List<StrategeDisplayItem>,
    bppcCounter: BpCounter,
    listState: LazyListState = rememberLazyListState(),
    itemContent: @Composable (ColumnScope.() -> Unit)
) {
    Column(Modifier.padding(horizontal = 5.dp)) {
        WLCounter(bppcCounter)
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SPACE_SIZE)
        ) {
            itemsIndexed(
                items = bppcDisplayList,
                key = { index, item -> "$index-${item.hashCode()}" }
            ) { idx, item ->
                val dataPoints = (item as? BppcDisplayItem.Real)?.data?.let {
                    listOf(it.dataA, it.dataB, it.dataC)
                } ?: listOf(0, 0, 0)

                Column(Modifier.width(ITEM_SIZE)) {
                    TextItem("${idx + 1}", TEXT_COLOR_NEUTRAL, fontSize = 10.sp)
                    dataPoints.forEach { data ->
                        TextItem(
                            text = if (data == 0) "" else "$data",
                            color = determineColor(data)
                        )
                    }
                }
            }
        }


        Box(
            Modifier
                .fillMaxWidth()
                .height(TABLE_HEIGHT * 3 + SPACE_SIZE * 4)
        ) {
            // 占位，保持布局对齐
        }
        val strategy = remember { listOf("", "34", "78") }
        Strategy(strategy, strategyList)
        Strategy(strategy, strategyList)
        Strategy(strategy, strategyList)
        itemContent()
    }
}

@Composable
fun BPPCounter(counter: BpCounter) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = ITEM_SIZE + SPACE_SIZE)
            .height(ITEM_SIZE)
    ) {
        TextItem("B${counter.bCount}", TEXT_COLOR_B, width = TITLE_WIDTH_SHORT)
        TextItem("P${counter.pCount}", TEXT_COLOR_P, width = TITLE_WIDTH_SHORT)
        TextItem("Total ${counter.bCount + counter.pCount}", Color.Black, width = TITLE_WIDTH_LONG)
    }
}

@Composable
fun WLCounter(counter: BpCounter) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .height(ITEM_SIZE)
    ) {
        TextItem("W${counter.bCount}", TEXT_COLOR_B, width = TITLE_WIDTH_SHORT)
        TextItem("L${counter.pCount}", TEXT_COLOR_P, width = TITLE_WIDTH_SHORT)
        TextItem("Total ${counter.bCount + counter.pCount}", Color.Black, width = TITLE_WIDTH_LONG)
    }
}

@Composable
private fun Titles() {
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

@Composable
private fun TableAndChart(items: List<BppcDisplayItem>, listState: LazyListState) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(
            items = items,
            key = { index, item -> "$index-${item.hashCode()}" }
        ) { idx, item ->
            val dataPoints = (item as? BppcDisplayItem.Real)?.data?.let {
                listOf(it.dataA, it.dataB, it.dataC)
            } ?: listOf(0, 0, 0)

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem("${idx + 1}", TEXT_COLOR_NEUTRAL, fontSize = 10.sp)

                dataPoints.forEach { data ->
                    TextItem(
                        text = if (data == 0) "" else "$data",
                        color = determineColor(data)
                    )
                }

                Spacer(Modifier.height(SPACE_SIZE))
                dataPoints.forEach { data ->
                    VerticalBar(
                        value = data,
                        color = determineColor(data),
                        textMeasurer = rememberTextMeasurer()
                    )
                    Spacer(Modifier.height(SPACE_SIZE))
                }
            }
        }
    }
}

private fun determineColor(value: Int): Color {
    return if (value in RED_COLOR_VALUES) TEXT_COLOR_B else TEXT_COLOR_P
}

@Composable
fun VerticalBar(value: Int, color: Color, textMeasurer: TextMeasurer) {
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
            // 绘制网格线
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

            if (value > 0) {
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

@Composable
fun Strategy(
    title: List<String>, items: List<StrategeDisplayItem>,
    listState: LazyListState = rememberLazyListState()
) {
    Spacer(Modifier.width(SPACE_SIZE))
    Row(Modifier.fillMaxWidth()) {
        if (title[0].isNotEmpty()) {
            StrategyTitles(title[0])
        }

        Spacer(Modifier.width(SPACE_SIZE))
        Column {
            Row {
                TextItem(title[1], width = TITLE_WIDTH_SHORT)
                TextItem(title[2], width = TITLE_WIDTH_SHORT)
            }
            StrategyMap(listState, items)
            Spacer(Modifier.height(SPACE_SIZE))
        }
    }
}

@Composable
private fun StrategyMap(listState: LazyListState, items: List<StrategeDisplayItem>) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(
            items = items,
            key = { index, item -> "$index-${item.hashCode()}" }
        ) { idx, item ->
            val dataPoints = (item as? StrategeDisplayItem.Real)?.data?.let {
                listOf(it.strategy1, it.strategy2)
            } ?: listOf(0, 0)

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem(
                    text = if (dataPoints[0] == 0) "" else "${dataPoints[0]}",
                    color = determineColor(dataPoints[0])
                )
                TextItem(
                    text = if (dataPoints[1] == 0) "" else "${dataPoints[1]}",
                    color = determineColor(dataPoints[1])
                )
            }
        }
    }
}

@Composable
fun StrategyTitles(title: String) {
    Column(Modifier.width(ITEM_SIZE)) {
        Spacer(Modifier.height(ITEM_SIZE + SPACE_SIZE))
        TextItem(title, TEXT_COLOR_NEUTRAL)
    }
}

@Composable
fun TextItem(
    text: String,
    color: Color = Color.Black,
    width: Dp = ITEM_SIZE,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    onClick: (() -> Unit)? = null
) {
    val borderStroke = remember { BorderStroke(BORDER, Color.LightGray) }

    Box(
        Modifier
            .width(width)
            .height(ITEM_SIZE)
            .border(borderStroke)
            .clickable {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun InputButtons(viewModel: InputViewModel?) {
    @Composable
    fun RowScope.DefaultModifier(): Modifier = Modifier
        .padding(3.dp)
        .height(40.dp)
        .weight(1f)

    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Row {
            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel?.openB() }
            ) {
                Text(text = "开 B")
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel?.openP() }
            ) {
                Text(text = "开 P")
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel?.openP() }
            ) {
                Text(text = "撤销")
            }
        }

        Row {
            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel?.openB() }
            ) {
                Text(text = "押 B", fontSize = 14.sp)
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel?.openP() }
            ) {
                Text(text = "押 P")
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel?.openP() }
            ) {
                Text(text = "撤销")
            }
        }
    }
}

