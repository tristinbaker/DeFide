package app.defide.data.repository

import app.defide.data.db.content.dao.BibleDao
import app.defide.data.db.user.dao.BibleBookmarkDao
import app.defide.data.db.user.dao.BibleHighlightDao
import app.defide.data.db.user.entity.BibleBookmarkEntity
import app.defide.data.db.user.entity.BibleHighlightEntity
import app.defide.data.model.Book
import app.defide.data.model.Translation
import app.defide.data.model.Verse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepository @Inject constructor(
    private val bibleDao: BibleDao,
    private val bookmarkDao: BibleBookmarkDao,
    private val highlightDao: BibleHighlightDao,
) {
    suspend fun getTranslations(): List<Translation> =
        withContext(Dispatchers.IO) { bibleDao.getTranslations() }

    suspend fun getBooks(translationId: String): List<Book> =
        withContext(Dispatchers.IO) { bibleDao.getBooks(translationId) }

    suspend fun getBook(translationId: String, bookNumber: Int): Book? =
        withContext(Dispatchers.IO) { bibleDao.getBook(translationId, bookNumber) }

    suspend fun getChapterCount(bookId: Int): Int =
        withContext(Dispatchers.IO) { bibleDao.getChapterCount(bookId) }

    suspend fun getVerses(bookId: Int, chapter: Int): List<Verse> =
        withContext(Dispatchers.IO) { bibleDao.getVerses(bookId, chapter) }

    suspend fun getVerseOfDay(translationId: String): Pair<Verse, Book>? =
        withContext(Dispatchers.IO) {
            val epochDay = LocalDate.now().toEpochDay()
            val verse = bibleDao.getVerseOfDay(translationId, epochDay) ?: return@withContext null
            val book = bibleDao.getBookById(verse.bookId) ?: return@withContext null
            Pair(verse, book)
        }

    suspend fun search(translationId: String, query: String): List<Verse> =
        withContext(Dispatchers.IO) { bibleDao.searchVerses(translationId, query) }

    // Bookmarks
    fun getBookmarks(): Flow<List<BibleBookmarkEntity>> = bookmarkDao.getAll()

    fun getBookmarksForBook(
        translationId: String,
        bookNumber: Int,
    ): Flow<List<BibleBookmarkEntity>> = bookmarkDao.getForBook(translationId, bookNumber)

    suspend fun addBookmark(
        translationId: String,
        bookNumber: Int,
        chapter: Int,
        verse: Int,
        note: String? = null,
    ) = withContext(Dispatchers.IO) {
        bookmarkDao.insert(
            BibleBookmarkEntity(
                id = UUID.randomUUID().toString(),
                translationId = translationId,
                bookNumber = bookNumber,
                chapter = chapter,
                verse = verse,
                note = note,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteBookmark(id: String) =
        withContext(Dispatchers.IO) { bookmarkDao.delete(id) }

    // Highlights
    fun getHighlightsForVerse(verseId: Int): Flow<List<BibleHighlightEntity>> =
        highlightDao.getForVerse(verseId)

    suspend fun addHighlight(verseId: Int, color: String = "yellow") =
        withContext(Dispatchers.IO) {
            highlightDao.insert(
                BibleHighlightEntity(
                    id = UUID.randomUUID().toString(),
                    verseId = verseId,
                    color = color,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }

    suspend fun deleteHighlight(id: String) =
        withContext(Dispatchers.IO) { highlightDao.delete(id) }
}
