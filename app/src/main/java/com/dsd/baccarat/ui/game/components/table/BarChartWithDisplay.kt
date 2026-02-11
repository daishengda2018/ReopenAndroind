package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.SPACE_SIZE
import com.dsd.baccarat.ui.game.components.common.TextItem

/**
 * 带显示窗的柱状图组件（V2功能）
 *
 * @param chartData 图表数据
 * @param modifier 修饰符
 */
@Composable
fun BarChartWithDisplay(
    chartData: List<TableDisplayItem>,
    modifier: Modifier = Modifier
) {
    // Count occurrences of each pattern (1-8)
    val counts = remember(chartData) {
        (1..8).associateWith { num ->
            chartData.count { item ->
                (item as? TableDisplayItem.Real)?.data?.let { data ->
                    data.dataA?.second == num ||
                    data.dataB?.second == num ||
                    data.dataC?.second == num
                } == true
            }
        }
    }

    // Find pattern with minimum count for highlighting
    val minCountNumber = remember(counts) {
        counts.minByOrNull { it.value }?.key
    }

    Column(modifier = modifier) {
        // 8 display windows
        Row(
            horizontalArrangement = Arrangement.spacedBy(SPACE_SIZE)
        ) {
            (1..8).forEach { num ->
                DisplayWindow(
                    number = num,
                    count = counts[num] ?: 0,
                    showBackground = num == minCountNumber
                )
            }
        }

        Spacer(Modifier.height(SPACE_SIZE))

        // Bar charts
        LazyRow {
            items(chartData) { item ->
                VerticalBarChart(
                    value = (item as? TableDisplayItem.Real)?.data?.dataA
                )
            }
        }
    }
}

@Composable
private fun DisplayWindow(
    number: Int,
    count: Int,
    showBackground: Boolean
) {
    val color = when (number) {
        1, 2, 5, 6 -> Color.Red
        3, 4, 7, 8 -> Color.Blue
        else -> Color.Gray
    }

    TextItem(
        text = "$number:$count",
        color = color,
        isHighLight = showBackground,
        fontSize = 12.sp
    )
}
