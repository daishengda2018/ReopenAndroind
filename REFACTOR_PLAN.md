# 单向数据流（UDF）重构设计文档

## 1. 当前架构问题分析

### 1.1 主要问题
1. **Screen.kt 文件过大**（1129行）：包含太多UI组件和逻辑
2. **缺少明确的 UI State 模型**：数据分散在多个 StateFlow 中
3. **事件处理混乱**：ViewModel 中有很多函数直接被 UI 调用
4. **UI 组件耦合度高**：组件直接依赖 ViewModel，而不是纯数据参数
5. **不完全符合 UDF 模式**：State 和 Event 没有明确分离

### 1.2 当前结构
```
com.dsd.baccarat/
├── ui/compose/
│   └── Screen.kt (1129行) ❌
├── model/
│   └── DefaultViewModel.kt (976行) ❌
└── data/
    └── DataStructure.kt
```

## 2. 新架构设计（遵循 Google 最佳实践）

### 2.1 UDF 核心概念
- **UI State**: 不可变的数据类，包含 UI 所需的所有状态
- **UI Event**: 密封类，表示用户操作产生的所有事件
- **ViewModel**: 处理事件，更新状态（Reducer 模式）
- **UI Components**: 纯函数式组件，只负责展示

### 2.2 新目录结构

```
com.dsd.baccarat/
├── ui/
│   ├── game/                          # 游戏主屏幕
│   │   ├── GameScreen.kt             # 主屏幕入口
│   │   ├── components/               # UI 组件
│   │   │   ├── layout/               # 布局组件
│   │   │   │   ├── LeftSideSection.kt
│   │   │   │   └── RightSideSection.kt
│   │   │   ├── counter/              # 计数器组件
│   │   │   │   └── CounterDisplay.kt
│   │   │   ├── table/                # 表格组件
│   │   │   │   ├── BppcTable.kt
│   │   │   │   ├── WlTable.kt
│   │   │   │   └── VerticalBarChart.kt
│   │   │   ├── strategy/             # 策略组件
│   │   │   │   ├── Strategy3WaysDisplay.kt
│   │   │   │   ├── StrategyGridDisplay.kt
│   │   │   │   └── StrategyTable.kt
│   │   │   ├── input/                # 输入组件
│   │   │   │   ├── InputButtons.kt
│   │   │   │   └── DateSelectDialog.kt
│   │   │   └── common/               # 通用组件
│   │   │       ├── TextItem.kt
│   │   │       ├── CurrentTimeDisplay.kt
│   │   │       └── NotificationSoundEffect.kt
│   │   └── state/                    # UI State 和 Event
│   │       ├── GameUiState.kt        # UI 状态
│   │       └── GameUiEvent.kt        # UI 事件
│   ├── history/                      # 历史记录屏幕
│   │   └── (类似结构)
│   └── theme/                        # 主题
│       └── (保持不变)
├── viewmodel/
│   └── GameViewModel.kt             # 重构后的 ViewModel
├── data/
│   ├── model/                        # 数据模型
│   │   ├── (现有模型保持不变)
│   ├── repository/                   # 数据仓库
│   │   └── (现有仓库保持不变)
│   └── room/                         # 数据库
│       └── (现有数据库保持不变)
└── di/                              # 依赖注入
    └── (现有 DI 保持不变)
```

## 3. UI State 设计

### 3.1 GameUiState.kt
```kotlin
data class GameUiState(
    // 计时器状态
    val timerState: TimerState = TimerState(),

    // BP 计数器
    val bppcCounter: Counter = Counter(),

    // WL 计数器
    val wlCounter: Counter = Counter(),

    // BPPC 表格数据
    val bppcTableData: List<TableDisplayItem> = emptyList(),

    // WL 表格数据
    val wlTableData: List<TableDisplayItem> = emptyList(),

    // 策略 3 路数据
    val strategy3WaysList: List<Strategy3WaysData> = emptyList(),

    // 策略网格数据
    val strategyGridList: List<StrategyGridInfo> = emptyList(),

    // 预测数据
    val predictedList: List<PredictedStrategy3WaysValue> = emptyList(),

    // 当前押注状态
    val currentBetInput: InputEntity? = null,

    // 历史计数器
    val historyWCount: Int = 0,
    val historyLCount: Int = 0,

    // 输入文本
    val inputText: String = "",

    // 只显示新游戏
    val isOnlyShowNewGame: Boolean = false,

    // 日期选择对话框状态
    val dateSelectionState: DateSelectionState = DateSelectionState(),

    // 历史模式标记
    val isHistoryMode: Boolean = false,
    val historyStartTime: Long = 0L,
    val historyEndTime: Long = 0L
)

data class TimerState(
    val status: TimerStatus = TimerStatus.Idle,
    val elapsedSeconds: Int = 0,
    val showReminder: Boolean = false
)

data class DateSelectionState(
    val isDialogVisible: Boolean = false,
    val availableDates: List<GameSessionEntity> = emptyList(),
    val selectedDate: GameSessionEntity? = null,
    val isLoading: Boolean = false
)
```

