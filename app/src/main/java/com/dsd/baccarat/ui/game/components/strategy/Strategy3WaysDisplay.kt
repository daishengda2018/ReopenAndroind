package com.dsd.baccarat.ui.game.components.strategy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dsd.baccarat.data.Strategy3WyasDisplayItem
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.SPACE_SIZE
import com.dsd.baccarat.ui.game.components.common.TITLE_WIDTH_MIDEM
import com.dsd.baccarat.ui.game.components.common.TextItem
import com.dsd.baccarat.ui.game.components.common.determineColor

/**
 * 策略三路显示组件
 *
 * @param titles 标题列表
 * @param mainTitleIndex 主标题索引（用于高亮）
 * @param titleStr 标题字符串
 * @param predictedValue1 预测值1
 * @param predictedValue2 预测值2
 * @param displayItems1 显示项1列表
 * @param displayItems2 显示项2列表
 * @param isHistoryMode 是否是历史模式
 * @param listStateProvider 滚动状态提供者
 */
@Composable
fun Strategy3WaysDisplay(
    titles: List<String>,
    mainTitleIndex: Int = 0,
    titleStr: String?,
    predictedValue1: String?,
    predictedValue2: String?,
    displayItems1: List<Strategy3WyasDisplayItem>,
    displayItems2: List<Strategy3WyasDisplayItem>,
    isHistoryMode: Boolean = false,
    listStateProvider: @Composable () -> LazyListState
) {
    val selectedOption = remember { mutableIntStateOf(1) }

    Spacer(Modifier.height(SPACE_SIZE))

    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(ITEM_SIZE)) {
            Spacer(Modifier.height(ITEM_SIZE))
            TextItem(text = titles[0], color = Color.Black, isHighLight = (mainTitleIndex == 0))
            TextItem(text = titles[1], color = Color.Black, isHighLight = (mainTitleIndex == 1))
        }

        Spacer(Modifier.width(SPACE_SIZE))

        Column {
            Row {
                TextItem(
                    text = titles[2],
                    width = TITLE_WIDTH_MIDEM,
                    isSelected = (selectedOption.intValue == 1)
                ) {
                    selectedOption.intValue = 1
                }
                Spacer(Modifier.width(ITEM_SIZE))
                TextItem(
                    text = titles[3],
                    width = TITLE_WIDTH_MIDEM,
                    isSelected = (selectedOption.intValue == 2)
                ) {
                    selectedOption.intValue = 2
                }

                if (!isHistoryMode) {
                    when (selectedOption.intValue) {
                        1 -> TextItem(
                            text = predictedValue1 ?: "-",
                            color = if (predictedValue1.isNullOrEmpty()) Color.Gray else determineColor(predictedValue1),
                            width = (TITLE_WIDTH_MIDEM * 2),
                            isShowBorder = false
                        )

                        2 -> TextItem(
                            text = predictedValue2 ?: "-",
                            color = if (predictedValue2.isNullOrEmpty()) Color.Gray else determineColor(predictedValue2),
                            width = (TITLE_WIDTH_MIDEM * 2),
                            isShowBorder = false
                        )
                    }
                }
            }

            when (selectedOption.intValue) {
                1 -> StrategyTable(displayItems = displayItems1, listStateProvider = listStateProvider)
                2 -> StrategyTable(displayItems = displayItems2, listStateProvider = listStateProvider)
            }
        }
    }
}

/**
 * 策略表格组件
 */
@Composable
private fun StrategyTable(
    displayItems: List<Strategy3WyasDisplayItem>,
    listStateProvider: @Composable () -> LazyListState
) {
    LazyRow(
        state = listStateProvider(),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            count = displayItems.size,
            key = { index -> index }
        ) { idx ->
            val item = (displayItems.getOrNull(idx) as? Strategy3WyasDisplayItem.Real)?.data

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem(
                    text = if (item?.first == -1) "/" else item?.first?.toString() ?: "",
                    color = determineColor(item?.first)
                )
                TextItem(
                    text = if (item?.second == -1) "/" else item?.second?.toString() ?: "",
                    color = determineColor(item?.second)
                )
            }
        }
    }
}
