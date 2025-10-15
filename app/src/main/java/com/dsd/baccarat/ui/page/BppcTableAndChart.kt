package com.dsd.baccarat.ui.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.BpCounter
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem

// --- 常量定义 (为了清晰，进行分组和修正) ---
const val MIN_TABLE_COLUMN_COUNT = 25

private val ITEM_SIZE = 22.dp
private val SPACE_SIZE = 5.dp
private val TABLE_HEIGHT = ITEM_SIZE * 4
private val TOTAL_HEIGHT = (TABLE_HEIGHT * 4) + (SPACE_SIZE * 3) // 修正拼写错误 TOTLE -> TOTAL

private val BORDER = 0.3.dp
private const val MAX_VALUE = 8

private val TEXT_COLOR_B = Color.Red
private val TEXT_COLOR_P = Color.Blue
private val TEXT_COLOR_NEUTRAL = Color.Gray

@Preview(showBackground = true, device = "id:pixel_tablet")
@Composable
private fun Demo() {
    // 使用 remember 确保 items 列表在重组时不会被重新创建
    val items = remember {
        listOf(
            BppcItem(1, 2, 3),
            BppcItem(4, 5, 6),
            BppcItem(7, 8, 1),
        )
    }
    val counter = remember { BpCounter(12, 13) }

    // 【优化1】使用 derivedStateOf，确保只有在 items 变化时才重新计算
    val displayItems by remember(items) {
        derivedStateOf {
            val emptyCount = (MIN_TABLE_COLUMN_COUNT - items.size).coerceAtLeast(0)
            items.map { BppcDisplayItem.Real(it) } + List(emptyCount) { BppcDisplayItem.Empty }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize() // 【优化2】使用 fillMaxSize() 代替 fillMaxWidth().fillMaxHeight()
        ) {
            BppcTableAndChart(displayItems, counter)
        }
    }
}

@Composable
fun BppcTableAndChart(displayItems: List<BppcDisplayItem>, counter: BpCounter) {
    val listState = rememberLazyListState()

    // 【优化3】优化自动滚动逻辑
    LaunchedEffect(displayItems.size) { // 仅在列表大小变化时触发
        val lastRealIndex = displayItems.indexOfLast { it is BppcDisplayItem.Real }
        if (lastRealIndex != -1) {
            // 使用 animateScrollToItem 获得更平滑的滚动动画效果
            listState.animateScrollToItem(displayItems.lastIndex)
        }
    }

    Column(
        Modifier
            .fillMaxWidth(0.5f)
            .padding(horizontal = 5.dp)
    ) {
        Counter(counter)
        Row(Modifier.fillMaxWidth()) // 使用修正后的常量
        {
            Titles()
            Spacer(Modifier.width(SPACE_SIZE))
            TableAndChart(displayItems, listState)
        }

        Strategy(listOf("A","12", "56"), displayItems, listState)
        Strategy(listOf("B","12", "56"), displayItems, listState)
        Strategy(listOf("C","12", "56"), displayItems, listState)
    }
}

@Composable
fun Counter(counter: BpCounter) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ITEM_SIZE)
    ) {
        // 【优化4】移除不必要的默认参数
        TextItem("B${counter.bCount}", TEXT_COLOR_B, width = (ITEM_SIZE + SPACE_SIZE))
        TextItem("P${counter.pCount}", TEXT_COLOR_P, width = ITEM_SIZE * 2)
        TextItem("Total ${counter.bCount + counter.pCount}", Color.Black, width = ITEM_SIZE * 3)
    }
}

@Composable
private fun Titles() {
    Column(Modifier.width(ITEM_SIZE)) {
        // 使用 remember 避免在每次重组时重新创建列表
        val mainTitles = remember { listOf("\\", "A", "B", "C") }
        val subTitles = remember { listOf("A", "B", "C") }

        mainTitles.forEach {
            TextItem(it, TEXT_COLOR_NEUTRAL)
        }

        Spacer(Modifier.height(SPACE_SIZE)) // 使用常量

        subTitles.forEach {
            TextItem(it, TEXT_COLOR_NEUTRAL)
            Spacer(Modifier.height(ITEM_SIZE * 3 + SPACE_SIZE))
        }
    }
}


