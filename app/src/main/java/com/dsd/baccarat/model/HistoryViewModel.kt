package com.dsd.baccarat.model

import androidx.lifecycle.viewModelScope
import com.dsd.baccarat.data.TemporaryStorageRepository
import com.dsd.baccarat.data.room.dao.BetDataDao
import com.dsd.baccarat.data.room.dao.GameSessionDao
import com.dsd.baccarat.data.room.dao.InputDataDao
import com.dsd.baccarat.data.room.dao.NoteDataDao
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

/**
 * Create by Shengda 2025/11/4 19:05
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    override val temporaryStorageRepository: TemporaryStorageRepository,
    override val betDataDao: BetDataDao,
    override val inputDataDao: InputDataDao,
    override val noteDataDao: NoteDataDao,
    override val gameSessionDao: GameSessionDao
) : DefaultViewModel(temporaryStorageRepository, betDataDao, inputDataDao, noteDataDao, gameSessionDao) {

    override fun setup() {
        // 什么也不执行
    }

    fun loadHistory(gameId: String) {
        // 只响应一次的 Flow
        viewModelScope.launch {
            mInputTextStateFlow.value = noteDataDao.getNoteByGameId(gameId).joinToString { it.content }
            val list = inputDataDao.getInputsByGameId(gameId)
            mOpenInputList.clear()
            mOpenInputList.addAll(list)
            resumeOpenedData()
        }

        viewModelScope.launch {
            val allBets = betDataDao.getBetDataByGameId(gameId)
            mBetResultList.clear()
            mBetResultList.addAll(allBets)
            resumeBetedData()
        }
    }
}