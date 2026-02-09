# Dead Code Analysis Report

**Generated:** 2026-02-09
**Project:** ReopenAndroid (Baccarat Game)
**Analysis Scope:** All Kotlin source files

---

## Executive Summary

This report identifies dead code (unused classes, functions, constants, and imports) across the Android codebase. The analysis found **5 items** that can be safely removed.

---

## Findings by Category

### ✅ SAFE - Unused Code (Recommended for Removal)

#### 1. Unused Class: `DisplayItem` (DataStructure.kt:73-75)

**Location:** `app/src/main/java/com/dsd/baccarat/data/DataStructure.kt:73-75`

**Code:**
```kotlin
// UI 显示项的基类
@Immutable
sealed class DisplayItem {
    @Immutable object Empty : DisplayItem()
}
```

**Usage Analysis:**
- Defined but never extended or used anywhere in the codebase
- Only the definition itself exists
- No other classes inherit from `DisplayItem`
- The specialized classes `TableDisplayItem` and `Strategy3WyasDisplayItem` are separate and used

**Impact:** None - safe to remove

**Recommendation:** DELETE

---

#### 2. Unused Constant: `DEFAULT_STRATEGY_3WAYS` (GameViewModel.kt:1014)

**Location:** `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt:1014`

**Code:**
```kotlin
private val DEFAULT_STRATEGY_3WAYS = Strategy3WaysData()
```

**Usage Analysis:**
- Defined but never referenced
- Similar constant `DEFAULT_STRATEGY_3WAY` (line 1010) is used instead
- Appears to be a duplicate/typo

**Impact:** None - safe to remove

**Recommendation:** DELETE

---

#### 3. Unused Constant: `DEFAULT_PREDICTED_3WAYS` (GameViewModel.kt:1015)

**Location:** `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt:1015`

**Code:**
```kotlin
private val DEFAULT_PREDICTED_3WAYS = PredictedStrategy3WaysValue()
```

**Usage Analysis:**
- Defined but never referenced
- Similar constant `DEFAULT_PREDICTION` (line 1009) is used instead
- Appears to be a duplicate/typo

**Impact:** None - safe to remove

**Recommendation:** DELETE

---

#### 4. Unused Import: `Intent` (HistoryActivity.kt:4)

**Location:** `app/src/main/java/com/dsd/baccarat/HistoryActivity.kt:4`

**Code:**
```kotlin
import android.content.Intent
```

**Usage Analysis:**
- Intent is used in `RightSideSection.kt` via the `HandleSideEffects` function
- Not used directly in `HistoryActivity.kt`
- `HistoryActivity.kt` only reads intent extras but doesn't create new intents

**Impact:** Minor - cleaner imports

**Recommendation:** DELETE

---

#### 5. Unused Comment Reference (GameViewModel.kt:483)

**Location:** `app/src/main/java/com/dsd/baccarat/viewmodel/GameViewModel.kt:483`

**Code:**
```kotlin
// ==================== 业务逻辑（从 DefaultViewModel 迁移） ====================
```

**Usage Analysis:**
- Comment references `DefaultViewModel` which has been deleted
- The migration is complete
- Comment is now obsolete

**Impact:** Documentation cleanup

**Recommendation:** UPDATE or DELETE - change to "业务逻辑" or remove section comment

---

## Previously Cleaned (Confirmed Safe)

### ✅ Deleted Files (No References Found)

1. **DateUtils.kt** - Deleted, no references found in codebase
2. **DefaultViewModel.kt** - Deleted, functionality migrated to GameViewModel

---

## Statistics

| Category | Count | Lines |
|----------|-------|-------|
| Unused Classes | 1 | 3 |
| Unused Constants | 2 | 2 |
| Unused Imports | 1 | 1 |
| Obsolete Comments | 1 | 1 |
| **Total** | **5** | **7** |

---

## Severity Classification

### 🟢 SAFE (5 items)
- All findings are safe to remove
- No impact on functionality
- Clean up improves code maintainability

### 🟡 CAUTION (0 items)
- None identified

### 🔴 DANGER (0 items)
- None identified

---

## Recommended Actions

### Priority 1: Safe to Remove (All items)

1. **Remove `DisplayItem` class** from DataStructure.kt
2. **Remove `DEFAULT_STRATEGY_3WAYS` constant** from GameViewModel.kt
3. **Remove `DEFAULT_PREDICTED_3WAYS` constant** from GameViewModel.kt
4. **Remove `Intent` import** from HistoryActivity.kt
5. **Update section comment** in GameViewModel.kt line 483

---

## Testing Strategy

Before applying deletions:
1. ✅ Run existing tests (if any)
2. ✅ Build project successfully
3. ✅ Verify app functionality

After applying deletions:
1. ✅ Rebuild project
2. ✅ Run tests to ensure no regressions
3. ✅ Manual smoke test of core features

---

## Notes

- The analysis was performed by searching for usage patterns across all Kotlin files
- No automated dead code detection tools (like Detekt) were configured
- Manual verification confirmed no usages exist for identified items
- The project has good separation of concerns with minimal dead code accumulation

---

## Conclusion

The codebase is relatively clean with only **5 dead code items** identified. All items are safe to remove with no impact on functionality. The codebase shows good maintenance practices with recent cleanup of unused files (DateUtils.kt, DefaultViewModel.kt).

**Estimated cleanup impact:** ~7 lines of code removed, improved code clarity.
