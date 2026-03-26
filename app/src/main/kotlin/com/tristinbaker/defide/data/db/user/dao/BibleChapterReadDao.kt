package com.tristinbaker.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tristinbaker.defide.data.db.user.entity.BibleChapterReadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleChapterReadDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markRead(entity: BibleChapterReadEntity)

    @Query("DELETE FROM bible_chapter_read WHERE translation_id = :translationId AND book_number = :bookNumber AND chapter = :chapter")
    suspend fun unmarkRead(translationId: String, bookNumber: Int, chapter: Int)

    @Query("SELECT chapter FROM bible_chapter_read WHERE translation_id = :translationId AND book_number = :bookNumber")
    fun getReadChapters(translationId: String, bookNumber: Int): Flow<List<Int>>

    @Query("DELETE FROM bible_chapter_read WHERE translation_id = :translationId AND book_number = :bookNumber")
    suspend fun resetBook(translationId: String, bookNumber: Int)

    @Query("SELECT * FROM bible_chapter_read ORDER BY read_at DESC")
    fun getAll(): Flow<List<BibleChapterReadEntity>>
}
