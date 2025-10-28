package com.dsd.baccarat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.dsd.baccarat.data.CountKeys.NOTE_TEXT
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CountRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // 1. 获取W类型数量的Flow（供UI观察）
    val wCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[CountKeys.W_COUNT] ?: 0 // 默认为0
        }

    // 2. 获取L类型数量的Flow（供UI观察）
    val lCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[CountKeys.L_COUNT] ?: 0 // 默认为0
        }


    val noteTextFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CountKeys.NOTE_TEXT] ?: ""
        }


    val opendList: Flow<List<InputType>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[CountKeys.INPUT_TYPE_LIST] ?: return@map emptyList()
            SerializationUtils.deserializeInputTypeList(json)
        }

    val betList: Flow<List<BeltResultType>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[CountKeys.BET_TYPE_LIST] ?: return@map emptyList()
            SerializationUtils.deserializeBeltResultTypeList(json)
        }


    // 3. 更新数量（核心：根据类型和操作，自动累加/累减）
    suspend fun updateCount(
        type: BeltResultType, // W或L
        operation: OperationType // 加或减
    ) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            // 先获取当前值（默认0）
            val currentCount = when (type) {
                BeltResultType.W -> preferences[CountKeys.W_COUNT] ?: 0
                BeltResultType.L -> preferences[CountKeys.L_COUNT] ?: 0
            }
            // 根据操作类型计算新值（累减时确保不小于0，可选）
            val newValue = when (operation) {
                OperationType.INCREMENT -> currentCount + 1
                OperationType.DECREMENT -> (currentCount - 1).coerceAtLeast(0) // 避免负数
            }
            // 写入新值
            when (type) {
                BeltResultType.W -> preferences[CountKeys.W_COUNT] = newValue
                BeltResultType.L -> preferences[CountKeys.L_COUNT] = newValue
            }
        }
    }

    suspend fun saveNoteText(text: String) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences -> preferences[NOTE_TEXT] = text }
    }

    // 保存 List<InputType>
    suspend fun saveOpendList(list: List<InputType>) {
        val json = SerializationUtils.serializeInputTypeList(list)
        context.dataStore.edit { preferences ->
            preferences[CountKeys.INPUT_TYPE_LIST] = json
        }
    }

    suspend fun saveBetList(list: List<BeltResultType>) {
        val json = SerializationUtils.serializeBeltResultTypeList(list)
        context.dataStore.edit { preferences ->
            preferences[CountKeys.BET_TYPE_LIST] = json
        }
    }
    companion object {
        private const val WLR_HISTORY_PREFERENCES = "wlr_history_preferences"
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = WLR_HISTORY_PREFERENCES)
    }
}