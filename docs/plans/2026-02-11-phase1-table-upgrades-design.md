# Phase 1: Core Table Upgrades Design

**Date:** 2026-02-11
**Version:** V2 Requirements
**Estimated Effort:** 14 person-days (BP表格3天 + W/L输赢表格4天 + 数字形态柱状图2天 + 集成测试5天)

---

## Overview

本文档描述V2需求中Phase 1的核心表格升级设计，包括BP表格、W/L输赢表格和数字形态柱状图的升级方案。

---

## 1. Architecture Approach

### Design Decision: Minimal Changes (Approach A)

扩展现有 `GameTable` 组件而非创建新组件，通过条件渲染支持不同表格类型。

**优点：**
- 代码复用性高，减少重复
- 保持现有滚动优化和性能
- 更易测试和调试
- 实施速度更快

---

## 2. Data Model Changes

### 2.1 Table Metadata

在 `GameUiState.kt` 中添加表格级别元数据：

```kotlin
@Immutable
data class GameUiState(
    // ... 现有字段 ...

    // V2: 表格元数据
    val bpTableMeta: TableMetadata = TableMetadata(),
    val wlTableMeta: TableMetadata = TableMetadata(),

    // W/L表格专用：上一张表的最后5列
    val wlPreviousTableData: List<TableDisplayItem> = emptyList()
)

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

### 2.2 Display Marks

扩展 `TableItem` 支持V2显示特性：

```kotlin
@Immutable
data class TableItem(
    // V1 字段保持不变
    val dataA: Pair<Boolean, Int?>? = null,
    val dataB: Pair<Boolean, Int?>? = null,
    val dataC: Pair<Boolean, Int?>? = null,

    // V2 显示标记
    val displayMarks: DisplayMarks? = null
)

@Immutable
data class DisplayMarks(
    val circleA: CircleType? = null,       // 套圈标记（红/蓝）
    val circleB: CircleType? = null,
    val circleC: CircleType? = null,
    val wlSymbolA: WLSymbol? = null,       // W/L同步符号
    val wlSymbolB: WLSymbol? = null,
    val wlSymbolC: WLSymbol? = null,
    val isLightBackground: Boolean = false // 浅底色标记（W/L前5列）
)

@Immutable
enum class CircleType { RED, BLUE, BOTH }

@Immutable
enum class WLSymbol { WIN, LOSS }
```

### 2.3 New Enums

```kotlin
enum class TableType {
    BP,    // 开牌结果表格（显示B或P）
    WL     // 押注输赢表格（显示W或L）
}

enum class CircleMarkType {
    ZF,         // 紧连的正反形态（用于BP、连跳、随机表格）
    ZF_SEP,     // 隔一的正反形态（用于BP、连跳、随机表格）
    CIRCLE_12,  // BP形态押注表格专用
    CIRCLE_34,
    CIRCLE_56,
    CIRCLE_78,
    WL_ALARM    // W/L表格数字2和7的蓝色圈
}
```

---

## 3. UI Component Changes

### 3.1 GameTable Component

**文件：** `ui/game/components/table/GameTable.kt`

**改造要点：**

```kotlin
@Composable
fun GameTable(
    items: List<TableDisplayItem>,
    listState: LazyListState,
    tableType: TableType,
    tableMetadata: TableMetadata,
    showCharts: Boolean = false,
    showHistory: Boolean = false,
    modifier: Modifier = Modifier
)
```

**新增功能：**
1. **表格类型支持** - 根据TableType渲染不同内容
2. **表格编号显示** - 左上角显示5位编号（00001-99999）
3. **列分隔线** - 第5、15列细线，第10、20列粗线
4. **套圈标记** - 调用CircleOverlay组件

### 3.2 CircleOverlay Component

**文件：** `ui/game/components/table/CircleOverlay.kt` (新增)

在数字周围绘制套圈：

```kotlin
@Composable
fun CircleOverlay(
    circleType: CircleType?,
    isFlashing: Boolean = false,  // 数字2报警时闪烁
    modifier: Modifier = Modifier
)
```

### 3.3 VerticalBarChart Upgrade

**文件：** `ui/game/components/table/VerticalBarChart.kt` (改造)

**V2新特性：**
1. **颜色规则** - 12/56红色，34/78蓝色
2. **线条粗细** - 1357细线条，2468粗线条
3. **等高逻辑** - 1和2等高，3和4等高，5和6等高，7和8等高

```kotlin
val color = when (number) {
    1, 2, 5, 6 -> Color.Red
    3, 4, 7, 8 -> Color.Blue
    else -> Color.Gray
}

