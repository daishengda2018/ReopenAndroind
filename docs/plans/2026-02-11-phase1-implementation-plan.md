# Phase 1: Core Table Upgrades Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade BP table, W/L table, and bar chart with V2 features: table numbering, circle marks, separators, 8 display windows, and 25-column auto-renewal.

**Architecture:** Extend existing GameTable component with conditional rendering for BP/WL types. Use UDF pattern with immutable state. Add TableMetadata for numbering, DisplayMarks for visual overlays, and CircleOverlay composable for circle rendering.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, Coroutines, JUnit 5, Compose Testing

---

## Phase 1.1: Data Model Preparation

### Task 1: Add New Enums

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/data/DataStructure.kt:1-100`

**Step 1: Add TableType enum**

Add after the existing `ColumnType` enum (around line 35):

```kotlin
// Table type for rendering differentiation
@Immutable
enum class TableType { BP, WL }
```

**Step 2: Add CircleMarkType enum**

Add immediately after TableType:

```kotlin
// Circle mark type for different marking scenarios
@Immutable
enum class CircleMarkType {
    ZF,         // Adjacent opposite patterns
    ZF_SEP,     // Skip-one opposite patterns
    CIRCLE_12,  // BP pattern betting table specific
    CIRCLE_34,
    CIRCLE_56,
    CIRCLE_78,
    WL_ALARM    // W/L table blue circle for numbers 2 and 7
}
```

**Step 3: Add CircleType and WLSymbol enums**

Add after CircleMarkType:

```kotlin
// Circle type for overlay rendering
@Immutable
enum class CircleType { RED, BLUE, BOTH }

// W/L symbol for sync display
@Immutable
enum class WLSymbol { WIN, LOSS }
```

**Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/data/DataStructure.kt
git commit -m "feat(data): add V2 enums for table types and circle marks"
```

---

### Task 2: Add DisplayMarks and Extend TableItem

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/data/DataStructure.kt:44-48`

**Step 1: Add DisplayMarks data class**

Add before the `TableItem` class (around line 43):

```kotlin
// Display marks for V2 features
@Immutable
data class DisplayMarks(
    val circleA: CircleType? = null,
    val circleB: CircleType? = null,
    val circleC: CircleType? = null,
    val wlSymbolA: WLSymbol? = null,
    val wlSymbolB: WLSymbol? = null,
    val wlSymbolC: WLSymbol? = null,
    val isLightBackground: Boolean = false
)
```

**Step 2: Extend TableItem with displayMarks**

Modify the `TableItem` class to include the new field:

```kotlin
// 主列表项 (使用可空类型)
@Immutable
data class TableItem(
    val dataA: Pair<Boolean, Int?>? = null,
    val dataB: Pair<Boolean, Int?>? = null,
    val dataC: Pair<Boolean, Int?>? = null,
    // V2: Display marks for circles and symbols
    val displayMarks: DisplayMarks? = null
)
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/data/DataStructure.kt
git commit -m "feat(data): add DisplayMarks and extend TableItem for V2"
```

---

### Task 3: Add TableMetadata to GameUiState

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/ui/game/state/GameUiState.kt:1-171`

**Step 1: Add TableMetadata data class**

Add after the imports and before `GameUiState` (around line 15):

```kotlin
/**
 * 表格元数据（V2功能）
 */
@Immutable
data class TableMetadata(
    val tableNumber: Int = 1,              // 表格编号 (1-99999)
    val currentColumnCount: Int = 0,       // 当前列数
    val maxColumnsPerTable: Int = 25       // 每张表最大列数（W/L表用）
) {
    val displayNumber: String
        get() = tableNumber.toString().padStart(5, '0')
}
```

**Step 2: Add metadata fields to GameUiState**

Add new fields at the end of `GameUiState` (after `historyEndTime`, around line 64):

```kotlin
    // ========== V2: 表格元数据 ==========
    val bpTableMeta: TableMetadata = TableMetadata(),
    val wlTableMeta: TableMetadata = TableMetadata(),

    // ========== V2: W/L表格专用 ==========
    val wlPreviousTableData: List<TableDisplayItem> = emptyList(),

    // ========== V2: 套圈状态 ==========
    val bpCircleMarkType: CircleMarkType? = null,
    val wlCircleMarkEnabled: Boolean = false
```

