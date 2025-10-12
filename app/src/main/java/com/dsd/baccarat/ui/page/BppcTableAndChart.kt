package com.dsd.baccarat.ui.page

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.BppcItem
import com.dsd.baccarat.data.InputViewModel

val ITEM_SIZE = 25.dp
val ITEM_SIZE_HALF = ITEM_SIZE / 2
val TABLE_HEIGHT = ITEM_SIZE * 4
val PADDING_CHAT_TITLE = ITEM_SIZE * 3 + ITEM_SIZE_HALF
const val MIN_COUNT = 25
val BORDER = 0.3.dp
const val MAX_BAR = 8

@Preview(showBackground = true)
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
    val realData = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
    // 2. 计算需要补充的空占位数量
    val emptyCount = (MIN_COUNT - realData.size).coerceAtLeast(0)
    // 3. 混合实际数据项和占位项
    val displayItems: List<BppcDisplayItem> = realData
        .map { BppcDisplayItem.Real(it) } // 实际数据转换为 Real 项
        .plus(List(emptyCount) { BppcDisplayItem.Empty }) // 补充 Empty 占位项

    InternalTableAndChart(displayItems)
}

@Composable
private fun InternalTableAndChart(displayItems: List<BppcDisplayItem>) {
    val listState = rememberLazyListState()
    LaunchedEffect(displayItems.size) {
        if (displayItems.size >= MIN_COUNT) listState.scrollToItem(displayItems.lastIndex)
    }

    Column(Modifier.fillMaxWidth(0.5f)) {
        BppcTable(displayItems, listState)
        BppcBarChart(displayItems, listState)
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