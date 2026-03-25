package app.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import app.defide.data.db.content.firstOrNull
import app.defide.data.db.content.mapRows
import app.defide.data.db.content.toPrayer
import app.defide.data.model.Prayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getAll(): List<Prayer> =
        db.rawQuery("SELECT * FROM prayers ORDER BY title", null)
            .mapRows { toPrayer() }.withTags()

    fun getCategories(): List<String> =
        db.rawQuery("SELECT DISTINCT category FROM prayers ORDER BY category", null)
            .mapRows { getString(0) }

    fun getTags(): List<String> =
        db.rawQuery("SELECT DISTINCT tag FROM prayer_tags ORDER BY tag", null)
            .mapRows { getString(0) }

    fun getByCategory(category: String): List<Prayer> =
        db.rawQuery(
            "SELECT * FROM prayers WHERE category = ? ORDER BY title",
            arrayOf(category),
        ).mapRows { toPrayer() }.withTags()

    fun getByTag(tag: String): List<Prayer> =
        db.rawQuery(
            """
            SELECT p.* FROM prayers p
            JOIN prayer_tags pt ON p.id = pt.prayer_id
            WHERE pt.tag = ?
            ORDER BY p.title
            """.trimIndent(),
            arrayOf(tag),
        ).mapRows { toPrayer() }.withTags()

    fun getById(id: String): Prayer? =
        db.rawQuery("SELECT * FROM prayers WHERE id = ?", arrayOf(id))
            .firstOrNull { toPrayer() }?.withTags()

    /** FTS5 full-text search. */
    fun search(query: String): List<Prayer> {
        val safeQuery = "\"${query.replace("\"", " ")}\""
        return try {
            db.rawQuery(
                """
                SELECT * FROM prayers
                WHERE rowid IN (SELECT docid FROM prayers_fts WHERE prayers_fts MATCH ?)
                ORDER BY title
                LIMIT 50
                """.trimIndent(),
                arrayOf(safeQuery),
            ).mapRows { toPrayer() }.withTags()
        } catch (e: android.database.SQLException) {
            emptyList()
        }
    }

    private fun tagsForPrayer(prayerId: String): List<String> =
        db.rawQuery(
            "SELECT tag FROM prayer_tags WHERE prayer_id = ? ORDER BY tag",
            arrayOf(prayerId),
        ).mapRows { getString(0) }

    private fun Prayer.withTags() = copy(tags = tagsForPrayer(id))
    private fun List<Prayer>.withTags() = map { it.withTags() }
}
