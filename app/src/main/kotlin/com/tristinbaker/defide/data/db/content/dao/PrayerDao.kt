package com.tristinbaker.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import com.tristinbaker.defide.data.db.content.firstOrNull
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toPrayer
import com.tristinbaker.defide.data.model.Prayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getAll(language: String = "en"): List<Prayer> =
        db.rawQuery("SELECT * FROM prayers WHERE language = ? ORDER BY title", arrayOf(language))
            .mapRows { toPrayer() }.withTags(language)

    fun getCategories(language: String = "en"): List<String> =
        db.rawQuery(
            "SELECT DISTINCT category FROM prayers WHERE language = ? ORDER BY category",
            arrayOf(language),
        ).mapRows { getString(0) }

    fun getTags(language: String = "en"): List<String> =
        db.rawQuery(
            "SELECT DISTINCT tag FROM prayer_tags WHERE language = ? ORDER BY tag",
            arrayOf(language),
        ).mapRows { getString(0) }

    fun getByCategory(category: String, language: String = "en"): List<Prayer> =
        db.rawQuery(
            "SELECT * FROM prayers WHERE category = ? AND language = ? ORDER BY title",
            arrayOf(category, language),
        ).mapRows { toPrayer() }.withTags(language)

    fun getByTag(tag: String, language: String = "en"): List<Prayer> =
        db.rawQuery(
            """
            SELECT p.* FROM prayers p
            JOIN prayer_tags pt ON p.id = pt.prayer_id AND p.language = pt.language
            WHERE pt.tag = ? AND p.language = ?
            ORDER BY p.title
            """.trimIndent(),
            arrayOf(tag, language),
        ).mapRows { toPrayer() }.withTags(language)

    fun getById(id: String, language: String = "en"): Prayer? =
        db.rawQuery(
            "SELECT * FROM prayers WHERE id = ? AND language = ?",
            arrayOf(id, language),
        ).firstOrNull { toPrayer() }?.withTags(language)

    /** FTS full-text search. */
    fun search(query: String, language: String = "en"): List<Prayer> {
        val safeQuery = "\"${query.replace("\"", " ")}\""
        return try {
            db.rawQuery(
                """
                SELECT * FROM prayers
                WHERE language = ? AND rowid IN (SELECT docid FROM prayers_fts WHERE prayers_fts MATCH ?)
                ORDER BY title
                LIMIT 50
                """.trimIndent(),
                arrayOf(language, safeQuery),
            ).mapRows { toPrayer() }.withTags(language)
        } catch (e: android.database.SQLException) {
            emptyList()
        }
    }

    private fun tagsForPrayer(prayerId: String, language: String): List<String> =
        db.rawQuery(
            "SELECT tag FROM prayer_tags WHERE prayer_id = ? AND language = ? ORDER BY tag",
            arrayOf(prayerId, language),
        ).mapRows { getString(0) }

    private fun Prayer.withTags(language: String) = copy(tags = tagsForPrayer(id, language))
    private fun List<Prayer>.withTags(language: String) = map { it.withTags(language) }
}
