package com.tristinbaker.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tristinbaker.defide.data.db.user.entity.BibleBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BibleBookmarkEntity)

    @Query("DELETE FROM bible_bookmarks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM bible_bookmarks ORDER BY created_at DESC")
    fun getAll(): Flow<List<BibleBookmarkEntity>>

    @Query("SELECT * FROM bible_bookmarks WHERE translation_id = :translationId AND book_number = :bookNumber ORDER BY chapter, verse")
    fun getForBook(translationId: String, bookNumber: Int): Flow<List<BibleBookmarkEntity>>
}
