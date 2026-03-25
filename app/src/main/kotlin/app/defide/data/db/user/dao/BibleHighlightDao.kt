package app.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.defide.data.db.user.entity.BibleHighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleHighlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: BibleHighlightEntity)

    @Query("DELETE FROM bible_highlights WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM bible_highlights WHERE verse_id = :verseId")
    fun getForVerse(verseId: Int): Flow<List<BibleHighlightEntity>>

    @Query("SELECT * FROM bible_highlights ORDER BY created_at DESC")
    fun getAll(): Flow<List<BibleHighlightEntity>>
}
