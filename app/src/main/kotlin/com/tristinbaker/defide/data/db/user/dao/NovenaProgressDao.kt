package com.tristinbaker.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tristinbaker.defide.data.db.user.entity.NovenaProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovenaProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: NovenaProgressEntity)

    @Update
    suspend fun update(progress: NovenaProgressEntity)

    @Query("SELECT * FROM novena_progress WHERE novena_id = :novenaId ORDER BY start_date DESC LIMIT 1")
    suspend fun getByNovenaId(novenaId: String): NovenaProgressEntity?

    @Query("SELECT * FROM novena_progress WHERE completed = 0 ORDER BY start_date DESC")
    fun getActive(): Flow<List<NovenaProgressEntity>>

    @Query("SELECT * FROM novena_progress WHERE completed = 1 ORDER BY start_date DESC")
    fun getCompleted(): Flow<List<NovenaProgressEntity>>

    @Query("UPDATE novena_progress SET last_completed_day = :day WHERE id = :id")
    suspend fun advanceDay(id: String, day: Int)

    @Query("UPDATE novena_progress SET completed = 1 WHERE id = :id")
    suspend fun markComplete(id: String)

    @Query("DELETE FROM novena_progress WHERE id = :id")
    suspend fun delete(id: String)
}
