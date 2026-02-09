---
name: reopen-android-patterns
description: Coding patterns extracted from ReopenAndroid - a Baccarat tracking app built with Kotlin, Jetpack Compose, and UDF architecture
version: 1.0.0
source: local-git-analysis
analyzed_commits: 200
last_updated: 2026-02-09
---

# ReopenAndroid Coding Patterns

A Baccarat game tracking and strategy analysis Android application following Google's UDF (Unidirectional Data Flow) architecture with Jetpack Compose.

## Project Overview

**Tech Stack:**
- **Language**: Kotlin 2.2.21
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: UDF (Unidirectional Data Flow) + MVVM
- **Dependency Injection**: Hilt
- **Database**: Room with KSP
- **Data Persistence**: DataStore Preferences
- **Async**: Coroutines + Flow
- **Build**: Gradle with Kotlin DSL
- **Code Quality**: Detekt (static analysis)

**Key Features:**
- Real-time Baccarat game tracking
- Multiple strategy analysis (3-ways, grid strategies)
- Historical data persistence with Room
- Game session management
- Timer and notification system
- Data visualization with custom charts

## Architecture Patterns

### 1. Unidirectional Data Flow (UDF)

This project strictly follows Google's official UDF architecture for Compose:

```kotlin
// Single source of truth
data class GameUiState(
    val bppcCounter: Counter = Counter(),
    val wlCounter: Counter = Counter(),
    val timerState: TimerState = TimerState(),
    val tableItems: List<TableItem> = emptyList(),
    // All UI state in one place
)

// Events as sealed class
sealed class GameUiEvent {
    data object OpenB : GameUiEvent()
    data object OpenP : GameUiEvent()
    data class UpdateNote(val note: String) : GameUiEvent()
    // All user actions here
}

// Side effects separate from state
sealed class GameSideEffect {
    data class ShowToast(val message: String) : GameSideEffect()
    data object NavigateToHistory : GameSideEffect()
    // One-time events here
}
```

**ViewModel Pattern:**
```kotlin
@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: TemporaryStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GameSideEffect>()
    val sideEffect: SharedFlow<GameSideEffect> = _sideEffect.asSharedFlow()

    fun onEvent(event: GameUiEvent) {
        when (event) {
            is GameUiEvent.OpenB -> handleOpenB()
            // Handle all events
        }
    }
}
```

**UI Collection Pattern:**
```kotlin
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GameSideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
```

### 2. Component Organization

UI components are organized by function in `ui/game/components/`:

```
components/
├── common/          # Reusable general components
├── input/           # User input components
├── layout/          # Layout structure components
├── strategy/        # Strategy display components
└── table/           # Table and chart components
```

**Component Pattern:**
```kotlin
// Components receive pure data, NOT ViewModels
@Composable
fun CounterDisplay(
    value1: Int,
    value2: Int,
    color1: Color,
    color2: Color,
    label1: String,
    label2: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Counter(value1, color1, label1)
        Counter(value2, color2, label2)
    }
}
```

## Commit Conventions

This project uses **conventional commits**:

- `feat:` - New features (e.g., `[feat] 九宫格策略`)
- `fix:` - Bug fixes (e.g., `fix: 九宫格策略`)
- `refactor:` - Code refactoring (e.g., `refactor: remove dead code`)
- `[refactor]` - Major refactoring efforts
- `Update` - Simple file updates
- Chinese messages accepted for domain-specific features

**Examples:**
```
[feat] 三路策略 + UI 调整
[feat] 协同滚动
fix: 九宫格策略
[refactor] 使用 Claude 重构所有
refactor: remove dead code and improve constants usage
```

## Code Organization

### Directory Structure

```
app/src/main/java/com/dsd/baccarat/
├── ui/
│   ├── game/
│   │   ├── GameScreen.kt              # Main screen entry
│   │   ├── components/                # UI components
│   │   └── state/                     # UI state & events
│   ├── theme/                         # Compose theme
│   └── compose/                       # Legacy code (to be removed)
├── viewmodel/
│   └── GameViewModel.kt               # UDF ViewModel
├── model/
│   ├── DefaultViewModel.kt            # Legacy ViewModel (deprecated)
│   └── HistoryViewModel.kt
├── data/
│   ├── DataStructure.kt               # Domain models
│   ├── TemporaryStorageRepository.kt  # Repository pattern
│   ├── room/                          # Room database
│   │   ├── AppDatabase.kt
│   │   ├── dao/                       # Data access objects
│   │   └── entity/                    # Database entities
│   └── utils/                         # Utility classes
├── MainActivity.kt
├── MyApp.kt                           # Application class
├── AppModule.kt                       # Hilt modules
└── BaseActivity.kt
```

### File Naming