### 3.2 GameUiEvent.kt
```kotlin
sealed interface GameUiEvent {
    // 输入事件
    data object OpenB : GameUiEvent
    data object OpenP : GameUiEvent
    data object RemoveLastOpen : GameUiEvent

    // 押注事件
    data object BetB : GameUiEvent
    data object BetP : GameUiEvent
    data object RemoveLastBet : GameUiEvent

    // 计时器事件
    data object StartTimer : GameUiEvent
    data object PauseOrResumeTimer : GameUiEvent
    data object StopTimer : GameUiEvent
    data object DismissReminder : GameUiEvent

    // 游戏管理事件
    data object NewGame : GameUiEvent
    data object SaveGame : GameUiEvent

    // 文本输入事件
    data class UpdateInputText(val text: String) : GameUiEvent

    // 日期选择事件
    data object ShowDateSelectDialog : GameUiEvent
    data object DismissDialog : GameUiEvent
    data class SelectDate(val date: GameSessionEntity) : GameUiEvent
    data object ConfirmDateSelection : GameUiEvent
}
```

## 4. ViewModel 重构

### 4.1 职责划分
- **状态管理**：单一 `StateFlow<GameUiState>`
- **事件处理**：`onEvent(GameUiEvent)` 方法
- **业务逻辑**：将复杂逻辑提取到独立的 UseCase 或 Repository

### 4.2 核心 API
```kotlin
class GameViewModel @Inject constructor(
    // 依赖注入
) : ViewModel() {

    // 唯一的 UI 状态流
    val uiState: StateFlow<GameUiState>

    // 事件处理
    fun onEvent(event: GameUiEvent)

    // 副作用（一次性事件）
    val sideEffect: SharedFlow<GameSideEffect>
}

sealed interface GameSideEffect {
    data object PlaySound : GameSideEffect
    data class ShowToast(val message: String) : GameSideEffect
    data class NavigateToHistory(val gameId: String) : GameSideEffect
}
```

## 5. UI 组件重构原则

### 5.1 组件拆分原则
- **单一职责**：每个组件只负责一个功能
- **纯函数式**：组件不持有状态，只接收参数
- **可复用性**：通过参数控制组件行为

### 5.2 组件签名
```kotlin
// ❌ 旧方式（直接依赖 ViewModel）
@Composable
private fun CounterDisplay(viewModel: DefaultViewModel) { ... }

// ✅ 新方式（纯数据参数）
@Composable
fun CounterDisplay(
    value1: Int,
    value2: Int,
    color1: Color,
    color2: Color,
    modifier: Modifier = Modifier
) { ... }
```

## 6. 重构步骤

### Phase 1: 创建新结构（不影响现有功能）
1. 创建新的目录结构
2. 定义 `GameUiState` 和 `GameUiEvent`
3. 创建新的 `GameViewModel`（保留旧 ViewModel）

### Phase 2: 拆分 Screen.kt
1. 提取通用组件到 `components/common/`
2. 提取功能组件到各自的目录
3. 创建新的 `GameScreen` 入口

### Phase 3: 重构 ViewModel
1. 实现 UDF 模式的 `GameViewModel`
2. 迁移业务逻辑
3. 添加单元测试

### Phase 4: 集成和验证
1. 更新依赖注入
2. 运行应用并验证功能
3. 对比测试确保无副作用

### Phase 5: 清理
1. 删除旧的 `DefaultViewModel`
2. 删除旧的 `Screen.kt`
3. 更新文档

## 7. 兼容性保证

### 7.1 数据模型不变
- 所有 `data/` 包下的类保持不变
- Room 数据库 Entity 不变
- Repository 接口不变

### 7.2 业务逻辑不变
- 所有算法逻辑完全保留
- 状态转换逻辑保持一致
- 副作用处理方式相同

## 8. 测试策略

### 8.1 单元测试
- ViewModel 的事件处理
- Reducer 的状态转换
- UseCase 的业务逻辑

### 8.2 UI 测试
- Compose UI 组件测试
- UI 交互流程测试

### 8.3 集成测试
- 完整的用户流程测试
- 数据持久化测试

## 9. 预期收益

1. **可维护性提升**：代码结构清晰，易于理解和修改
2. **可测试性提升**：组件解耦，便于单元测试
3. **可复用性提升**：组件独立，可在多处使用
4. **符合最佳实践**：遵循 Google 官方推荐的架构模式
5. **团队协作友好**：目录结构清晰，便于多人协作
