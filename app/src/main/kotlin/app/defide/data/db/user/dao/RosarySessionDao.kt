package app.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.defide.data.db.user.entity.RosarySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RosarySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RosarySessionEntity)

    @Query("UPDATE rosary_sessions SET completed = 1, completed_at = :completedAt WHERE id = :id")
    suspend fun markComplete(id: String, completedAt: Long)

    @Query("SELECT * FROM rosary_sessions ORDER BY started_at DESC")
    fun getAll(): Flow<List<RosarySessionEntity>>

    @Query("SELECT * FROM rosary_sessions ORDER BY started_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<RosarySessionEntity>>
}
