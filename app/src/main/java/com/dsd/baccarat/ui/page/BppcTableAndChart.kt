package com.dsd.baccarat.ui.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.BpCounter
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem
import kotlin.collections.indexOfLast

val ITEM_SIZE = 25.dp
val TABLE_SPACE = 5.dp
val TABLE_HEIGHT = ITEM_SIZE * 4
val TOTLE_HEIGHT = (TABLE_HEIGHT * 4) + (TABLE_SPACE * 3)
const val MIN_COUNT = 25
val BORDER = 0.3.dp
const val MAX_VALUE = 8

val TEXT_COLOR_B = Color.Red
val TEXT_COLOR_P = Color.Blue

@Preview(showBackground = true)
@Composable
private fun Demo() {
    val items = listOf(
        BppcItem(1, 2, 3),
        BppcItem(4, 5, 6),
        BppcItem(7, 8, 1),
    )
    val emptyCount = (MIN_COUNT - items.size).coerceAtLeast(0)
    val displayItems: List<BppcDisplayItem> = items
        .map { BppcDisplayItem.Real(it) } // 实际数据转换为 Real 项
        .plus(List(emptyCount) { BppcDisplayItem.Empty }) // 补充 Empty 占位项
    val counter = BpCounter(12, 13)

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            BppcTableAndChart(displayItems, counter)
        }
    }
}

@Composable
fun BppcTableAndChart(displayItems: List<BppcDisplayItem>, counter: BpCounter) {
    val listState = rememberLazyListState()
    LaunchedEffect(displayItems) {
        // 自动滚动到最后一个实际数据项，确保最新数据可见
        val visibleCount = listState.layoutInfo.visibleItemsInfo.size
        // 找到最后一个 Real 项的索引
        val lastRealIndex = displayItems.indexOfLast { it is BppcDisplayItem.Real }
        // 只有当最后一个 Real 项不在可见范围内时才滚动
        if ((lastRealIndex + 1) > visibleCount) {
            listState.scrollToItem(displayItems.lastIndex)
        }
    }

    Column(
        Modifier
            .fillMaxWidth(0.5f)
            .padding(horizontal = 5.dp)
    )
    {
        Counter(counter)
        Row(
            Modifier
                .fillMaxWidth()
                .height(TOTLE_HEIGHT)
        ) {
            Titles()
            Spacer(Modifier.width(TABLE_SPACE))
            TableAndChart(displayItems, listState)
        }
    }
}

@Composable
fun Counter(counter: BpCounter) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ITEM_SIZE)
    ) {
        TextItem("B${counter.bCount}", TEXT_COLOR_B, width = (ITEM_SIZE + TABLE_SPACE), fontWeight = FontWeight.Normal)
        TextItem("P${counter.pCount}", Color.Blue, width = ITEM_SIZE * 2, fontWeight = FontWeight.Normal)
        TextItem("Total ${counter.bCount + counter.pCount}", Color.Black, width = ITEM_SIZE * 3, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun Titles() {
    Column(Modifier.width(ITEM_SIZE)) {
        listOf("\\", "A", "B", "C").forEach {
            TextItem(it, Color.Gray)
        }

        Spacer(Modifier.height(5.dp))

        listOf("A", "B", "C").forEach {
            TextItem(it, Color.Gray)
            Spacer(Modifier.height(ITEM_SIZE * 3 + TABLE_SPACE))
        }
    }
}

@Composable
private fun TableAndChart(items: List<BppcDisplayItem>, listState: LazyListState) {
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(items) { idx, item ->
            Column(Modifier.width(ITEM_SIZE)) {
                TextItem("${idx + 1}", Color.Gray)

                listOf(
                    if (item is BppcDisplayItem.Real) item.data.dataA else 0,
                    if (item is BppcDisplayItem.Real) item.data.dataB else 0,
                    if (item is BppcDisplayItem.Real) item.data.dataC else 0
                ).forEach { data ->
                    TextItem(
                        if (data == 0) "" else "$data",
                        if (data in listOf(1, 4, 6, 7)) TEXT_COLOR_B else TEXT_COLOR_P
                    )
                }

                listOf(
                    if (item is BppcDisplayItem.Real) item.data.dataA else 0,
                    if (item is BppcDisplayItem.Real) item.data.dataB else 0,
                    if (item is BppcDisplayItem.Real) item.data.dataC else 0
                ).forEach { data ->
                    Spacer(Modifier.height(TABLE_SPACE))
                    VerticalBar(data)
                }
            }
        }
    }
}

@Composable
fun VerticalBar(value: Int) {
    Box(
        Modifier
            .width(ITEM_SIZE)
            .height(TABLE_HEIGHT)
    ) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize()) {
            val gridColor = Color.LightGray
            val gridWidth = (BORDER * 2).toPx()
            val interval = size.height / MAX_VALUE
            // 横线
            for (i in 0..MAX_VALUE) {
                drawLine(
                    color = if (i == 4) Color.Black else gridColor,
                    start = Offset(0f, i * interval),
                    end = Offset(size.width, i * interval),
                    strokeWidth = gridWidth
                )
            }
            // 竖线
            drawLine(gridColor, Offset(size.width, 0f), Offset(size.width, size.height), gridWidth)
            drawLine(gridColor, Offset(0f, 0f), Offset(0f, size.height), gridWidth)

            val barHeight = (value.toFloat() / MAX_VALUE * size.height)
            val color = if (value in listOf(1, 4, 6, 7)) TEXT_COLOR_B else TEXT_COLOR_P
            // 柱子
            drawRect(
                color = color,
                topLeft = Offset(size.width / 2 - 1f, size.height - barHeight),
                size = Size(2.dp.toPx(), barHeight)
            )
            // 数字
            drawText(
                textMeasurer = textMeasurer,
                text = AnnotatedString("$value"),
                topLeft = Offset(10f, size.height - barHeight),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = color
                )
            )

            // 底边
            drawLine(
                Color.Black,
                Offset(0f, size.height),
                Offset(size.width, size.height),
                gridWidth
            )
        }
    }
}


@Composable
fun TextItem(
    text: String,
    color: Color,
    width: Dp = ITEM_SIZE,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Box(
        Modifier
            .width(width)
            .height(ITEM_SIZE)
            .border(BORDER, Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = color, fontWeight = fontWeight)
    }
}