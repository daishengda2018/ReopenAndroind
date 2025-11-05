package com.dsd.baccarat

import com.dsd.baccarat.model.DefaultViewModel.Companion.MAX_COLUMN_COUNT
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    val predictedList = ArrayList<String>()
    var itemList: List<Pair<String, List<String>>> = listOf(
        Pair("4", listOf("P", "B", "B")),
        Pair("6", listOf("B", "P", "B")),
        Pair("8", listOf("B", "B", "P"))
    )

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testFilterLogic() {
        // 测试场景 2

        val actualOpenedList0 = listOf("")
        val actualOpenedList1 = listOf("P")
        val actualOpenedList2 = listOf("P", "P")
        val actualOpenedList3 = listOf("P", "P", "B")

        val actualOpenedList4 = listOf("")
        val actualOpenedList5 = listOf("P")
        val actualOpenedList6 = listOf("P", "B")
        val actualOpenedList7 = listOf("P", "B", "P")

        filterItems(actualOpenedList0)
        println("测试场景 0 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        filterItems(actualOpenedList1)
        println("测试场景 1 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        filterItems(actualOpenedList2)
        println("测试场景 2 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        filterItems(actualOpenedList3)
        println("测试场景 3 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        println("--------------") // 预期输出: [P, B]

        filterItems(actualOpenedList4)
        println("测试场景 4 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        filterItems(actualOpenedList5)
        println("测试场景 5 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        filterItems(actualOpenedList6)
        println("测试场景 6 结果: $predictedList ${itemList}") // 预期输出: [P, B]

        filterItems(actualOpenedList7)
        println("测试场景 7 结果: $predictedList ${itemList}") // 预期输出: [P, B]
    }


    // 封装过滤逻辑
    fun filterItems(actualOpenedList: List<String>) {
        // 2. 准备数据
        predictedList.apply {
            if (size >= MAX_COLUMN_COUNT) clear()
        }

        // 3. 根据条件处理数据
        if (actualOpenedList.size < MAX_COLUMN_COUNT) {
            val currentIndex = actualOpenedList.size
            val aRowItemList = if (actualOpenedList.isEmpty()) {
                // 情况1: actualOpenedList 为空，直接提取当前索引的值
                itemList.filterNot { itemList -> itemList.first == "XX" }.map { it.second[currentIndex] }
            } else {
                // 情况2: 过滤掉与 actualOpenedList 完全匹配的项，提取当前索引的值
                itemList
                    .filterNot { itemList -> itemList.first == "XX" }
                    .filterNot { item ->
                        item.second.withIndex().any { (index, value) ->
                            index < actualOpenedList.size && value == actualOpenedList[index]
                        }
                    }
                    .map { it.second[currentIndex] }
                    .ifEmpty { emptyList() }
            }

            // 4. 更新 predictedList
            predictedList.add(if (aRowItemList.distinct().size == 1) aRowItemList.first() else "-")
        } else {
            // 情况3: 处理已满的情况：需要标记完全不匹配的元素
            val openedSize = actualOpenedList.size
            itemList = itemList
                .map { item ->
                    if (item.second.withIndex().none { (index, value) ->
                            index < openedSize && value == actualOpenedList[index]
                        }) {
                        item.copy("XX") // 完全不匹配时标记为 "XX"
                    } else {
                        item // 有匹配时保持不变
                    }
                }
        }
    }

    // 辅助函数：检查 item 是否与 openedList 匹配
    private fun isItemMatched(itemValues: List<String?>, openedList: List<String?>): Boolean {
        return itemValues.withIndex().none { (index, value) ->
            value != openedList[index]
        }
    }

    // 模拟 Data 类
    data class Data(val itemList: List<Pair<String, List<String>>>)
}