package com.tristinbaker.defide.data.db.userbible

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toTranslation
import com.tristinbaker.defide.data.model.Translation
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val DB_NAME = "defide_user_bibles.db"
private const val DB_VERSION = 1

/**
 * Writable SQLite database holding user-imported Bible translations.
 *
 * Mirrors the bible schema of the read-only content database so [com.tristinbaker.defide.data.db.content.dao.BibleDao]
 * can route queries to either one. Lives in its own file because the content
 * database is recopied from assets on every CONTENT_VERSION bump, which would
 * wipe anything inserted into it. Book and verse IDs start at reserved bases so
 * they never collide with content rows (highlights reference verse IDs globally).
 */
@Singleton
class UserBibleDatabase internal constructor(
    context: Context,
    dbName: String,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, DB_NAME)

    private val helper = object : SQLiteOpenHelper(context, dbName, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE translations (
                    id       TEXT PRIMARY KEY,
                    name     TEXT NOT NULL,
                    language TEXT NOT NULL,
                    license  TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE books (
                    id              INTEGER PRIMARY KEY,
                    translation_id  TEXT NOT NULL,
                    book_number     INTEGER NOT NULL,
                    testament       TEXT NOT NULL,
                    short_name      TEXT NOT NULL,
                    full_name       TEXT NOT NULL,
                    dr_name         TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE verses (
                    id      INTEGER PRIMARY KEY,
                    book_id INTEGER NOT NULL,
                    chapter INTEGER NOT NULL,
                    verse   INTEGER NOT NULL,
                    text    TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_user_books_translation ON books(translation_id, book_number)")
            db.execSQL("CREATE INDEX idx_user_verses_book ON verses(book_id, chapter, verse)")
            db.execSQL("CREATE VIRTUAL TABLE verses_fts USING fts4(text)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    val db: SQLiteDatabase get() = helper.writableDatabase

    fun getTranslations(): List<Translation> =
        db.rawQuery("SELECT * FROM translations ORDER BY name", null)
            .mapRows { toTranslation() }

    fun deleteTranslation(translationId: String) {
        val database = db
        database.beginTransaction()
        try {
            deleteTranslationRows(database, translationId)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    companion object {
        /** Translation IDs of imported bibles carry this prefix; BibleDao routes on it. */
        const val TRANSLATION_ID_PREFIX = "user_"

        /** Book IDs at or above this value belong to the user bible database. */
        const val BOOK_ID_BASE = 1_000_000

        /** Verse IDs at or above this value belong to the user bible database. */
        const val VERSE_ID_BASE = 10_000_000

        /** Must run inside a transaction on [db]. */
        internal fun deleteTranslationRows(db: SQLiteDatabase, translationId: String) {
            db.execSQL(
                """
                DELETE FROM verses_fts WHERE docid IN (
                    SELECT v.id FROM verses v JOIN books b ON v.book_id = b.id
                    WHERE b.translation_id = ?
                )
                """.trimIndent(),
                arrayOf(translationId),
            )
            db.execSQL(
                "DELETE FROM verses WHERE book_id IN (SELECT id FROM books WHERE translation_id = ?)",
                arrayOf(translationId),
            )
            db.execSQL("DELETE FROM books WHERE translation_id = ?", arrayOf(translationId))
            db.execSQL("DELETE FROM translations WHERE id = ?", arrayOf(translationId))
        }
    }
}
