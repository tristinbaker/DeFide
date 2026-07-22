package com.tristinbaker.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tristinbaker.defide.data.db.user.entity.SinHabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SinHabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: SinHabitEntity)

    @Query("SELECT * FROM sin_habits ORDER BY created_at ASC")
    fun getAll(): Flow<List<SinHabitEntity>>

    @Query("UPDATE sin_habits SET last_relapse_at = :relapseAt WHERE id = :id")
    suspend fun logRelapse(id: String, relapseAt: Long)

    @Query("DELETE FROM sin_habits WHERE id = :id")
    suspend fun delete(id: String)
}