val strokeWidth = when (number) {
    1, 3, 5, 7 -> 1.dp    // 细线条
    2, 4, 6, 8 -> 2.dp    // 粗线条
    else -> 1.dp
}
```

### 3.4 BarChartWithDisplay Component

**文件：** `ui/game/components/table/BarChartWithDisplay.kt` (新增)

包含8个显示窗和底色逻辑：

```kotlin
@Composable
fun BarChartWithDisplay(
    chartData: List<Pair<Boolean, Int?>?>,
    modifier: Modifier = Modifier
) {
    // 计算每个形态的数量
    val counts = (1..8).associateWith { num ->
        chartData.count { it?.second == num }
    }

    // 找出数量最少的形态
    val minCountNumber = counts.minByOrNull { it.value }?.key

    Column {
        // 8个显示窗
        Row {
            (1..8).forEach { num ->
                DisplayWindow(
                    number = num,
                    count = counts[num] ?: 0,
                    showBackground = num == minCountNumber
                )
            }
        }

        // 柱状图
        LazyRow {
            items(chartData) { item ->
                VerticalBarChart(value = item)
            }
        }
    }
}
```

### 3.5 WLTableWithHistory Component

**文件：** `ui/game/components/table/WLTableWithHistory.kt` (新增)

W/L表格专用，支持前5列显示上一张表数据：

```kotlin
@Composable
fun WLTableWithHistory(
    currentItems: List<TableDisplayItem>,
    previousItems: List<TableDisplayItem>,
    listState: LazyListState,
    tableMetadata: TableMetadata,
    modifier: Modifier = Modifier
)
```

---

## 4. Business Logic

### 4.1 Circle Mark Logic

**文件：** `viewmodel/GameViewModel.kt`

#### BP表格套圈（ZF和Z/F）

```kotlin
private fun applyBpCircleMarks(
    tableData: List<TableDisplayItem>,
    markType: CircleMarkType
): List<TableDisplayItem>

// 正反形态定义
private val oppositePairs = mapOf(
    1 to 2, 2 to 1,
    3 to 4, 4 to 3,
    5 to 6, 6 to 5,
    7 to 8, 8 to 7
)

private fun isOppositePair(num1: Int, num2: Int): Boolean {
    return oppositePairs[num1] == num2
}
```

**规则说明：**
- **ZF（紧连）**：检查相邻位置，若前一个是正反形态则红圈，后一个则蓝圈
- **Z/F（隔一）**：检查隔一个位置，若前二是正反形态则红圈，后二则蓝圈

#### W/L表格套圈（数字2和7）

```kotlin
private fun applyWlCircleMarks(
    tableData: List<TableDisplayItem>
): List<TableDisplayItem> {
    return tableData.map { item ->
        val number = item.data.dataA?.second
        val circleType = when (number) {
            2, 7 -> CircleType.BLUE
            else -> null
        }
        updateTableItemWithCircle(item, circleType)
    }
}

