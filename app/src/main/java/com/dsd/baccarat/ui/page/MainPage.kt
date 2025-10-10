package com.dsd.baccarat.ui.page

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.CombinationData

/**
 * 横向滚动列表：4行结构（编号行 + A行 + B行 + C行）
 * @param combinationList 列数据集合（每一项对应一列的完整数据）
 */
@Composable
fun HorizontalMultiRowList(combinationList: List<CombinationData>) {
    // 横向懒加载容器：包裹4行内容，整体横向滚动
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .height(120.dp)  // 固定列表总高度（4行）
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically  // 所有列顶部对齐
    ) {
        // 遍历列数据：每一个item对应一列（包含4行内容）
        itemsIndexed(combinationList) { index, columnData ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()  // 列高度充满列表高度
                    .width(30.dp),     // 固定列宽度（确保每列对齐）
                verticalArrangement = Arrangement.SpaceEvenly,  // 4行均匀分布
                horizontalAlignment = Alignment.CenterHorizontally  // 行内容居中
            ) {
                // 第1行：列编号（灰色背景 + 加粗）
                ColumnRowItem(
                    text = "${columnData.columnNumber}",
                    backgroundColor = Color(0xFFF5F5F5),
                    textColor = Color(0xFF333333),
                    fontWeight = FontWeight.Bold,
                )

                // 第2行：数据A（蓝色背景）
                ColumnRowItem(
                    text = "A:${columnData.dataA}",
                    backgroundColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2)
                )

                // 第3行：数据B（绿色背景）
                ColumnRowItem(
                    text = "B:${columnData.dataB}",
                    backgroundColor = Color(0xFFE8F5E9),
                    textColor = Color(0xFF388E3C)
                )

                // 第4行：数据C（橙色背景）
                ColumnRowItem(
                    text = "C:${columnData.dataC}",
                    backgroundColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFF57C00)
                )
            }
        }
    }
}

/**
 * 单个行项组件：统一控制行的样式（背景、圆角、文字样式）
 */
@Composable
private fun ColumnRowItem(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)  // 固定每行高度
            .border(
                width = 0.5.dp,
                color = Color.LightGray
            ),
        // 添加底部横线分割
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

// 示例：生成测试数据并使用列表
@Preview(showBackground = true)
@Composable
fun HorizontalListDemo(innerPadding: PaddingValues) {

    // 模拟15列测试数据（可根据实际需求动态生成）
    val testData = (1..25).map { columnNum ->
        CombinationData(
            columnNumber = columnNum,
            dataA = columnNum,
            dataB = columnNum,
            dataC = columnNum
        )
    }

    Box(modifier = Modifier.padding(innerPadding))
    {
        // 渲染横向列表
        HorizontalMultiRowList(combinationList = testData)
    }
}
