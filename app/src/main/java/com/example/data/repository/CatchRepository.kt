package com.example.data.repository

import com.example.data.database.CatchDao
import com.example.data.database.CatchEntity
import kotlinx.coroutines.flow.Flow

class CatchRepository(private val catchDao: CatchDao) {
    val allCatches: Flow<List<CatchEntity>> = catchDao.getAllCatches()

    suspend fun getUnsyncedCatches(): List<CatchEntity> = catchDao.getUnsyncedCatches()

    suspend fun insertCatch(catch: CatchEntity) {
        catchDao.insertCatch(catch)
    }

    suspend fun updateCatch(catch: CatchEntity) {
        catchDao.updateCatch(catch)
    }

    suspend fun deleteCatch(catch: CatchEntity) {
        catchDao.deleteCatch(catch)
    }

    suspend fun deleteCatchById(id: Int) {
        catchDao.deleteCatchById(id)
    }
}
