package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.TextItem
import com.dsd.baccarat.ui.game.components.common.determineColor

/**
 * 游戏表格组件（用于显示 BPPC 或 WL 数据）
 *
 * @param items 表格数据项列表
 * @param listState 滚动状态
 * @param showCharts 是否显示图表（BPPC 表格显示，WL 表格不显示）
 * @param showHistory 是否显示历史标记
 * @param modifier 修饰符
 */
@Composable
fun GameTable(
    items: List<TableDisplayItem>,
    listState: LazyListState,
    showCharts: Boolean,
    showHistory: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyRow(
        state = listState,
        modifier = modifier
    ) {
        itemsIndexed(
            items = items,
            key = { index, item ->
                // 使用稳定的 key：基于索引和 item 类型
                when (item) {
                    is TableDisplayItem.Empty -> "empty-$index"
                    is TableDisplayItem.Real -> "real-${item.data.dataA?.second}-${item.data.dataB?.second}-${item.data.dataC?.second}-$index"
                }
            }
        ) { idx, item ->
            TableItem(
                index = idx,
                item = item,
                showCharts = showCharts,
                showHistory = showHistory
            )
        }
    }
}

/**
 * 单个表格项
 */
@Composable
private fun TableItem(
    index: Int,
    item: TableDisplayItem,
    showCharts: Boolean,
    showHistory: Boolean
) {
    val data = (item as? TableDisplayItem.Real)?.data

    Column(
        Modifier.width(ITEM_SIZE)
    ) {
        // 索引行
        TextItem(
            text = "${index + 1}",
            color = Color.Black,
            fontSize = 10.sp
        )

        // 数据行 (A, B, C)
        TextItem(
            text = data?.dataA?.second?.toString() ?: "",
            color = determineColor(data?.dataA?.second),
            isHistory = if (showHistory) { data?.dataA?.first ?: false } else false
        )
        TextItem(
            text = data?.dataB?.second?.toString() ?: "",
            color = determineColor(data?.dataB?.second),
            isHistory = if (showHistory) { data?.dataB?.first ?: false } else false
        )
        TextItem(
            text = data?.dataC?.second?.toString() ?: "",
            color = determineColor(data?.dataC?.second),
            isHistory = if (showHistory) { data?.dataC?.first ?: false } else false
        )

        // 显示图表
        if (showCharts) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(5.dp))
            VerticalBarChart(value = data?.dataA)
            androidx.compose.foundation.layout.Spacer(Modifier.height(5.dp))
            VerticalBarChart(value = data?.dataB)
            androidx.compose.foundation.layout.Spacer(Modifier.height(5.dp))
            VerticalBarChart(value = data?.dataC)
        }
    }
}