- **Screens**: `*Screen.kt` (e.g., `GameScreen.kt`)
- **ViewModels**: `*ViewModel.kt` (e.g., `GameViewModel.kt`)
- **State**: `*UiState.kt`, `*UiEvent.kt`, `*SideEffect.kt`
- **Components**: `*Display.kt`, `*Section.kt`, `*Buttons.kt`
- **Entities**: `*Entity.kt`
- **DAOs**: `*DataDao.kt`, `*Dao.kt`

## Common Workflows

### Adding a New Feature

1. **Define State** in `ui/game/state/GameUiState.kt`
2. **Define Event** in `ui/game/state/GameUiEvent.kt`
3. **Implement Logic** in `viewmodel/GameViewModel.kt`
4. **Create Component** in appropriate `ui/game/components/` subdirectory
5. **Wire Up** in `ui/game/GameScreen.kt`

**Example: Adding a new button**
```kotlin
// 1. Add event
sealed class GameUiEvent {
    data object NewAction : GameUiEvent()
}

// 2. Add state if needed
data class GameUiState(
    val newField: String = ""
)

// 3. Handle in ViewModel
fun onEvent(event: GameUiEvent) {
    when (event) {
        is GameUiEvent.NewAction -> {
            // Business logic
            _uiState.update { it.copy(newField = "updated") }
        }
    }
}

// 4. Create component
@Composable
fun NewActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Text("New Action")
    }
}

// 5. Use in GameScreen
NewActionButton(
    onClick = { viewModel.onEvent(GameUiEvent.NewAction) }
)
```

### Database Changes with Room

1. **Update Entity** in `data/room/entity/`
2. **Update DAO** in `data/room/dao/` if needed
3. **Increment Database Version** in `AppDatabase.kt`
4. **Add Migration** if needed
5. **Schema auto-generated** to `app/schemas/`

```kotlin
// Entity
@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val data: String
)

// DAO
@Dao
interface GameSessionDao {
    @Query("SELECT * FROM game_sessions")
    fun getAll(): Flow<List<GameSessionEntity>>

    @Insert
    suspend fun insert(session: GameSessionEntity)
}

// Database
@Database(
    entities = [GameSessionEntity::class, /* ... */],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
}
```

### Adding Dependencies

Update `gradle/libs.versions.toml`:
```toml
[versions]
androidx-room = "2.8.3"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "androidx-room" }
```

Then use in `app/build.gradle.kts`:
```kotlin
implementation(libs.androidx.room.runtime)
```

## Testing Patterns

### Unit Tests

Location: `app/src/test/java/com/dsd/baccarat/`

```kotlin
class GridStrategyHelpersTest {
    @Test
    fun testStrategyCalculation() {
        // Given
        val input = listOf(/* test data */)

        // When
        val result = calculateStrategy(input)

        // Then
        assertEquals(expected, result)
    }
}
```

## Build & Quality Tools

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run Detekt (static analysis)
./gradlew detekt

# Clean build
./gradlew clean
```

### Detekt Configuration

Location: `config/detekt/detekt.yml`

Custom rules configured for:
- Code complexity
- Naming conventions
- Compose-specific patterns
- Coroutines best practices

## Data Flow Patterns

### Repository Pattern

```kotlin
@Singleton
class TemporaryStorageRepository @Inject constructor(
    private val inputDataDao: InputDataDao,
    private val dataStore: DataStore<Preferences>
) {
    fun getAllInputs(): Flow<List<InputEntity>> = inputDataDao.getAll()

    suspend fun saveInput(input: InputEntity) = inputDataDao.insert(input)

    suspend fun savePreference(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }
}
```

### Dependency Injection with Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "baccarat_database"
        ).build()
    }

    @Provides
    fun provideInputDao(database: AppDatabase): InputDataDao = database.inputDataDao()
}
```

## State Management Patterns

### Immutable State Updates

```kotlin
// CORRECT: Immutable update
_uiState.update { currentState ->
    currentState.copy(
        bppcCounter = currentState.bppcCounter.copy(
            value1 = newValue
        )
    )
}

// WRONG: Direct mutation
_uiState.value.bppcCounter.value1 = newValue // ❌
```

### Derived State

```kotlin
@Composable
fun ExpensiveComputation(
    items: List<Item>
) {
    val filtered by remember {
        derivedStateOf {
            items.filter { it.isActive }
        }
    }
    // Use filtered
}
```

## UI Patterns

### Synchronized Scrolling

```kotlin
// Shared scroll states for synchronized lists
val sharedScrollStates = remember { mutableStateListOf<LazyListState>() }

LaunchedEffect(sharedScrollStates) {
    snapshotFlow { sharedScrollStates.firstOrNull()?.firstVisibleItemIndex }
        .collect { index ->
            // Sync all other lists
            sharedScrollStates.forEach { state ->
                if (state != sharedScrollStates.first()) {
                    state.scrollToItem(index)
                }
            }
        }
}
```

### Timer Management

