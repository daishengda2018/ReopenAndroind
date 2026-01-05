package com.dsd.baccarat.ui.game.components.strategy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dsd.baccarat.data.StrategyGridInfo
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.SPACE_SIZE
import com.dsd.baccarat.ui.game.components.common.TITLE_WIDTH_SHORT
import com.dsd.baccarat.ui.game.components.common.TextItem
import com.dsd.baccarat.ui.game.components.common.determineColor

// 策略网格列数
private const val STRATEGY_GRID_COLUMN_COUNT = 3

/**
 * 策略网格显示组件
 *
 * @param title 标题
 * @param strategyItem 策略网格信息
 */
@Composable
fun StrategyGridDisplay(
    title: String,
    strategyItem: StrategyGridInfo
) {
    Spacer(Modifier.height(SPACE_SIZE))

    Row {
        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            TextItem(text = title)
        }
        Spacer(Modifier.width(SPACE_SIZE))

        for (i in 0 until STRATEGY_GRID_COLUMN_COUNT) {
            val gridItem = strategyItem.itemList.getOrNull(i)
            val isObsolete = gridItem?.isObsolete ?: false
            val dataList = gridItem?.items
            val combinationTitle = gridItem?.title

            Column(Modifier.width(ITEM_SIZE)) {
                TextItem(
                    text = combinationTitle ?: "",
                    color = determineColor(combinationTitle?.toInt() ?: 0),
                    isObsolete = isObsolete,
                    isShowBorder = false,
                    width = TITLE_WIDTH_SHORT
                )

                val data2 = dataList?.getOrNull(0)
                TextItem(text = data2 ?: "", color = determineColor(data2), isObsolete = isObsolete, width = TITLE_WIDTH_SHORT)

                val data3 = dataList?.getOrNull(1)
                TextItem(text = data3 ?: "", color = determineColor(data3), isObsolete = isObsolete, width = TITLE_WIDTH_SHORT)

                val data4 = dataList?.getOrNull(2)
                TextItem(text = data4 ?: "", color = determineColor(data4), isObsolete = isObsolete, width = TITLE_WIDTH_SHORT)
            }
        }

        Spacer(Modifier.width(ITEM_SIZE))

        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            for (i in 0 until STRATEGY_GRID_COLUMN_COUNT) {
                val text = strategyItem.predictedList.getOrNull(i) ?: ""
                TextItem(text = text, color = determineColor(text))
            }
        }

        Column {
            Spacer(Modifier.height(ITEM_SIZE))
            for (i in 0 until STRATEGY_GRID_COLUMN_COUNT) {
                val text = strategyItem.actualOpenedList.getOrNull(i) ?: ""
                TextItem(text = text, color = determineColor(text))
            }
        }
    }
}
