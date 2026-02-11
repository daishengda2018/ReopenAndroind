package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TableType
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.TextItem
import com.dsd.baccarat.ui.game.components.common.ThickSeparator
import com.dsd.baccarat.ui.game.components.common.ThinSeparator
import com.dsd.baccarat.ui.game.components.common.determineColor
import com.dsd.baccarat.ui.game.state.TableMetadata

/**
 * 游戏表格组件（用于显示 BPPC 或 WL 数据）
 *
 * @param items 表格数据项列表
 * @param listState 滚动状态
 * @param tableType 表格类型（BP 或 WL）
 * @param tableMetadata 表格元数据
 * @param showCharts 是否显示图表（BPPC 表格显示，WL 表格不显示）
 * @param showHistory 是否显示历史标记
 * @param modifier 修饰符
 */
@Composable
fun GameTable(
    items: List<TableDisplayItem>,
    listState: LazyListState,
    tableType: TableType = TableType.BP,
    tableMetadata: TableMetadata = TableMetadata(),
    showCharts: Boolean = false,
    showHistory: Boolean = false,
    previousItems: List<TableDisplayItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column {
        // Table number display
        if (tableMetadata.tableNumber > 0) {
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                modifier = Modifier
            ) {
                if (previousItems.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = "Prev: ${(tableMetadata.tableNumber - 1).toString().padStart(5, '0')}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                androidx.compose.material3.Text(
                    text = tableMetadata.displayNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

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
                // Add separators at columns 5, 10, 15, 20
                when (idx) {
                    4, 14 -> ThinSeparator()   // 第5、15列细线
                    9, 19 -> ThickSeparator()  // 第10、20列粗线
                }

                TableItem(
                    index = idx,
                    item = item,
                    tableType = tableType,
                    showCharts = showCharts,
                    showHistory = showHistory
                )
            }
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
    tableType: TableType,
    showCharts: Boolean,
    showHistory: Boolean
) {
    val data = (item as? TableDisplayItem.Real)?.data
    val displayMarks = data?.displayMarks

    Column(
        Modifier.width(ITEM_SIZE)
    ) {
        // 索引行
        TextItem(
            text = "${index + 1}",
            color = Color.Black,
            fontSize = 10.sp
        )

        // 数据行 (A, B, C) with circle overlay
        DataTableCell(
            text = data?.dataA?.second?.toString() ?: "",
            color = determineColor(data?.dataA?.second),
            isHistory = if (showHistory) { data?.dataA?.first ?: false } else false,
            circleType = displayMarks?.circleA
        )
        DataTableCell(
            text = data?.dataB?.second?.toString() ?: "",
            color = determineColor(data?.dataB?.second),
            isHistory = if (showHistory) { data?.dataB?.first ?: false } else false,
            circleType = displayMarks?.circleB
        )
        DataTableCell(
            text = data?.dataC?.second?.toString() ?: "",
            color = determineColor(data?.dataC?.second),
            isHistory = if (showHistory) { data?.dataC?.first ?: false } else false,
            circleType = displayMarks?.circleC
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

/**
 * 数据表格单元格（带可选的圈覆盖层）
 */
@Composable
private fun DataTableCell(
    text: String,
    color: Color,
    isHistory: Boolean,
    circleType: com.dsd.baccarat.data.CircleType?
) {
    androidx.compose.foundation.layout.Box {
        TextItem(
            text = text,
            color = color,
            isHistory = isHistory
        )

        // Add circle overlay
        if (circleType != null) {
            CircleOverlay(
                circleType = circleType,
                isFlashing = false  // Will be true for number 2 in WL table
            )
        }
    }
}
