package com.dsd.baccarat.ui.game.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 细分隔线（第5、15列）
 */
@Composable
fun ThinSeparator(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(1.dp)
            .height(TABLE_HEIGHT)
    ) {
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
}

/**
 * 粗分隔线（第10、20列）
 */
@Composable
fun ThickSeparator(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(2.dp)
            .height(TABLE_HEIGHT)
    ) {
        drawLine(
            color = Color.Black,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}
