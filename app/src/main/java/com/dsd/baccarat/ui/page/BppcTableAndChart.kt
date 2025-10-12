package com.dsd.baccarat.ui.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.data.BppcItem
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.InputViewModel

// BPPC 表格常量
private val BCCP_ITEM_SIZE = 25.dp
private val BCCP_TABLE_HEIGH = BCCP_ITEM_SIZE * 4
private const val MIN_DISPLY_COUNT = 25  // 最小显示列数
private val BORDER_WIDTH = 0.3.dp

// 柱状图常量（可根据需求调整）
private const val MAX_BAR_VALUE = 8 // 柱状图最大值（1~8）
private val BAR_ITEM_WIDTH = 80.dp // 单个数据项（含编号+3个柱体）的宽度
private val BAR_TOTAL_HEIGHT = 120.dp // 柱状图总高度（竖向）
private val SINGLE_BAR_WIDTH = 5.dp // 单个柱体宽度
private val BAR_SPACING = 4.dp // 柱体间间距

// 示例：生成测试数据并使用列表
@Preview(showBackground = true)
@Composable
private fun Demo() {
    Scaffold { innerPadding ->
        // 1. 准备实际数据（假设已有3条BppcItem数据）
        val realDataItems = listOf(
            BppcItem(dataA = 1, dataB = 2, dataC = 3),
            BppcItem(dataA = 4, dataB = 5, dataC = 6),
            BppcItem(dataA = 7, dataB = 8, dataC = 9),
        )

        // 2. 计算需要补充的占位项数量（总长度10 - 实际数据量）
        val totalCount = MIN_DISPLY_COUNT
        val emptyCount = maxOf(0, totalCount - realDataItems.size)

        // 3. 混合实际数据项和占位项，组成长度为10的列表
        val displayItems: List<BppcDisplayItem> = realDataItems
            .map { BppcDisplayItem.Real(it) } // 实际数据转换为 Real 项
            .plus(List(emptyCount) { BppcDisplayItem.Empty }) // 补充 Empty 占位项

        val listState = rememberLazyListState()
        InternalTableAndChart(innerPadding, displayItems, listState)
    }
}

@Composable
fun BppcTableAndChart(innerPadding: PaddingValues, viewModel: InputViewModel) {
    // 1. 收集ViewModel中的实际数据（随生命周期自动管理）
    val realDataList = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle()

    // 2. 计算需要补充的空占位数量
    val emptyCount = maxOf(0, MIN_DISPLY_COUNT - realDataList.value.size)

    // 3. 合并实际数据和空占位数据（构建最终显示列表）
    val displayItems: List<BppcDisplayItem> = realDataList.value
        .map { BppcDisplayItem.Real(it) } // 实际数据转换为DisplayItem.Real
        .plus(List(emptyCount) { BppcDisplayItem.Empty }) // 补充空占位项

    // 创建并记住 LazyListState（管理滚动状态）
    val listState = rememberLazyListState()
    // 监听数据变化，数据更新时滚动到最后一项
    LaunchedEffect(realDataList.value.size) {  // 当 items 变化时，触发该块
        if (realDataList.value.size >= MIN_DISPLY_COUNT) {
            // 滚动到最后一个元素（索引为 items.lastIndex）
            listState.scrollToItem(index = realDataList.value.lastIndex)
        }
    }

    InternalTableAndChart(innerPadding, displayItems, listState)
}

@Composable
private fun InternalTableAndChart(
    innerPadding: PaddingValues,
    displayItems: List<BppcDisplayItem>,
    listState: LazyListState
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.5f)
    ) {
        BppcTable(innerPadding, displayItems, listState)
        BppcBarChart(displayItems, listState)
    }
}

@Composable
private fun BppcTable(
    innerPadding: PaddingValues,
    displayItems: List<BppcDisplayItem>,
    listState: LazyListState
) {
    Row(
        modifier = Modifier
            .padding(innerPadding)
            .height(BCCP_TABLE_HEIGH)
            .fillMaxWidth()
    )
    {
        BppcTableTitle()
        // 渲染横向列表
        BppcTableLazyRow(displayItems, listState)
    }
}

@Composable
private fun BppcTableTitle() {
    Column(
        modifier = Modifier
            .width(BCCP_ITEM_SIZE)
            .wrapContentHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        BppcTableItem("\\", Color.Gray, FontWeight.Normal)
        BppcTableItem("A", Color.Gray, FontWeight.Normal)
        BppcTableItem("B", Color.Gray, FontWeight.Normal)
        BppcTableItem("C", Color.Gray, FontWeight.Normal)
    }
}

/**
 * 横向滚动列表：4行结构（编号行 + A行 + B行 + C行）
 * @param displayItems 列数据集合（每一项对应一列的完整数据）
 */