// 数字2报警逻辑
private fun checkNumber2Alarm(tableData: List<TableDisplayItem>) {
    val hasNumber2 = tableData.any {
        (it as? TableDisplayItem.Real)?.data?.dataA?.second == 2
    }

    if (hasNumber2) {
        _sideEffect.tryEmit(GameSideEffect.TriggerAlarm(AlarmType.NUMBER_2))
    }
}
```

### 4.2 W/L表格25列换新表逻辑

```kotlin
private fun checkAndCreateNewWlTable() {
    val currentColumnCount = _uiState.value.wlTableMeta.currentColumnCount

    if (currentColumnCount >= 25) {
        // 保存当前表格的最后5列
        val lastFiveColumns = _uiState.value.wlTableData.takeLast(5)

        // 创建新表格
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

fun addWlData(newItem: TableDisplayItem) {
    _uiState.update { state ->
        val updatedData = state.wlTableData + newItem
        state.copy(
            wlTableData = updatedData,
            wlTableMeta = state.wlTableMeta.copy(
                currentColumnCount = state.wlTableMeta.currentColumnCount + 1
            )
        )
    }
    checkAndCreateNewWlTable()
}
```

### 4.3 表格编号递增逻辑

**BP表格：**
- 保存时递增编号：`tableNumber = (tableNumber % 99999) + 1`

**W/L表格：**
- 每25列换新表时递增编号

---

## 5. UI Events

### 5.1 New Events

在 `GameUiEvent.kt` 中添加：

```kotlin
sealed interface GameUiEvent {
    // ... 现有事件 ...

    // V2: 套圈事件
    data class ToggleCircleZF(val tableType: TableType) : GameUiEvent
    data class ToggleCircleZFSep(val tableType: TableType) : GameUiEvent
}
```

---

## 6. File Structure

```
ui/game/
├── state/
│   ├── GameUiState.kt (扩展)
│   └── GameUiEvent.kt (扩展)
├── components/
│   ├── table/
│   │   ├── GameTable.kt (改造)
│   │   ├── WLTableWithHistory.kt (新增)
│   │   ├── VerticalBarChart.kt (改造)
│   │   ├── CircleOverlay.kt (新增)
│   │   └── BarChartWithDisplay.kt (新增)
│   └── common/
│       └── SeparatorComponents.kt (新增)
├── GameScreen.kt (更新调用)
└── GameViewModel.kt (扩展套圈逻辑)

data/
└── DataStructure.kt (扩展枚举和数据类)
```

---

## 7. Testing Strategy

### 7.1 Unit Tests

- [ ] ViewModel套圈逻辑测试
- [ ] 表格编号递增测试
- [ ] 25列换新表逻辑测试
- [ ] 正反形态识别测试

### 7.2 UI Tests

- [ ] 表格渲染测试
- [ ] 套圈显示测试
- [ ] 分隔线显示测试
- [ ] 表格编号显示测试

### 7.3 Integration Tests

- [ ] 完整BP表格工作流
- [ ] 完整W/L表格工作流
- [ ] 数字2报警功能
- [ ] 柱状图等高逻辑

---

## 8. Migration Strategy

### Phase 1.1: 数据模型准备
1. 扩展 `GameUiState` 添加表格元数据
2. 扩展 `TableItem` 添加显示标记
3. 添加新枚举类型

### Phase 1.2: UI组件改造
1. 改造 `GameTable` 支持表格类型
2. 创建 `CircleOverlay` 组件
3. 改造 `VerticalBarChart`
4. 创建 `BarChartWithDisplay`

### Phase 1.3: 业务逻辑实现
1. 实现套圈逻辑
2. 实现25列换新表逻辑
3. 实现表格编号递增
4. 实现数字2报警

### Phase 1.4: 集成和测试
1. 更新 `GameScreen` 调用新组件
2. 端到端测试
3. 性能优化

---

## 9. Acceptance Criteria

### BP表格
- [ ] 表格编号显示正确（左上角，5位数字）
- [ ] 第5、15列细线分隔，第10、20列粗线分隔
- [ ] ZF按钮给紧连正反形态套红蓝圈
- [ ] Z/F按钮给隔一正反形态套红蓝圈
- [ ] W/L同步符号正确显示在数字左上角
- [ ] 统计数据顺序：合计数(#), P数, B数, BP差数

### W/L输赢表格
- [ ] 前5列显示上一张表数据（浅底色）
- [ ] 数字2和7套蓝色圈
- [ ] 数字2出现时蓝色圈闪烁+声音报警
- [ ] 每25列自动换新表
- [ ] 表格编号正确递增（右下角）
- [ ] 统计数据顺序：合计数(#), L数, W数, WL差数
- [ ] 历史累计数据移到表格下方

### 数字形态柱状图
- [ ] 8个显示窗动态显示形态数量
- [ ] 数量最少的形态打底色
- [ ] 12/56红色，34/78蓝色
- [ ] 1和2等高，3和4等高，5和6等高，7和8等高
- [ ] 1357细线条，2468粗线条

---

## 10. References

- [V1需求文档](../需求V1.docx)
- [V2需求文档](../需求V2.docx)
- [需求V2报价方案](../需求V2报价方案.md)
- [REFACTOR_PLAN.md](../../REFACTOR_PLAN.md)
- [QUICK_START.md](../../QUICK_START.md)

---

**Document Status:** ✅ Complete
**Next Step:** Create implementation plan using `everything-claude-code:plan`
