package io.github.galitach.mathhero.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemResultDao {
    @Insert
    suspend fun insert(result: ProblemResultEntity)

    @Query("SELECT * FROM progress_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ProblemResultEntity>>

    @Query("SELECT * FROM progress_history ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<ProblemResultEntity>

    @Query("DELETE FROM progress_history WHERE id IN (SELECT id FROM progress_history ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("SELECT COUNT(*) FROM progress_history")
    suspend fun getCount(): Int
}