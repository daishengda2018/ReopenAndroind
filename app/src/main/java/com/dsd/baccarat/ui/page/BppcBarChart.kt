package com.dsd.baccarat.ui.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem


@Composable
fun BppcBarChart(items: List<BppcDisplayItem>, listState: LazyListState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = ITEM_SIZE_HALF)
    ) {
        BarChartTitle()
        Spacer(Modifier.width(ITEM_SIZE_HALF))
        LazyRow(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items) { idx, item ->
                val data = if (item is BppcDisplayItem.Real) item.data else BppcItem()
                ItemBarChart(data)
            }
        }
    }
}

@Composable
private fun BarChartTitle() {
    Column(Modifier.width(ITEM_SIZE)) {
        listOf("A", "B", "C").forEach {
            TextItem(it, Color.Gray)
            Spacer(Modifier.height(PADDING_CHAT_TITLE))
        }
    }
}

@Composable
private fun ItemBarChart(item: BppcItem) {
    Column(Modifier.width(ITEM_SIZE)) {
        listOf(item.dataA, item.dataB, item.dataC).forEach {
            VerticalBar(it)
            Spacer(Modifier.height(ITEM_SIZE_HALF))
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
        Canvas(Modifier.fillMaxSize()) {
            val gridColor = Color.LightGray
            val gridWidth = BORDER.toPx()
            val interval = size.height / MAX_BAR
            // 横线
            for (i in 0..MAX_BAR) {
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
            // 柱子
            val barHeight = (value.toFloat() / MAX_BAR * size.height)
            drawRect(
                color = if (value in listOf(1, 4, 6, 7)) Color.Red else Color.Black,
                topLeft = Offset(size.width / 2 - 1f, size.height - barHeight),
                size = Size(2.dp.toPx(), barHeight)
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