```kotlin
// Timer in ViewModel
private var timerJob: Job? = null

fun startTimer() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
        while (true) {
            delay(1000)
            _uiState.update { it.copy(
                timerState = it.timerState.copy(
                    elapsedSeconds = it.timerState.elapsedSeconds + 1
                )
            )}
        }
    }
}

fun stopTimer() {
    timerJob?.cancel()
}
```

## Code Quality Standards

### File Size Limits

- **Target**: 200-400 lines per file
- **Maximum**: 800 lines
- **UI Components**: Keep small, extract sub-components
- **ViewModels**: Extract business logic to use cases if >500 lines

### Code Organization Principles

1. **Separation of Concerns**: UI, business logic, and data layers are distinct
2. **Single Responsibility**: Each class/component has one clear purpose
3. **DRY**: Extract reusable logic to utilities/base classes
4. **Composition over Inheritance**: Prefer composition for UI components

### Constants

```kotlin
// Define constants at top of file
private const val SCROLL_POSITION_MULTIPLIER = 1000f
private const val MAX_HISTORY_ITEMS = 100

// Use object for group of related constants
object GameConstants {
    const val DEFAULT_TIMER_SECONDS = 0
    const val MAX_INPUT_LENGTH = 50
}
```

## Platform-Specific Patterns

### Android Lifecycle

```kotlin
// Collect state with lifecycle awareness
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// LaunchedEffect for one-time operations
LaunchedEffect(Unit) {
    viewModel.initialize()
}

// DisposableEffect for cleanup
DisposableEffect(Unit) {
    val listener = Listener()
    register(listener)
    onDispose {
        unregister(listener)
    }
}
```

### Context Usage

```kotlin
// Get context in Composable
val context = LocalContext.current

// Prefer dependency injection over passing context
@Composable
fun MyScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    // Don't pass context to ViewModels unless necessary
}
```

## Performance Patterns

### Lazy List Optimization

```kotlin
LazyColumn(
    state = rememberLazyListState(),
    modifier = Modifier.fillMaxSize()
) {
    items(items, key = { it.id }) { item ->
        ItemRow(item)
    }
}
```

### Recomposition Prevention

```kotlin
// Use stable types for parameters
@Composable
fun StableComponent(
    data: ImmutableData,  // Stable
    onClick: () -> Unit   // Stable
)

// Use remember for expensive calculations
val expensiveResult = remember(input) {
    calculateExpensive(input)
}
```

## Migration Notes

### Legacy Code

The following files are kept for reference but are **deprecated**:
- `ui/compose/Screen.kt` - Old 1129-line screen file
- `model/DefaultViewModel.kt` - Old ViewModel without UDF

**Do not use these patterns in new code.**

### From Legacy to UDF

**Old Pattern (Avoid):**
```kotlin
// Multiple StateFlows
private val mBppcCounterStateFlow = MutableStateFlow(Counter())
val bppcCounterStateFlow: StateFlow<Counter> = mBppcCounterStateFlow.asStateFlow()

// Component receives ViewModel
@Composable
private fun CounterDisplay(viewModel: DefaultViewModel) {
    val bppcCounter = viewModel.bppcCounterStateFlow.collectAsState()
}
```

**New Pattern (Use):**
```kotlin
// Single state source
data class GameUiState(
    val bppcCounter: Counter = Counter()
)

// Component receives pure data
@Composable
fun CounterDisplay(
    value1: Int,
    value2: Int,
    color1: Color,
    color2: Color
)
```

## Development Workflow

1. **Make changes** following UDF patterns
2. **Build**: `./gradlew assembleDebug`
3. **Test**: Run app and verify functionality
4. **Analyze**: `./gradlew detekt` (fix critical issues)
5. **Commit**: Use conventional commit format
6. **Test again**: Ensure no regressions

## Common Issues & Solutions

### Issue: State not updating
**Solution**: Ensure you're using `_uiState.update { it.copy(...) }` for immutable updates

### Issue: Compose recomposition loop
**Solution**: Use `remember` and `derivedStateOf` for expensive calculations

### Issue: Room schema mismatch
**Solution**: Increment database version and add migration or fallback to destructive migration

### Issue: Hilt dependency not found
**Solution**: Ensure `@HiltAndroidApp` is on Application class and `@AndroidEntryPoint` on Activity

## Best Practices Summary

1. ✅ **Always use UDF** for new features
2. ✅ **Keep components small** and focused
3. ✅ **Pass data, not ViewModels** to components
4. ✅ **Use sealed classes** for state and events
5. ✅ **Collect state with lifecycle awareness**
6. ✅ **Extract reusable logic** to repositories/use cases
7. ✅ **Write tests** for business logic
8. ✅ **Follow Kotlin conventions** (nullability, immutability)
9. ✅ **Use Detekt** to catch code smells
10. ✅ **Document complex logic** with KDoc comments

## Resources

- [UDF Guide](https://developer.android.com/jetpack/compose/architecture#udf)
- [Compose Best Practices](https://developer.android.com/jetpack/compose/bestpractices)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Documentation](https://developer.android.com/training/data-storage/room)
