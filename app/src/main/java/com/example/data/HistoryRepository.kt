package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    suspend fun insert(item: HistoryItem) {
        historyDao.insertHistory(item)
    }

    suspend fun delete(id: Long) {
        historyDao.deleteHistory(id)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
