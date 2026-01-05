package com.dsd.baccarat.ui.game.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.dsd.baccarat.ui.theme.PurpleGrey80

/**
 * 可复用的带边框文本项组件
 *
 * @param modifier 修饰符
 * @param text 显示的文本
 * @param color 文字颜色
 * @param width 组件宽度
 * @param fontSize 文字大小
 * @param fontWeight 文字粗细
 * @param isSelected 是否选中
 * @param isObsolete 是否已过时
 * @param isShowBorder 是否显示边框
 * @param isHistory 是否是历史数据
 * @param isHighLight 是否高亮
 * @param onClick 点击回调
 */
@Composable
fun TextItem(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Black,
    width: Dp = ITEM_SIZE,
    fontSize: TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    isSelected: Boolean = false,
    isObsolete: Boolean = false,
    isShowBorder: Boolean = true,
    isHistory: Boolean = false,
    isHighLight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = remember(isHighLight, isSelected, isObsolete) {
        when {
            isSelected || isHighLight -> PurpleGrey80
            isObsolete || isHistory -> Color.LightGray
            else -> Color.Transparent
        }
    }

    Box(
        modifier
            .width(width)
            .height(ITEM_SIZE)
            .background(backgroundColor)
            .clickable { onClick?.invoke() }
            .alpha(if (isObsolete) 0.5f else 1.0f)
            .conditionalBorder(isShowBorder, isObsolete || isHistory),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight
        )
    }
}

/**
 * 条件边框修饰符
 */
fun Modifier.conditionalBorder(showBorder: Boolean, isWhiteBorder: Boolean): Modifier {
    return if (showBorder) {
        this.border(
            BorderStroke(BORDER_SIZE, if (isWhiteBorder) Color.White else Color.LightGray)
        )
    } else {
        this
    }
}

// ==================== 常量 ====================

val ITEM_SIZE = 22.dp
val SPACE_SIZE = 5.dp
val TABLE_HEIGHT = ITEM_SIZE * 4
val TITLE_WIDTH_SHORT = ITEM_SIZE * 2
val TITLE_WIDTH_MIDEM = ITEM_SIZE * 3
val TITLE_WIDTH_LONG = ITEM_SIZE * 4
val BORDER_SIZE = 0.3.dp

// 文本颜色常量
// B = Banker (庄家), P = Player (玩家), W = Win (赢), L = Lose (输)
val TEXT_COLOR_BANKER = Color.Red   // 庄家颜色
val TEXT_COLOR_PLAYER = Color.Blue  // 玩家颜色
val TEXT_COLOR_WIN = Color.Red      // 赢的颜色
val TEXT_COLOR_LOSE = Color.Blue    // 输的颜色

/**
 * 根据数值确定文本颜色
 */
fun determineColor(value: Int?): Color {
    if (value == null) return Color.Black
    return if (value in RED_COLOR_VALUES1) Color.Red else Color.Blue
}

/**
 * 根据字符串值确定文本颜色
 */
fun determineColor(value: String?): Color {
    if (value == null) return Color.Black
    return if (value == "B") Color.Red else Color.Blue
}

/**
 * 根据数值确定柱状图颜色
 */
fun determineColorBarChart(value: Int?): Color {
    if (value == null) return Color.Black
    return if (value in RED_COLOR_VALUES2) Color.Red else Color.Blue
}

// ==================== 私有常量 ====================

private val RED_COLOR_VALUES1 = setOf(1, 4, 6, 7)
private val RED_COLOR_VALUES2 = setOf(1, 2, 5, 6)
