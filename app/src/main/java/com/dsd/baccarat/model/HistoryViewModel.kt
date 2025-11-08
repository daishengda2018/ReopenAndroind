package com.dsd.baccarat.model

import androidx.lifecycle.viewModelScope
import com.dsd.baccarat.data.TemporaryStorageRepository
import com.dsd.baccarat.data.room.dao.BetDataDao
import com.dsd.baccarat.data.room.dao.GameSessionDao
import com.dsd.baccarat.data.room.dao.InputDataDao
import com.dsd.baccarat.data.room.dao.NoteDataDao
import com.dsd.baccarat.data.room.entity.BetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

/**
 * Create by Shengda 2025/11/4 19:05
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    override val repository: TemporaryStorageRepository,
    override val betDataDao: BetDataDao,
    override val inputDataDao: InputDataDao,
    override val noteDataDao: NoteDataDao,
    override val gameSessionDao: GameSessionDao
) : DefaultViewModel(repository, betDataDao, inputDataDao, noteDataDao, gameSessionDao) {

    override fun setup() {
        // 什么也不执行
    }

    fun loadHistory(gameId: String, startTime: Long) {
        // 每次数据变化都会响应的 Flow
        viewModelScope.launch {
            // 收集 TemporaryStorageRepository 的冷流（wCountFlow）
            repository.wHistoryCountFlow.collect { newCount ->
                // 将新值发射到热流（_wCount）
                mWHistoryCounter.value = newCount
            }
        }

        viewModelScope.launch {
            repository.lHistoryCountFlow.collect { newCount ->
                mLHistoryCounter.value = newCount
            }
        }

        // 只响应一次的 Flow
        viewModelScope.launch {
            mInputTextStateFlow.value = noteDataDao.getNoteByGameId(gameId).joinToString { it.content }
            val allInputs = inputDataDao.getInputsByGameId(gameId)
            mOpenInputList.clear()
            mOpenInputList.addAll(allInputs)
            resumeOpenedData()

            val historyList = betDataDao.queryHistoryBefore(startTime)
                .map { it.copy().apply { this.isHistory = true } }
            val curbetList = betDataDao.loadDataWithGameId(gameId)
            recordBetDataOnStartup(historyList, curbetList)
        }
    }

    override fun isBetHistory(last3Inputs: List<BetEntity>): Boolean {
        return last3Inputs.last().isHistory
    }

}