package com.dsd.baccarat.ui.game.components.table

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dsd.baccarat.data.CircleType
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE

/**
 * 套圈覆盖层组件
 *
 * @param circleType 圈的类型（红/蓝/双圈）
 * @param isFlashing 是否闪烁（数字2报警时使用）
 * @param modifier 修饰符
 */
@Composable
fun CircleOverlay(
    circleType: CircleType?,
    isFlashing: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (circleType == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val color = when (circleType) {
        CircleType.RED -> Color.Red
        CircleType.BLUE -> Color.Blue
        CircleType.BOTH -> Color.Magenta
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .width(ITEM_SIZE)
                .height(ITEM_SIZE)
        ) {
            val circleRadius = size.minDimension / 2 - 2.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)

            // Draw outer circle (ring effect)
            drawCircle(
                color = color,
                radius = circleRadius,
                center = center,
                alpha = if (isFlashing) alpha else 1f
            )

            // Draw inner circle to create ring
            drawCircle(
                color = Color.White,
                radius = circleRadius - 3.dp.toPx(),
                center = center,
                alpha = if (isFlashing) alpha else 1f
            )
        }
    }
}
