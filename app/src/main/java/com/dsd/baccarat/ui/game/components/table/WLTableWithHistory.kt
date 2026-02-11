package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TableType
import com.dsd.baccarat.ui.game.state.TableMetadata

/**
 * W/L表格（带历史数据前5列）
 *
 * @param currentItems 当前表格数据
 * @param previousItems 上一张表的最后5列
 * @param listState 滚动状态
 * @param tableMetadata 表格元数据
 * @param modifier 修饰符
 */
@Composable
fun WLTableWithHistory(
    currentItems: List<TableDisplayItem>,
    previousItems: List<TableDisplayItem>,
    listState: LazyListState,
    tableMetadata: TableMetadata,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // First 5 columns from previous table with light background
        if (previousItems.isNotEmpty()) {
            GameTable(
                items = previousItems,
                listState = listState,
                tableType = TableType.WL,
                tableMetadata = tableMetadata,
                showCharts = false,
                showHistory = true,
                modifier = Modifier
            )
        }

        // Current table data
        GameTable(
            items = currentItems,
            listState = listState,
            tableType = TableType.WL,
            tableMetadata = tableMetadata,
            showCharts = false,
            showHistory = false,
            modifier = Modifier
        )
    }
}