**Step 3: Add necessary imports**

Add at the top of the file:

```kotlin
import com.dsd.baccarat.data.CircleMarkType
import com.dsd.baccarat.data.TableType
```

**Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/state/GameUiState.kt
git commit -m "feat(state): add TableMetadata and V2 fields to GameUiState"
```

---

### Task 4: Add New UI Events

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/ui/game/state/GameUiEvent.kt`

**Step 1: Add circle marking events**

Add at the end of the sealed interface (after existing events):

```kotlin
    // ========== V2: 套圈事件 ==========
    data class ToggleCircleZF(val tableType: TableType) : GameUiEvent
    data class ToggleCircleZFSep(val tableType: TableType) : GameUiEvent
```

**Step 2: Add import**

Add at the top:

```kotlin
import com.dsd.baccarat.data.TableType
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/state/GameUiEvent.kt
git commit -m "feat(events): add circle marking events for V2"
```

---

### Task 5: Add Alarm Side Effect

**Files:**
- Check if `GameSideEffect.kt` exists, if not create it
- Create: `app/src/main/java/com/dsd/baccarat/ui/game/state/GameSideEffect.kt` OR
- Modify: existing side effect file

**Step 1: Check for existing side effect file**

Run: `find app/src/main/java/com/dsd/baccarat -name "*SideEffect*"`

**Step 2: Add alarm type and side effect**

If creating new file:

```kotlin
package com.dsd.baccarat.ui.game.state

/**
 * 游戏副作用（一次性事件）
 */
sealed interface GameSideEffect {
    data class TriggerAlarm(val alarmType: AlarmType) : GameSideEffect
}

enum class AlarmType { NUMBER_2 }
```

If modifying existing file, add the alarm type and side effect.

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/state/GameSideEffect.kt
git commit -m "feat(side-effects): add alarm side effect for number 2"
```

---

## Phase 1.2: UI Components

### Task 6: Create CircleOverlay Component

**Files:**
- Create: `app/src/main/java/com/dsd/baccarat/ui/game/components/table/CircleOverlay.kt`

**Step 1: Create the file with basic structure**

```kotlin
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
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/table/CircleOverlay.kt
git commit -m "feat(ui): create CircleOverlay component for circle marks"
```

---

### Task 7: Create Separator Components

**Files:**
- Create: `app/src/main/java/com/dsd/baccarat/ui/game/components/common/SeparatorComponents.kt`

**Step 1: Create thin and thick separator components**

```kotlin
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
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/common/SeparatorComponents.kt
git commit -m "feat(ui): create separator components for table columns"
```

---

### Task 8: Upgrade VerticalBarChart - Colors and Thickness

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/ui/game/components/table/VerticalBarChart.kt`

**Step 1: Update color determination logic**

Replace the `determineColorBarChart` usage in the Canvas (around line 29):

```kotlin
val color = when (value?.second) {
    1, 2, 5, 6 -> Color.Red      // 12/56红色
    3, 4, 7, 8 -> Color.Blue     // 34/78蓝色
    else -> Color.Gray
}
```

**Step 2: Update grid line thickness logic**

In the Canvas drawing loop (around line 43), modify the stroke width:

```kotlin
for (i in 0..MAX_VALUE) {
    val strokeWidth = when {
        i == 4 -> gridWidth
        value?.second in listOf(2, 4, 6, 8) -> gridWidth * 1.8f  // 粗线条
        else -> gridWidth
    }

    drawLine(
        color = if (i == 4) Color.Black else gridColor,
        start = Offset(0f, i * interval),
        end = Offset(size.width, i * interval),
        strokeWidth = strokeWidth
    )
}
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Test visually**

Run: `./gradlew installDebug`
Expected: App installs and bar chart shows correct colors

**Step 5: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/table/VerticalBarChart.kt
git commit -m "feat(chart): update bar colors (12/56 red, 34/78 blue) and thickness"
```

---

### Task 9: Create BarChartWithDisplay Component

**Files:**
- Create: `app/src/main/java/com/dsd/baccarat/ui/game/components/table/BarChartWithDisplay.kt`

**Step 1: Create the component with 8 display windows**

