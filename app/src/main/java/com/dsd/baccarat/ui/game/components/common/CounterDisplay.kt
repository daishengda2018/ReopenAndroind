package com.dsd.baccarat.ui.game.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat

/**
 * 计数器显示组件
 *
 * @param value1 第一个值
 * @param color1 第一个值的颜色
 * @param value2 第二个值
 * @param color2 第二个值的颜色
 * @param isShowWsr 是否显示 WSR（胜率）
 * @param isHistory 是否是历史数据
 * @param modifier 修饰符
 */
@Composable
fun CounterDisplay(
    modifier: Modifier = Modifier,
    value1: Int,
    color1: Color,
    value2: Int,
    color2: Color,
    isShowWsr: Boolean = false,
    isHistory: Boolean = false,
) {
    val total = value1 + value2
    val df = remember { DecimalFormat("0.0%") }
    val backgroundColor = remember(isHistory) {
        if (isHistory) Color.LightGray else Color.Transparent
    }

    Row(
        modifier
            .wrapContentWidth()
            .height(ITEM_SIZE)
            .background(backgroundColor),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start
    ) {
        TextItem(text = "$value1", color = color1, isHistory = isHistory, width = TITLE_WIDTH_SHORT)
        TextItem(text = "$value2", color = color2, isHistory = isHistory, width = TITLE_WIDTH_SHORT)
        TextItem(text = "T$total", color = Color.Black, isHistory = isHistory, width = TITLE_WIDTH_MIDEM)
        TextItem(text = "${value1 - value2}", color = color2, isHistory = isHistory, width = TITLE_WIDTH_SHORT)

        if (isShowWsr) {
            val wsr = if (total != 0) value1 / total.toFloat() else 0f
            TextItem(
                text = "WSR${if (wsr == 1f) "100%" else df.format(wsr)}",
                color = Color.Magenta,
                isHistory = isHistory,
                width = TITLE_WIDTH_LONG
            )
        }
    }
}
