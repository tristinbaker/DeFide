package com.tristinbaker.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import com.tristinbaker.defide.data.db.content.firstOrNull
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toSaint
import com.tristinbaker.defide.data.model.Saint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaintsContentDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getAll(language: String = "en"): List<Saint> =
        db.rawQuery(
            "SELECT * FROM saints WHERE language = ? ORDER BY name",
            arrayOf(language),
        ).mapRows { toSaint() }

    fun getById(id: String, language: String = "en"): Saint? =
        db.rawQuery(
            "SELECT * FROM saints WHERE id = ? AND language = ?",
            arrayOf(id, language),
        ).firstOrNull { toSaint() }

    fun getByCategory(category: String, language: String = "en"): List<Saint> =
        db.rawQuery(
            "SELECT * FROM saints WHERE category = ? AND language = ? ORDER BY name",
            arrayOf(category, language),
        ).mapRows { toSaint() }
}