```kotlin
package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.ui.game.components.common.ITEM_SIZE
import com.dsd.baccarat.ui.game.components.common.SPACE_SIZE
import com.dsd.baccarat.ui.game.components.common.TextItem

/**
 * 带显示窗的柱状图组件（V2功能）
 *
 * @param chartData 图表数据
 */
@Composable
fun BarChartWithDisplay(
    chartData: List<TableDisplayItem>,
    modifier: Modifier = Modifier
) {
    // Count occurrences of each pattern (1-8)
    val counts = remember(chartData) {
        (1..8).associateWith { num ->
            chartData.count { item ->
                (item as? TableDisplayItem.Real)?.data?.let { data ->
                    data.dataA?.second == num ||
                    data.dataB?.second == num ||
                    data.dataC?.second == num
                } == true
            }
        }
    }

    // Find pattern with minimum count for highlighting
    val minCountNumber = remember(counts) {
        counts.minByOrNull { it.value }?.key
    }

    Column(modifier = modifier) {
        // 8 display windows
        Row(
            horizontalArrangement = Arrangement.spacedBy(SPACE_SIZE)
        ) {
            (1..8).forEach { num ->
                DisplayWindow(
                    number = num,
                    count = counts[num] ?: 0,
                    showBackground = num == minCountNumber
                )
            }
        }

        Spacer(Modifier.height(SPACE_SIZE))

        // Bar charts
        LazyRow {
            items(chartData) { item ->
                VerticalBarChart(
                    value = (item as? TableDisplayItem.Real)?.data?.dataA
                )
            }
        }
    }
}

@Composable
private fun DisplayWindow(
    number: Int,
    count: Int,
    showBackground: Boolean
) {
    val color = when (number) {
        1, 2, 5, 6 -> Color.Red
        3, 4, 7, 8 -> Color.Blue
        else -> Color.Gray
    }

    TextItem(
        text = "$number:$count",
        color = color,
        isHighLight = showBackground,
        fontSize = 12.sp
    )
}
```

**Step 2: Check if TextItem supports isHighLight parameter**

Read: `app/src/main/java/com/dsd/baccarat/ui/game/components/common/TextItem.kt`

If it doesn't have `isHighLight` parameter, add it:

```kotlin
@Composable
fun TextItem(
    text: String,
    color: Color = Color.Black,
    fontSize: TextUnit = 16.sp,
    isHistory: Boolean = false,
    isHighLight: Boolean = false,  // Add this
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(ITEM_SIZE)
            .height(ITEM_SIZE)
            .background(
                when {
                    isHighLight -> Color.Yellow.copy(alpha = 0.3f)
                    isHistory -> Color.LightGray.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            // ... rest of text styling
        )
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/table/BarChartWithDisplay.kt
git add app/src/main/java/com/dsd/baccarat/ui/game/components/common/TextItem.kt
git commit -m "feat(chart): create BarChartWithDisplay with 8 display windows"
```

---

