package com.dsd.baccarat.ui.page

import android.R.attr.fontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem
import com.dsd.baccarat.data.InputViewModel

val ITEM_SIZE = 25.dp
val ITEM_SIZE_HALF = ITEM_SIZE / 2
val TABLE_HEIGHT = ITEM_SIZE * 4
val TOTLE_HEIGHT = (TABLE_HEIGHT * 4) + (ITEM_SIZE_HALF * 3)
val PADDING_CHAT_TITLE = ITEM_SIZE * 3 + ITEM_SIZE_HALF
const val MIN_COUNT = 25
val BORDER = 0.3.dp
const val MAX_VALUE = 8

@Composable
private fun Demo() {
    val items = listOf(
        BppcItem(1, 2, 3),
        BppcItem(4, 5, 6),
        BppcItem(7, 8, 9),
    )
    val emptyCount = (MIN_COUNT - items.size).coerceAtLeast(0)
    val displayItems: List<BppcDisplayItem> = items
        .map { BppcDisplayItem.Real(it) } // 实际数据转换为 Real 项
        .plus(List(emptyCount) { BppcDisplayItem.Empty }) // 补充 Empty 占位项

    InternalTableAndChart(displayItems)
}

@Composable
fun BppcTableAndChart(viewModel: InputViewModel) {
    // 1. 收集ViewModel中的实际数据（随生命周期自动管理）
    val displayItems = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
    InternalTableAndChart(displayItems)
}

@Composable
private fun InternalTableAndChart(displayItems: List<BppcDisplayItem>) {
    val listState = rememberLazyListState()
    LaunchedEffect(displayItems.size) {
        if (displayItems.size > MIN_COUNT) listState.scrollToItem(displayItems.lastIndex)
    }

    Row(
        Modifier
            .fillMaxWidth(0.5f)
            .height(TOTLE_HEIGHT)
    ) {
        TableTitle()
        TableLazyRow(displayItems, listState)
    }
}


@Composable
private fun TableTitle() {
    Column(Modifier.width(ITEM_SIZE)) {
        listOf("\\", "A", "B", "C").forEach {
            TextItem(it, Color.Gray)
        }

        Spacer(Modifier.height(ITEM_SIZE_HALF))

        listOf("A", "B", "C").forEach {
            TextItem(it, Color.Gray)
            Spacer(Modifier.height(PADDING_CHAT_TITLE))
        }
    }
}

@Composable
private fun TableLazyRow(items: List<BppcDisplayItem>, listState: LazyListState) {
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
                        if (data in listOf(1, 4, 6, 7)) Color.Red else Color.Black
                    )
                }

                listOf(
                    if (item is BppcDisplayItem.Real) item.data.dataA else 0,
                    if (item is BppcDisplayItem.Real) item.data.dataB else 0,
                    if (item is BppcDisplayItem.Real) item.data.dataC else 0
                ).forEach { data ->
                    Spacer(Modifier.height(ITEM_SIZE_HALF))
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
            val gridWidth = BORDER.toPx()
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
            val color = if (value in listOf(1, 4, 6, 7)) Color.Red else Color.Black
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
    fontWeight: FontWeight = FontWeight.Normal
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(ITEM_SIZE)
            .border(BORDER, Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = color, fontWeight = fontWeight)
    }
}