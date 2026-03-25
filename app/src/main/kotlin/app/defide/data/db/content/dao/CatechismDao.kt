package app.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import app.defide.data.db.content.firstOrNull
import app.defide.data.db.content.mapRows
import app.defide.data.db.content.toCccSection
import app.defide.data.model.CccSection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatechismDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getParts(): List<Int> =
        db.rawQuery("SELECT DISTINCT part FROM ccc_sections WHERE part IS NOT NULL ORDER BY part", null)
            .mapRows { getInt(0) }

    fun getSectionsByPart(part: Int): List<CccSection> =
        db.rawQuery(
            "SELECT * FROM ccc_sections WHERE part = ? ORDER BY id",
            arrayOf(part.toString()),
        ).mapRows { toCccSection() }

    fun getById(id: Int): CccSection? =
        db.rawQuery("SELECT * FROM ccc_sections WHERE id = ?", arrayOf(id.toString()))
            .firstOrNull { toCccSection() }

    /** FTS5 search by keyword or paragraph number. */
    fun search(query: String): List<CccSection> {
        // If query looks like a paragraph number, do a direct lookup
        val num = query.trim().toIntOrNull()
        if (num != null) {
            return db.rawQuery(
                "SELECT * FROM ccc_sections WHERE id = ?",
                arrayOf(num.toString()),
            ).mapRows { toCccSection() }
        }
        val safeQuery = "\"${query.replace("\"", " ")}\""
        return try {
            db.rawQuery(
                """
                SELECT * FROM ccc_sections
                WHERE id IN (SELECT docid FROM ccc_fts WHERE ccc_fts MATCH ?)
                ORDER BY id
                LIMIT 100
                """.trimIndent(),
                arrayOf(safeQuery),
            ).mapRows { toCccSection() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPrevId(id: Int): Int? =
        db.rawQuery(
            "SELECT id FROM ccc_sections WHERE id < ? ORDER BY id DESC LIMIT 1",
            arrayOf(id.toString()),
        ).firstOrNull { getInt(0) }

    fun getNextId(id: Int): Int? =
        db.rawQuery(
            "SELECT id FROM ccc_sections WHERE id > ? ORDER BY id ASC LIMIT 1",
            arrayOf(id.toString()),
        ).firstOrNull { getInt(0) }

    fun getAll(limit: Int = 50, offset: Int = 0): List<CccSection> =
        db.rawQuery(
            "SELECT * FROM ccc_sections ORDER BY id LIMIT ? OFFSET ?",
            arrayOf(limit.toString(), offset.toString()),
        ).mapRows { toCccSection() }
}