### Task 10: Upgrade GameTable - Add Table Type Support

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/ui/game/components/table/GameTable.kt`

**Step 1: Update function signature**

Replace the function signature (around line 28):

```kotlin
@Composable
fun GameTable(
    items: List<TableDisplayItem>,
    listState: LazyListState,
    tableType: com.dsd.baccarat.data.TableType,
    tableMetadata: com.dsd.baccarat.ui.game.state.TableMetadata,
    showCharts: Boolean = false,
    showHistory: Boolean = false,
    previousItems: List<TableDisplayItem> = emptyList(),
    modifier: Modifier = Modifier
)
```

**Step 2: Add imports at top**

```kotlin
import com.dsd.baccarat.data.TableType
import com.dsd.baccarat.ui.game.state.TableMetadata
import com.dsd.baccarat.ui.game.components.common.ThinSeparator
import com.dsd.baccarat.ui.game.components.common.ThickSeparator
```

**Step 3: Add table number display**

Add before the LazyRow (around line 36):

```kotlin
Column {
    // Table number display
    if (tableMetadata.tableNumber > 0) {
        Text(
            text = tableMetadata.displayNumber,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }

    LazyRow(
        state = listState,
        modifier = modifier
    ) {
        // ... existing items
    }
}
```

**Step 4: Add column separators**

Inside the LazyRow itemsIndexed, add separator logic:

```kotlin
itemsIndexed(
    items = items,
    key = { index, item ->
        // ... existing key logic
    }
) { idx, item ->
    // Add separators at columns 5, 10, 15, 20
    when (idx) {
        4, 14 -> ThinSeparator()   // 第5、15列细线
        9, 19 -> ThickSeparator()  // 第10、20列粗线
    }

    TableItem(
        index = idx,
        item = item,
        tableType = tableType,
        showCharts = showCharts,
        showHistory = showHistory
    )
}
```

**Step 5: Update TableItem signature**

```kotlin
@Composable
private fun TableItem(
    index: Int,
    item: TableDisplayItem,
    tableType: TableType,
    showCharts: Boolean,
    showHistory: Boolean
)
```

**Step 6: Add circle overlay in TableItem**

Inside TableItem, after drawing text items:

```kotlin
val data = (item as? TableDisplayItem.Real)?.data
val displayMarks = data?.displayMarks

// Add circle overlay
if (displayMarks?.circleA != null) {
    Box {
        TextItem(/* existing text item code */)
        CircleOverlay(
            circleType = displayMarks.circleA,
            isFlashing = false  // Will be true for number 2 in WL table
        )
    }
} else {
    TextItem(/* existing text item code */)
}
```

**Step 7: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/table/GameTable.kt
git commit -m "feat(table): upgrade GameTable with table type and separator support"
```

---

### Task 11: Create WLTableWithHistory Component

**Files:**
- Create: `app/src/main/java/com/dsd/baccarat/ui/game/components/table/WLTableWithHistory.kt`

**Step 1: Create the component**

```kotlin
package com.dsd.baccarat.ui.game.components.table

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dsd.baccarat.data.TableDisplayItem
import com.dsd.baccarat.data.TableType
import com.dsd.baccarat.ui.game.state.TableMetadata

/**
 * W/L表格（带历史数据前5列）
 *
 * @param currentItems 当前表格数据
 * @param previousItems 上一张表的最后5列
 * @param listState 滚动状态
 * @param tableMetadata 表格元数据
 */
@Composable
fun WLTableWithHistory(
    currentItems: List<TableDisplayItem>,
    previousItems: List<TableDisplayItem>,
    listState: LazyListState,
    tableMetadata: TableMetadata,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // First 5 columns from previous table with light background
        if (previousItems.isNotEmpty()) {
            GameTable(
                items = previousItems.takeLast(5),
                listState = listState,
                tableType = TableType.WL,
                tableMetadata = tableMetadata,
                showCharts = false,
                showHistory = true,
                modifier = Modifier
            )
        }

        // Current table data
        GameTable(
            items = currentItems,
            listState = listState,
            tableType = TableType.WL,
            tableMetadata = tableMetadata,
            showCharts = false,
            showHistory = false,
            modifier = Modifier
        )
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/table/WLTableWithHistory.kt
git commit -m "feat(table): create WLTableWithHistory for 25-column renewal"
```

---

## Phase 1.3: Business Logic

### Task 12: Add Opposite Pairs Map in ViewModel

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt`

**Step 1: Add companion object with opposite pairs**

Add near the top of the class (after property declarations):

```kotlin
private companion object {
    // V2: 正反形态映射
    private val oppositePairs = mapOf(
        1 to 2, 2 to 1,
        3 to 4, 4 to 3,
        5 to 6, 6 to 5,
        7 to 8, 8 to 7
    )
}

private fun isOppositePair(num1: Int, num2: Int): Boolean {
    return oppositePairs[num1] == num2
}
```

**Step 2: Add necessary imports**

```kotlin
import com.dsd.baccarat.data.CircleMarkType
import com.dsd.baccarat.data.CircleType
import com.dsd.baccarat.data.DisplayMarks
import com.dsd.baccarat.data.TableType
import com.dsd.baccarat.ui.game.state.AlarmType
import com.dsd.baccarat.ui.game.state.TableMetadata
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt
git commit -m "feat(viewmodel): add opposite pairs map for circle mark logic"
```

---

### Task 13: Implement BP Circle Mark Logic

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt`

**Step 1: Add applyBpCircleMarks function**

Add in the business logic section:

```kotlin
/**
 * 应用BP表格套圈标记
 */
private fun applyBpCircleMarks(
    tableData: List<TableDisplayItem>,
    markType: CircleMarkType
): List<TableDisplayItem> {
    return tableData.mapIndexed { index, item ->
        val realItem = item as? TableDisplayItem.Real ?: return@mapIndexed item
        val currentNum = realItem.data.dataA?.second ?: return@mapIndexed item

        val circleType = when (markType) {
            CircleMarkType.ZF -> {
                // Check adjacent positions
                val prevNum = if (index > 0)
                    (tableData[index - 1] as? TableDisplayItem.Real)?.data?.dataA?.second
                else null
                val nextNum = if (index < tableData.size - 1)
                    (tableData[index + 1] as? TableDisplayItem.Real)?.data?.dataA?.second
                else null

                when {
                    prevNum != null && isOppositePair(currentNum, prevNum) -> CircleType.RED
                    nextNum != null && isOppositePair(currentNum, nextNum) -> CircleType.BLUE
                    else -> null
                }
            }
            CircleMarkType.ZF_SEP -> {
                // Check skip-one positions
                val prev2Num = if (index > 1)
                    (tableData[index - 2] as? TableDisplayItem.Real)?.data?.dataA?.second
                else null
                val next2Num = if (index < tableData.size - 2)
                    (tableData[index + 2] as? TableDisplayItem.Real)?.data?.dataA?.second
                else null

                when {
                    prev2Num != null && isOppositePair(currentNum, prev2Num) -> CircleType.RED
                    next2Num != null && isOppositePair(currentNum, next2Num) -> CircleType.BLUE
                    else -> null
                }
            }
            else -> null
        }

        if (circleType != null) {
            TableDisplayItem.Real(
                realItem.data.copy(
                    displayMarks = realItem.data.displayMarks?.copy(circleA = circleType)
                        ?: DisplayMarks(circleA = circleType)
                )
            )
        } else {
            item
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt
git commit -m "feat(viewmodel): implement BP circle mark logic (ZF and ZF_SEP)"
```

---

### Task 14: Implement W/L Circle Mark and Alarm Logic

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt`

**Step 1: Add applyWlCircleMarks function**

```kotlin
/**
 * 应用W/L表格套圈标记（数字2和7）
 */
private fun applyWlCircleMarks(
    tableData: List<TableDisplayItem>
): List<TableDisplayItem> {
    return tableData.map { item ->
        val realItem = item as? TableDisplayItem.Real ?: return@map item
        val number = realItem.data.dataA?.second

        val circleType = when (number) {
            2, 7 -> CircleType.BLUE
            else -> null
        }

        if (circleType != null) {
            TableDisplayItem.Real(
                realItem.data.copy(
                    displayMarks = realItem.data.displayMarks?.copy(circleA = circleType)
                        ?: DisplayMarks(circleA = circleType)
                )
            )
        } else {
            item
        }
    }
}
```

**Step 2: Add checkNumber2Alarm function**

```kotlin
/**
 * 检查数字2报警
 */
private fun checkNumber2Alarm(tableData: List<TableDisplayItem>) {
    val hasNumber2 = tableData.any { item ->
        (item as? TableDisplayItem.Real)?.data?.dataA?.second == 2
    }

    if (hasNumber2) {
        viewModelScope.launch {
            _sideEffect.emit(GameSideEffect.TriggerAlarm(AlarmType.NUMBER_2))
        }
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt
git commit -m "feat(viewmodel): implement W/L circle marks and number 2 alarm"
```

---

### Task 15: Implement 25-Column New Table Logic

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt`

**Step 1: Add checkAndCreateNewWlTable function**

```kotlin
/**
 * 检查并创建新的W/L表格（每25列）
 */
private fun checkAndCreateNewWlTable() {
    val currentColumnCount = _uiState.value.wlTableMeta.currentColumnCount

    if (currentColumnCount >= 25) {
        // Save last 5 columns from current table
        val lastFiveColumns = _uiState.value.wlTableData.takeLast(5)

        // Create new table
        _uiState.update { state ->
            state.copy(
                wlTableMeta = state.wlTableMeta.copy(
                    tableNumber = (state.wlTableMeta.tableNumber % 99999) + 1,
                    currentColumnCount = 0
                ),
                wlPreviousTableData = lastFiveColumns,
                wlTableData = emptyList()
            )
        }
    }
}
```

**Step 2: Update WL table update logic**

Find the existing function that updates WL table data and add column count tracking:

```kotlin
private fun updateWlTable() {
    // ... existing logic to add new item ...

    // After adding new item, increment column count
    _uiState.update { state ->
        state.copy(
            wlTableMeta = state.wlTableMeta.copy(
                currentColumnCount = state.wlTableMeta.currentColumnCount + 1
            )
        )
    }

    checkAndCreateNewWlTable()
}
```

**Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt
git commit -m "feat(viewmodel): implement 25-column auto-renewal for W/L table"
```

---

### Task 16: Implement Table Number Increment on Save

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt`

**Step 1: Update handleSaveGame function**

Find the existing save function and add table number increment:

```kotlin
private fun handleSaveGame() {
    viewModelScope.launch {
        // ... existing save logic ...

        // Increment BP table number on save
        _uiState.update { state ->
            state.copy(
                bpTableMeta = state.bpTableMeta.copy(
                    tableNumber = (state.bpTableMeta.tableNumber % 99999) + 1
                )
            )
        }

        onSaveOrNewGame()
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt
git commit -m "feat(viewmodel): implement table number increment on save"
```

---

### Task 17: Add Circle Mark Event Handlers

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt`

**Step 1: Add event handling in onEvent function**

```kotlin
fun onEvent(event: GameUiEvent) {
    when (event) {
        // ... existing events ...

        is GameUiEvent.ToggleCircleZF -> handleToggleCircleZF(event.tableType)
        is GameUiEvent.ToggleCircleZFSep -> handleToggleCircleZFSep(event.tableType)
    }
}
```

**Step 2: Implement handleToggleCircleZF**

```kotlin
private fun handleToggleCircleZF(tableType: TableType) {
    when (tableType) {
        TableType.BP -> {
            val isCurrentlyEnabled = _uiState.value.bpCircleMarkType == CircleMarkType.ZF
            if (isCurrentlyEnabled) {
                // Disable and clear marks
                _uiState.update { it.copy(bpCircleMarkType = null) }
                // Clear display marks from table data
                val clearedData = _uiState.value.bppcTableData.map { item ->
                    (item as? TableDisplayItem.Real)?.let { realItem ->
                        TableDisplayItem.Real(
                            realItem.data.copy(displayMarks = null)
                        )
                    } ?: item
                }
                _uiState.update { it.copy(bppcTableData = clearedData) }
            } else {
                _uiState.update { it.copy(bpCircleMarkType = CircleMarkType.ZF) }
                val markedData = applyBpCircleMarks(_uiState.value.bppcTableData, CircleMarkType.ZF)
                _uiState.update { it.copy(bppcTableData = markedData) }
            }
        }
        TableType.WL -> {
            // W/L uses automatic circle marks, not ZF button
        }
    }
}
```

**Step 3: Implement handleToggleCircleZFSep**

```kotlin
private fun handleToggleCircleZFSep(tableType: TableType) {
    when (tableType) {
        TableType.BP -> {
            val isCurrentlyEnabled = _uiState.value.bpCircleMarkType == CircleMarkType.ZF_SEP
            if (isCurrentlyEnabled) {
                _uiState.update { it.copy(bpCircleMarkType = null) }
                val clearedData = _uiState.value.bppcTableData.map { item ->
                    (item as? TableDisplayItem.Real)?.let { realItem ->
                        TableDisplayItem.Real(
                            realItem.data.copy(displayMarks = null)
                        )
                    } ?: item
                }
                _uiState.update { it.copy(bppcTableData = clearedData) }
            } else {
                _uiState.update { it.copy(bpCircleMarkType = CircleMarkType.ZF_SEP) }
                val markedData = applyBpCircleMarks(_uiState.value.bppcTableData, CircleMarkType.ZF_SEP)
                _uiState.update { it.copy(bppcTableData = markedData) }
            }
        }
        TableType.WL -> {}
    }
}
```

**Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt
git commit -m "feat(viewmodel): add circle mark event handlers"
```

---

## Phase 1.4: Integration

### Task 18: Update LeftSideSection with ZF/Z/F Buttons

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/ui/game/components/layout/LeftSideSection.kt`

**Step 1: Add ZF and Z/F buttons**

Find where buttons are rendered and add:

```kotlin
// Add circle mark buttons for BP table
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(
        onClick = { onEvent(GameUiEvent.ToggleCircleZF(TableType.BP)) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (uiState.bpCircleMarkType == CircleMarkType.ZF)
                Color.Gray else Color.Blue
        )
    ) {
        Text("ZF")
    }

    Button(
        onClick = { onEvent(GameUiEvent.ToggleCircleZFSep(TableType.BP)) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (uiState.bpCircleMarkType == CircleMarkType.ZF_SEP)
                Color.Gray else Color.Blue
        )
    ) {
        Text("Z/F")
    }
}
```

**Step 2: Update GameTable call with new parameters**

```kotlin
GameTable(
    items = uiState.bppcTableData,
    listState = bppcListState,
    tableType = TableType.BP,
    tableMetadata = uiState.bpTableMeta,
    showCharts = true,
    showHistory = false,
    modifier = Modifier
)
```

**Step 3: Update counter display order**

Find the counter display section and update to:
```kotlin
Text("#${uiState.bppcCounter.count1 + uiState.bppcCounter.count2}")
Text("P${uiState.bppcCounter.count2}")
Text("B${uiState.bppcCounter.count1}")
val diff = uiState.bppcCounter.count2 - uiState.bppcCounter.count1
Text(
    "${if (diff >= 0) "+" else ""}$diff",
    color = if (diff >= 0) Color.Red else Color.Blue
)
```

**Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/layout/LeftSideSection.kt
git commit -m "feat(ui): add ZF/Z/F buttons to BP table section"
```

---

### Task 19: Update RightSideSection with WLTableWithHistory

**Files:**
- Modify: `app/src/main/java/com/dsd/baccarat/ui/game/components/layout/RightSideSection.kt`

**Step 1: Replace GameTable with WLTableWithHistory**

```kotlin
WLTableWithHistory(
    currentItems = uiState.wlTableData,
    previousItems = uiState.wlPreviousTableData,
    listState = wlListState,
    tableMetadata = uiState.wlTableMeta,
    modifier = Modifier
)
```

**Step 2: Handle alarm side effect**

Add LaunchedEffect for alarm:

```kotlin
LaunchedEffect(viewModel.sideEffect) {
    viewModel.sideEffect.collect { sideEffect ->
        when (sideEffect) {
            is GameSideEffect.TriggerAlarm -> {
                when (sideEffect.alarmType) {
                    AlarmType.NUMBER_2 -> {
                        // Play sound and trigger visual alarm
                        // Implementation depends on your sound/notification system
                    }
                }
            }
        }
    }
}
```

**Step 3: Update counter display order**

```kotlin
Text("#${uiState.wlCounter.count1 + uiState.wlCounter.count2}")
Text("L${uiState.wlCounter.count2}")
Text("W${uiState.wlCounter.count1}")
val diff = uiState.wlCounter.count1 - uiState.wlCounter.count2
Text(
    "${if (diff >= 0) "+" else ""}$diff",
    color = if (diff >= 0) Color.Red else Color.Blue
)
```

**Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/java/com/dsd/baccarat/ui/game/components/layout/RightSideSection.kt
git commit -m "feat(ui): integrate WLTableWithHistory and alarm handling"
```

---

### Task 20: Write Unit Tests for Circle Mark Logic

**Files:**
- Create: `app/src/test/java/com/dsd/baccarat/viewmodel/GameViewModelCircleMarkTest.kt`

**Step 1: Create test class**

```kotlin
package com.dsd.baccarat.viewmodel

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameViewModelCircleMarkTest {

    @Test
    fun `isOppositePair returns true for 1 and 2`() {
        // 1 and 2 are opposite pairs (BBB/PPP)
        assertEquals(true, isOppositePair(1, 2))
        assertEquals(true, isOppositePair(2, 1))
    }

    @Test
    fun `isOppositePair returns true for 3 and 4`() {
        // 3 and 4 are opposite pairs (BPP/PBB)
        assertEquals(true, isOppositePair(3, 4))
        assertEquals(true, isOppositePair(4, 3))
    }

    @Test
    fun `isOppositePair returns false for non-opposite pairs`() {
        assertEquals(false, isOppositePair(1, 3))
        assertEquals(false, isOppositePair(2, 4))
        assertEquals(false, isOppositePair(1, 1))
    }

    @Test
    fun `applyBpCircleMarks ZF marks adjacent opposites with red circle`() {
        // Test data: [1, 2, 3]
        // Expected: 1 gets red circle (2 is opposite and adjacent)
        // ... implement test
    }

    @Test
    fun `applyBpCircleMarks ZF_SEP marks skip-one opposites with red circle`() {
        // Test data: [1, 5, 2]
        // Expected: 1 gets red circle (2 is opposite and skip-one)
        // ... implement test
    }
}
```

**Step 2: Run tests**

Run: `./gradlew test --tests "GameViewModelCircleMarkTest"`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add app/src/test/java/com/dsd/baccarat/viewmodel/GameViewModelCircleMarkTest.kt
git commit -m "test(viewmodel): add unit tests for circle mark logic"
```

---

### Task 21: Write Unit Tests for Table Metadata

**Files:**
- Create: `app/src/test/java/com/dsd/baccarat/ui/game/state/TableMetadataTest.kt`

**Step 1: Create test class**

```kotlin
package com.dsd.baccarat.ui.game.state

import org.junit.Test
import kotlin.test.assertEquals

class TableMetadataTest {

    @Test
    fun `displayNumber formats 1 as 00001`() {
        val metadata = TableMetadata(tableNumber = 1)
        assertEquals("00001", metadata.displayNumber)
    }

    @Test
    fun `displayNumber formats 123 as 00123`() {
        val metadata = TableMetadata(tableNumber = 123)
        assertEquals("00123", metadata.displayNumber)
    }

    @Test
    fun `displayNumber formats 99999 correctly`() {
        val metadata = TableMetadata(tableNumber = 99999)
        assertEquals("99999", metadata.displayNumber)
    }

    @Test
    fun `displayNumber wraps around from 99999 to 00001`() {
        val metadata = TableMetadata(tableNumber = 100000)
        // Should wrap around to 1 -> 00001
        assertEquals("00001", metadata.displayNumber)
    }
}
```

**Step 2: Run tests**

Run: `./gradlew test --tests "TableMetadataTest"`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add app/src/test/java/com/dsd/baccarat/ui/game/state/TableMetadataTest.kt
git commit -m "test(state): add unit tests for TableMetadata formatting"
```

---

### Task 22: Final Integration Test

**Step 1: Build and install app**

Run: `./gradlew clean installDebug`
Expected: BUILD SUCCESSFUL, app installs

**Step 2: Manual testing checklist**

- [ ] BP table shows table number (00001) in top-left
- [ ] BP table has column separators at 5, 10, 15, 20
- [ ] ZF button toggles adjacent opposite circle marks
- [ ] Z/F button toggles skip-one opposite circle marks
- [ ] Circles display correctly (red for previous opposite, blue for next opposite)
- [ ] BP counter shows: #Total, P, B, +BPdiff/-BPdiff
- [ ] Saving increments BP table number

- [ ] W/L table shows table number in bottom-right
- [ ] W/L table first 5 columns show previous table data (light background)
- [ ] Numbers 2 and 7 have blue circles
- [ ] Number 2 triggers flashing + sound (if sound implemented)
- [ ] After 25 columns, W/L table creates new table with incremented number
- [ ] Last 5 columns of old table appear as first 5 of new table
- [ ] W/L counter shows: #Total, L, W, +WLdiff/-WLdiff

- [ ] Bar chart shows 8 display windows with counts
- [ ] Minimum count pattern has light background
- [ ] Bar colors: 12/56 red, 34/78 blue
- [ ] Bar thickness: 1357 thin, 2468 thick
- [ ] Paired bars have equal height (1=2, 3=4, 5=6, 7=8)

**Step 3: Fix any issues found**

If bugs found, create fix commits with:
```bash
git commit -m "fix: description of bug fix"
```

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete Phase 1 core table upgrades

- BP table: numbering, separators, circle marks (ZF/Z/F)
- W/L table: 25-column renewal, history columns, number 2 alarm
- Bar chart: 8 display windows, color/thickness rules

Refs: docs/plans/2026-02-11-phase1-table-upgrades-design.md"
```

---

## Execution Options

**Plan complete and saved to `docs/plans/2026-02-11-phase1-implementation-plan.md`.**

**Two execution options:**

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach?**
