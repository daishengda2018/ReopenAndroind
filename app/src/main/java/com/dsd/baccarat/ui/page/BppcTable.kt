package com.dsd.baccarat.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dsd.baccarat.data.BppcDisplayItem


@Composable
fun BppcTable(
    displayItems: List<BppcDisplayItem>,
    listState: LazyListState
) {
    Row(
        Modifier
            .height(TABLE_HEIGHT)
            .fillMaxWidth()
    ) {
        TableTitle()
        Spacer(Modifier.width(ITEM_SIZE_HALF))
        TableLazyRow(displayItems, listState)
    }
}

@Composable
private fun TableTitle() {
    Column(Modifier.width(ITEM_SIZE)) {
        listOf("\\", "A", "B", "C").forEach {
            TextItem(it, Color.Gray)
        }
    }
}

@Composable
private fun TableLazyRow(items: List<BppcDisplayItem>, listState: LazyListState) {
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(TABLE_HEIGHT)
    ) {
        itemsIndexed(items) { idx, item ->
            Column(
                Modifier
                    .width(ITEM_SIZE)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextItem("$idx", Color.Gray)
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
            }
        }
    }
}

