package com.tristinbaker.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import com.tristinbaker.defide.data.db.content.firstOrNull
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toBook
import com.tristinbaker.defide.data.db.content.toTranslation
import com.tristinbaker.defide.data.db.content.toVerse
import com.tristinbaker.defide.data.db.userbible.UserBibleDatabase
import com.tristinbaker.defide.data.model.Book
import com.tristinbaker.defide.data.model.Translation
import com.tristinbaker.defide.data.model.Verse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads bible data from the read-only content database and, for translations
 * imported by the user, from [UserBibleDatabase]. Routing is by translation ID
 * prefix or by the reserved ID ranges of user-imported rows.
 */
@Singleton
class BibleDao @Inject constructor(
    private val contentDb: SQLiteDatabase,
    private val userBibles: UserBibleDatabase,
) {

    private fun dbFor(translationId: String): SQLiteDatabase =
        if (translationId.startsWith(UserBibleDatabase.TRANSLATION_ID_PREFIX)) userBibles.db else contentDb

    private fun dbForBook(bookId: Int): SQLiteDatabase =
        if (bookId >= UserBibleDatabase.BOOK_ID_BASE) userBibles.db else contentDb

    fun getTranslations(): List<Translation> {
        val builtIn = contentDb.rawQuery("SELECT * FROM translations ORDER BY name", null)
            .mapRows { toTranslation() }
        return (builtIn + userBibles.getTranslations()).sortedBy { it.name }
    }

    fun getBooks(translationId: String): List<Book> =
        dbFor(translationId).rawQuery(
            "SELECT * FROM books WHERE translation_id = ? ORDER BY book_number",
            arrayOf(translationId),
        ).mapRows { toBook() }

    fun getBook(translationId: String, bookNumber: Int): Book? =
        dbFor(translationId).rawQuery(
            "SELECT * FROM books WHERE translation_id = ? AND book_number = ?",
            arrayOf(translationId, bookNumber.toString()),
        ).firstOrNull { toBook() }

    fun getChapterCount(bookId: Int): Int =
        dbForBook(bookId).rawQuery(
            "SELECT MAX(chapter) FROM verses WHERE book_id = ?",
            arrayOf(bookId.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun getVerses(bookId: Int, chapter: Int): List<Verse> =
        dbForBook(bookId).rawQuery(
            "SELECT * FROM verses WHERE book_id = ? AND chapter = ? ORDER BY verse",
            arrayOf(bookId.toString(), chapter.toString()),
        ).mapRows { toVerse() }

    fun getVerse(bookId: Int, chapter: Int, verse: Int): Verse? =
        dbForBook(bookId).rawQuery(
            "SELECT * FROM verses WHERE book_id = ? AND chapter = ? AND verse = ?",
            arrayOf(bookId.toString(), chapter.toString(), verse.toString()),
        ).firstOrNull { toVerse() }

    fun getVerseOfDay(translationId: String, epochDay: Long): Verse? {
        val db = dbFor(translationId)
        val bookCount = db.rawQuery(
            "SELECT COUNT(*) FROM books WHERE translation_id = ?",
            arrayOf(translationId),
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        if (bookCount == 0L) return null
        val bookOffset = epochDay % bookCount
        val bookId = db.rawQuery(
            "SELECT id FROM books WHERE translation_id = ? ORDER BY book_number LIMIT 1 OFFSET ?",
            arrayOf(translationId, bookOffset.toString()),
        ).use { if (it.moveToFirst()) it.getLong(0) else return null }
        val verseCount = db.rawQuery(
            "SELECT COUNT(*) FROM verses WHERE book_id = ?",
            arrayOf(bookId.toString()),
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        if (verseCount == 0L) return null
        val verseOffset = (epochDay / bookCount) % verseCount
        return db.rawQuery(
            "SELECT * FROM verses WHERE book_id = ? ORDER BY chapter, verse LIMIT 1 OFFSET ?",
            arrayOf(bookId.toString(), verseOffset.toString()),
        ).firstOrNull { toVerse() }
    }

    fun getBookById(bookId: Int, translationId: String? = null): Book? {
        val args = if (translationId != null) {
            arrayOf(bookId.toString(), translationId)
        } else {
            arrayOf(bookId.toString())
        }
        val sql = if (translationId != null) {
            "SELECT * FROM books WHERE id = ? AND translation_id = ?"
        } else {
            "SELECT * FROM books WHERE id = ?"
        }
        return dbForBook(bookId).rawQuery(sql, args).firstOrNull { toBook() }
    }

    /** FTS full-text search across all verses for a given translation. */
    fun searchVerses(translationId: String, query: String): List<Verse> {
        val safeQuery = "\"${query.replace("\"", " ")}\""
        return try { searchVersesInternal(translationId, safeQuery) } catch (e: android.database.SQLException) { emptyList() }
    }

    private fun searchVersesInternal(translationId: String, query: String): List<Verse> =
        dbFor(translationId).rawQuery(
            """
            SELECT v.* FROM verses v
            JOIN books b ON v.book_id = b.id
            WHERE b.translation_id = ?
              AND v.id IN (SELECT docid FROM verses_fts WHERE verses_fts MATCH ?)
            ORDER BY b.book_number, v.chapter, v.verse
            LIMIT 200
            """.trimIndent(),
            arrayOf(translationId, query),
        ).mapRows { toVerse() }

}