@Composable
private fun BppcTableLazyRow(displayItems: List<BppcDisplayItem>, listState: LazyListState) {
    // 横向懒加载容器：包裹4行内容，整体横向滚动
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(BCCP_TABLE_HEIGH),  // 固定列表总高度（4行）
        horizontalArrangement = Arrangement.spacedBy(0.dp),  // 列间距
        verticalAlignment = Alignment.CenterVertically,  // 所有列顶部对齐
    ) {
        // 遍历列数据：每一个item对应一列（包含4行内容）
        itemsIndexed(displayItems) { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()  // 列高度充满列表高度
                    .width(BCCP_ITEM_SIZE),     // 固定列宽度（确保每列对齐）
                verticalArrangement = Arrangement.SpaceEvenly,  // 4行均匀分布
                horizontalAlignment = Alignment.CenterHorizontally  // 行内容居中
            ) {
                // 第1行：列编号
                BppcTableItem(
                    text = "$index",
                    textColor = Color.Gray,
                    fontWeight = FontWeight.Normal,
                )

                DataItem(
                    item, data = when (item) {
                        is BppcDisplayItem.Real -> item.data.dataA
                        is BppcDisplayItem.Empty -> 0
                    }
                )

                DataItem(
                    item, data = when (item) {
                        is BppcDisplayItem.Real -> item.data.dataB
                        is BppcDisplayItem.Empty -> 0
                    }
                )

                DataItem(
                    item, data = when (item) {
                        is BppcDisplayItem.Real -> item.data.dataC
                        is BppcDisplayItem.Empty -> 0
                    }
                )
            }
        }
    }
}

@Composable
private fun DataItem(item: BppcDisplayItem, data: Int) {
    // 第2行：数据A
    BppcTableItem(
        text = when (item) {
            is BppcDisplayItem.Real -> if (data == 0) "" else "$data" // 显示实际内容
            is BppcDisplayItem.Empty -> "" // 占位项不显示内容
        },
        textColor = if (data in listOf(1, 4, 6, 7)) Color.Red else Color.Black,
    )
}

/**
 * 单个行项组件：统一控制行的样式
 */
@Composable
private fun BppcTableItem(
    text: String,
    textColor: Color,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BCCP_ITEM_SIZE)  // 固定每行高度
            .border(
                width = BORDER_WIDTH,
                color = Color.LightGray
            ),
        contentAlignment = Alignment.Center  // 文字居中
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun BppcBarChart(displayItems: List<BppcDisplayItem>, listState: LazyListState) {
    // 整体布局
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BCCP_ITEM_SIZE)
    ) {
        BppcBarChartTitle()
        // 柱状图
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(displayItems) { index, item ->
                when (item) {
                    is BppcDisplayItem.Real -> {
                        BppcItemBarChart(
                            item = item.data,
                            index = index,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    is BppcDisplayItem.Empty -> {
                        BppcItemBarChart(
                            item = BppcItem(),
                            index = index,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BppcBarChartTitle() {
    Column(
        modifier = Modifier
            .width(BCCP_ITEM_SIZE)
            .wrapContentHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        BppcTableItem("A", Color.Gray, FontWeight.Normal)
        Box(modifier = Modifier.padding(top = BCCP_TABLE_HEIGH)) { }
        BppcTableItem("B", Color.Gray, FontWeight.Normal)
        Box(modifier = Modifier.padding(top = BCCP_TABLE_HEIGH)) { }
        BppcTableItem("C", Color.Gray, FontWeight.Normal)
    }
}

/**
 * 单个数据项的柱状图组合（包含A/B/C三个竖向柱体）
 * @param item 实际数据项
 * @param index 数据项编号
 */
@Composable
private fun BppcItemBarChart(
    item: BppcItem,
    index: Int,
    modifier: Modifier = Modifier
) {
    // A/B/C三个柱体竖向排列
    Column(
        modifier = Modifier.width(BCCP_ITEM_SIZE)
    ) {
        // 数据柱状图
        VerticalBar(item.dataA)
        VerticalBar(item.dataB)
        VerticalBar(item.dataC)
    }
}

/**
 * 单个竖向柱体组件
 * @param barData 柱体数据值（决定高度和颜色）
 */

@Composable
fun VerticalBar(barData: Int) {
    Box(
        modifier = Modifier
            .width(BCCP_ITEM_SIZE)
            .height(BAR_TOTAL_HEIGHT)
            .padding(top = BCCP_ITEM_SIZE)
    ) {
        // 绘制网格底边
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridLineColor = Color.LightGray
            val gridLineWidth = BORDER_WIDTH.toPx()

            // 绘制水平网格线
            val gridLineInterval = size.height / MAX_BAR_VALUE
            for (i in 0..MAX_BAR_VALUE) {
                drawLine(
                    color = if (i == 4) Color.Black else gridLineColor,
                    start = Offset(0f, i * gridLineInterval),
                    end = Offset(size.width, i * gridLineInterval),
                    strokeWidth = gridLineWidth
                )
            }

            // 绘制垂直线 Left
            drawLine(
                color = gridLineColor,
                start = Offset(size.width, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = gridLineWidth
            )

            // 绘制垂直线 Right
            drawLine(
                color = gridLineColor,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = gridLineWidth
            )

            // 绘制柱状图
            val barHeight = (barData.toFloat() / MAX_BAR_VALUE * size.height)
            drawRect(
                color = if (barData in listOf(1, 4, 6, 7)) Color.Red else Color.Black,
                topLeft = Offset(size.width / 2 - 1f, size.height - barHeight),
                size = Size(2.dp.toPx(), barHeight)
            )

            // 绘制底部边框线
            drawLine(
                color = Color.Black,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = gridLineWidth
            )
        }
    }
}

