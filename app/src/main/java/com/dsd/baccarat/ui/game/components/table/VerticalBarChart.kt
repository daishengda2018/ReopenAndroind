package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dsd.baccarat.ui.game.components.common.BORDER_SIZE
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.TABLE_HEIGHT
import com.dsd.baccarat.ui.game.components.common.determineColorBarChart

/**
 * 垂直柱状图组件
 *
 * @param value 数据值（包含是否是历史数据和数值本身）
 * @param modifier 修饰符
 */
@Composable
fun VerticalBarChart(
    value: Pair<Boolean, Int?>?,
    modifier: Modifier = Modifier
) {
    val color = determineColorBarChart(value?.second)

    Box(
        modifier
            .width(ITEM_SIZE)
            .height(TABLE_HEIGHT)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val gridColor = Color.LightGray
            val gridWidth = BORDER_SIZE.toPx()
            val interval = size.height / MAX_VALUE

            // 绘制网格线和边框
            for (i in 0..MAX_VALUE) {
                drawLine(
                    color = if (i == 4) Color.Black else gridColor,
                    start = Offset(0f, i * interval),
                    end = Offset(size.width, i * interval),
                    strokeWidth = gridWidth
                )
            }

            // 绘制左右边框
            drawLine(
                gridColor,
                Offset(size.width, 0f),
                Offset(size.width, size.height),
                gridWidth * 1.8f
            )
            drawLine(
                gridColor,
                Offset(0f, 0f),
                Offset(0f, size.height),
                gridWidth * 1.8f
            )

            // 底部边框加粗
            drawLine(
                Color.Black,
                Offset(0f, size.height),
                Offset(size.width, size.height),
                gridWidth
            )

            // 绘制柱状图
            val value = value?.second ?: 0
            if (value > 0) {
                val barHeight = (value.toFloat() / MAX_VALUE) * size.height
                drawRect(
                    color = color,
                    topLeft = Offset(size.width / 2 - 1.dp.toPx(), size.height - barHeight),
                    size = Size(3.dp.toPx(), barHeight)
                )
            }
        }
    }
}

private const val MAX_VALUE = 8
