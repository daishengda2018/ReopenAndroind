package com.dsd.baccarat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class CountRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // 1. 获取W类型数量的Flow（供UI观察）
    val wHistoryCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[W_COUNT_HISTORY] ?: 0 // 默认为0
        }

    // 2. 获取L类型数量的Flow（供UI观察）
    val lHistoryCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[L_COUNT_HISTORY] ?: 0 // 默认为0
        }

    // 3. 更新数量（核心：根据类型和操作，自动累加/累减）
    suspend fun updateWlCount(
        type: BetResultType, // W或L
        operation: OperationType // 加或减
    ) = withContext(Dispatchers.IO) {
        val historyKey = when (type) {
            BetResultType.W -> W_COUNT_HISTORY
            BetResultType.L -> L_COUNT_HISTORY
        }
        val currentKey = when (type) {
            BetResultType.W -> W_COUNT_CUR
            BetResultType.L -> L_COUNT_CUR
        }

        context.dataStore.edit { preferences ->
            val currentCount = preferences[historyKey] ?: 0
            val newValue = when (operation) {
                OperationType.INCREMENT -> currentCount + 1
                OperationType.DECREMENT -> (currentCount - 1).coerceAtLeast(0)
            }
            preferences[historyKey] = newValue
        }

        context.dataStore.edit { preferences ->
            val currentCount = preferences[currentKey] ?: 0
            val newValue = when (operation) {
                OperationType.INCREMENT -> currentCount + 1
                OperationType.DECREMENT -> (currentCount - 1).coerceAtLeast(0)
            }
            preferences[currentKey] = newValue
        }
    }

    suspend fun clearCurWinLossCount() {
        context.dataStore.edit { preferences ->
            preferences[W_COUNT_CUR] = 0
            preferences[L_COUNT_CUR] = 0
        }
    }

    suspend fun getWinLossCurCount(): Counter {
        try {
            val preferences = context.dataStore.data.first()
            val w = preferences[W_COUNT_CUR] ?: 0
            val l = preferences[L_COUNT_CUR] ?: 0
            return Counter(w, l)
        } catch (e: IOException) {
            e.printStackTrace()
            return Counter()
        }
    }
    suspend fun saveNoteText(text: String) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences -> preferences[NOTE_TEXT] = text }
    }

    suspend fun getNoteText(): String {
        try {
            val preferences = context.dataStore.data.first()
            return preferences[NOTE_TEXT] ?: ""
        } catch (e: IOException) {
            // 处理读取文件时可能发生的 IO 异常
            e.printStackTrace()
            return ""
        }
    }

    // 保存 List<InputType>
    suspend fun saveOpendList(list: List<InputType>) {
        val json = SerializationUtils.serializeInputTypeList(list)
        context.dataStore.edit { preferences ->
            preferences[INPUT_TYPE_LIST] = json
        }
    }

    suspend fun getOpendList(): List<InputType> {
        try {
            // .data 是 Flow<Preferences>
            // .first() 挂起当前协程，直到获得第一个 Preferences 对象
            val preferences = context.dataStore.data.first()
            val json = preferences[INPUT_TYPE_LIST] ?: return emptyList()
            return SerializationUtils.deserializeInputTypeList(json)
        } catch (e: IOException) {
            // 处理读取文件时可能发生的 IO 异常
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun saveBetList(list: List<BetResultType>) {
        val result = if (list.size > 63) {
            list.toMutableList().apply { removeFirstOrNull() }
        } else {
            list
        }
        val json = SerializationUtils.serializeBeltResultTypeList(result)
        context.dataStore.edit { preferences ->
            preferences[BET_TYPE_LIST] = json
        }
    }

    suspend fun getBetList(): List<BetResultType> {
        try {
            // .data 是 Flow<Preferences>
            // .first() 挂起当前协程，直到获得第一个 Preferences 对象
            val preferences = context.dataStore.data.first()
            val json = preferences[BET_TYPE_LIST] ?: return emptyList()
            return SerializationUtils.deserializeBeltResultTypeList(json)
        } catch (e: IOException) {
            // 处理读取文件时可能发生的 IO 异常
            e.printStackTrace()
            return emptyList()
        }
    }

    companion object {
        private val W_COUNT_HISTORY = intPreferencesKey("w_count_history")
        private val L_COUNT_HISTORY = intPreferencesKey("l_count_history")

        private val W_COUNT_CUR = intPreferencesKey("w_count_cur")
        private val L_COUNT_CUR = intPreferencesKey("l_count_cur")

        private val NOTE_TEXT = stringPreferencesKey("note_text")

        private val INPUT_TYPE_LIST = stringPreferencesKey("opend_list")
        private val BET_TYPE_LIST = stringPreferencesKey("bet_list")
        private const val WLR_HISTORY_PREFERENCES = "wlr_history_preferences"
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = WLR_HISTORY_PREFERENCES)
    }
}