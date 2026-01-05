# UDF 架构快速入门

## 什么是 UDF（单向数据流）？

UDF 是 Google 官方推荐的 Jetpack Compose 应用架构模式：

```
UI 组件 → 用户操作 → Event → ViewModel → 更新 State → UI 更新
```

### 核心概念

1. **State（状态）** - UI 所需的所有数据，不可变
2. **Event（事件）** - 用户的所有操作，密封类
3. **ViewModel** - 处理事件，更新状态
4. **UI** - 纯展示组件，接收 State 和 Event 回调

## 新架构使用指南

### 1. 添加新功能

#### 步骤 1: 更新 UI State

在 `GameUiState.kt` 中添加新状态：

```kotlin
data class GameUiState(
    // ... 现有状态
    val isNewFeatureEnabled: Boolean = false,  // 添加新状态
)
```

#### 步骤 2: 添加 UI Event

在 `GameUiEvent.kt` 中添加新事件：

```kotlin
sealed interface GameUiEvent {
    // ... 现有事件
    data object ToggleNewFeature : GameUiEvent  // 添加新事件
}
```

#### 步骤 3: 在 ViewModel 中处理

在 `GameViewModel.kt` 的 `onEvent()` 方法中添加处理逻辑：

```kotlin
fun onEvent(event: GameUiEvent) {
    when (event) {
        // ... 现有事件处理
        is GameUiEvent.ToggleNewFeature -> {
            _uiState.update { it.copy(
                isNewFeatureEnabled = !it.isNewFeatureEnabled
            )}
        }
    }
}
```

#### 步骤 4: 在 UI 中使用

在组件中读取状态并触发事件：

```kotlin
@Composable
fun MyFeatureComponent(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit
) {
    Switch(
        checked = uiState.isNewFeatureEnabled,
        onCheckedChange = { onEvent(GameUiEvent.ToggleNewFeature) }
    )
}
```

### 2. 组件开发原则

#### ✅ 正确做法

```kotlin
// 组件只接收数据参数和事件回调
@Composable
fun MyComponent(
    title: String,
    count: Int,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(onClick = onClick, enabled = isEnabled) {
        Text("$title: $count")
    }
}
```

#### ❌ 错误做法

```kotlin
// 不要直接依赖 ViewModel
@Composable
fun MyComponent(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    Button(onClick = { viewModel.onEvent(...) }) {
        // ...
    }
}
```

### 3. 状态访问

在 `GameScreen.kt` 中访问状态：

```kotlin
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 传递给子组件
    MyComponent(
        count = uiState.bppcCounter.count1,
        onClick = { viewModel.onEvent(GameUiEvent.OpenB) }
    )
}
```

### 4. 副作用处理

对于导航、Toast 等一次性操作：

```kotlin
// 1. 定义副作用
sealed interface GameSideEffect {
    data class ShowToast(val message: String) : GameSideEffect
}

// 2. 在 ViewModel 中发送
private fun handleSomething() {
    _sideEffect.tryEmit(GameSideEffect.ShowToast("操作成功"))
}

// 3. 在 UI 中收集
LaunchedEffect(viewModel.sideEffect) {
    viewModel.sideEffect.collect { sideEffect ->
        when (sideEffect) {
            is GameSideEffect.ShowToast -> {
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

## 旧代码迁移指南

### 场景 1: 从旧的 ViewModel 迁移

**旧代码：**
```kotlin
// DefaultViewModel.kt
private val mCounter = MutableStateFlow(0)
val counter: StateFlow<Int> = mCounter.asStateFlow()

fun increment() {
    mCounter.value++
}
```

**新代码：**
```kotlin
// GameViewModel.kt
private val _uiState = MutableStateFlow(GameUiState())
val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

fun onEvent(event: GameUiEvent) {
    when (event) {
        is GameUiEvent.Increment -> {
            _uiState.update { it.copy(counter = it.counter + 1) }
        }
    }
}
```

### 场景 2: 从旧组件迁移

**旧代码：**
```kotlin
@Composable
fun MyComponent(viewModel: DefaultViewModel) {
    val counter by viewModel.counter.collectAsState()
    Button(onClick = { viewModel.increment() }) {
        Text("Count: $counter")
    }
}
```

**新代码：**
```kotlin
@Composable
fun MyComponent(
    counter: Int,
    onIncrement: () -> Unit
) {
    Button(onClick = onIncrement) {
        Text("Count: $counter")
    }
}
```

## 常见问题

### Q: 为什么要使用单一状态类？

A: 单一状态类确保：
- 只有一个状态源，避免状态不一致
- 状态更新原子性，UI 始终看到一致的状态
- 更容易测试和调试

### Q: 事件为什么使用密封类？

A: 密封类提供：
- 编译时类型安全
- when 表达式完整性检查
- 更好的可读性和维护性

### Q: 什么时候使用副作用？

A: 副作用用于：
- 导航到其他屏幕
- 显示 Toast/Snackbar
- 播放声音
- 请求权限
- 任何一次性、不可重现的操作

### Q: 如何调试状态变化？

A: 在 ViewModel 中添加日志：

```kotlin
fun onEvent(event: GameUiEvent) {
    Log.d("GameViewModel", "Received event: $event")
    when (event) {
        // ...
    }
    Log.d("GameViewModel", "New state: ${_uiState.value}")
}
```

## 最佳实践

1. **保持状态最小化** - 只在 State 中保存 UI 需要的数据
2. **业务逻辑在 ViewModel** - UI 组件应该是纯函数
3. **使用不可变数据** - 状态更新总是创建新实例
4. **避免状态重复** - 不要在多个地方存储相同数据
5. **及时清理副作用** - 使用 DisposableEffect 清理资源

## 学习资源

- [Google UDF 指南](https://developer.android.com/jetpack/compose/state)
- [Compose 状态管理](https://developer.android.com/jetpack/compose/state-management)
- [Now in Android](https://github.com/android/nowinandroid) - Google 官方示例项目

## 获取帮助

如有问题，请参考：
- `REFACTOR_PLAN.md` - 架构设计文档
- `REFACTOR_SUMMARY.md` - 重构总结
- 旧代码文件中的注释和文档
