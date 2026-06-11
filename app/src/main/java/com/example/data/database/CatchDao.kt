package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatchDao {
    @Query("SELECT * FROM catches ORDER BY timestamp DESC")
    fun getAllCatches(): Flow<List<CatchEntity>>

    @Query("SELECT * FROM catches WHERE isSynced = 0")
    suspend fun getUnsyncedCatches(): List<CatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatch(catch: CatchEntity)

    @Update
    suspend fun updateCatch(catch: CatchEntity)

    @Delete
    suspend fun deleteCatch(catch: CatchEntity)

    @Query("DELETE FROM catches WHERE id = :id")
    suspend fun deleteCatchById(id: Int)
}