@Composable
private fun TableAndChart(items: List<BppcDisplayItem>, listState: LazyListState) {
    // 【核心优化】提升 TextMeasurer 状态，避免在每个 item 中重复创建
    val textMeasurer = rememberTextMeasurer()

    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(
            items = items,
            // 【核心优化】为 LazyRow 提供稳定的 key，提升性能
            key = { index, item -> "$index-${item.hashCode()}" }
        ) { idx, item ->
            // 【核心优化】缓存从 item 中提取的数据，避免不必要的重算
            val (dataA, dataB, dataC) = remember(item) {
                if (item is BppcDisplayItem.Real) {
                    Triple(item.data.dataA, item.data.dataB, item.data.dataC)
                } else {
                    Triple(0, 0, 0)
                }
            }
            val dataPoints = remember(dataA, dataB, dataC) { listOf(dataA, dataB, dataC) }

            Column(Modifier.width(ITEM_SIZE)) {

                TextItem("${idx + 1}", TEXT_COLOR_NEUTRAL)
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
                        textMeasurer = textMeasurer
                    )
                    Spacer(Modifier.height(SPACE_SIZE))
                }
            }
        }
    }
}

// 【优化8】提取的纯函数，用于决定颜色
private fun determineColor(value: Int): Color {
    return if (value in listOf(1, 4, 6, 7)) TEXT_COLOR_B else TEXT_COLOR_P
}

@Composable
fun VerticalBar(value: Int, color: Color, textMeasurer: TextMeasurer) {
    // 【优化10】提升 TextStyle 的状态
    // TextStyle 是一个普通类，每次重组时创建它会产生开销。
    // 使用 remember(color) 可以确保只有在颜色变化时才创建新的 TextStyle 实例。
    val textStyle = remember(color) {
        TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = color
        )
    }

    Box(
        Modifier
            .width(ITEM_SIZE)
            .height(TABLE_HEIGHT)
    ) {
        // Canvas 的绘制成本较高，确保它只在输入参数(value, color)变化时才重绘
        Canvas(Modifier.fillMaxSize()) {
            val gridColor = Color.LightGray
            val gridWidth = BORDER.toPx() // 修正 gridWidth 的计算
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
            drawLine(gridColor, Offset(size.width, 0f), Offset(size.width, size.height), gridWidth)
            drawLine(gridColor, Offset(0f, 0f), Offset(0f, size.height), gridWidth)
            drawLine(
                Color.Black,
                Offset(0f, size.height),
                Offset(size.width, size.height),
                gridWidth
            )

            // 【优化9】只有当 value > 0 时才执行绘制操作
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
                    topLeft = Offset(10f, size.height - barHeight),
                    style = textStyle // 使用提升状态后的 textStyle
                )
            }
        }
    }
}

@Composable
fun Strategy(title: List<String>, items: List<BppcDisplayItem>, listState: LazyListState) {
    Spacer(Modifier.width(SPACE_SIZE))
    Row(Modifier.fillMaxWidth()) {
        StrategyTitles(title[0])
        Spacer(Modifier.width(SPACE_SIZE))
        Column {
            Row {
                TextItem(title[1], width = ITEM_SIZE * 2)
                TextItem(title[2], width = ITEM_SIZE * 2)
            }
            StrategyMap(listState, items)
            Spacer(Modifier.height(SPACE_SIZE))
        }
    }
}

@Composable
private fun StrategyMap(
    listState: LazyListState,
    items: List<BppcDisplayItem>
) {
    LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(
            items = items,
            // 【核心优化】为 LazyRow 提供稳定的 key，提升性能
            key = { index, item -> "$index-${item.hashCode()}" }
        ) { idx, item ->
            // 【核心优化】缓存从 item 中提取的数据，避免不必要的重算
            val (dataA, dataB, dataC) = remember(item) {
                if (item is BppcDisplayItem.Real) {
                    Triple(item.data.dataA, item.data.dataB, item.data.dataC)
                } else {
                    Triple(0, 0, 0)
                }
            }
            val dataPoints = remember(dataA, dataB, dataC) { listOf(dataA, dataB, dataC) }

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem("${idx + 1}", TEXT_COLOR_NEUTRAL)
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
        Spacer(Modifier.height(ITEM_SIZE + SPACE_SIZE)) // 使用常量
        TextItem(title, TEXT_COLOR_NEUTRAL)
    }
}

@Composable
fun TextItem(
    text: String,
    color: Color = Color.Black,
    width: Dp = ITEM_SIZE,
    fontWeight: FontWeight = FontWeight.Normal
) {
    // 【优化11】提升 BorderStroke 的状态
    // 与 TextStyle 类似，BorderStroke 也是一个普通类。
    // 使用 remember 将其缓存，可以避免在每次重组时不必要地重新创建此对象。
    val borderStroke = remember { BorderStroke(BORDER , Color.LightGray) }

    Box(
        Modifier
            .width(width)
            .height(ITEM_SIZE)
            .border(borderStroke), // 使用提升状态后的 borderStroke
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = color, fontWeight = fontWeight)
    }
}