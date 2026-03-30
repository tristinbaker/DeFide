package com.tristinbaker.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import com.tristinbaker.defide.data.db.content.firstOrNull
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toNovena
import com.tristinbaker.defide.data.db.content.toNovenaDay
import com.tristinbaker.defide.data.model.Novena
import com.tristinbaker.defide.data.model.NovenaDay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovenaContentDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getAll(language: String = "en"): List<Novena> =
        db.rawQuery("SELECT * FROM novenas WHERE language = ? ORDER BY title", arrayOf(language))
            .mapRows { toNovena() }

    fun getById(id: String, language: String = "en"): Novena? =
        db.rawQuery("SELECT * FROM novenas WHERE id = ? AND language = ?", arrayOf(id, language))
            .firstOrNull { toNovena() }

    fun getDay(novenaId: String, dayNumber: Int, language: String = "en"): NovenaDay? =
        db.rawQuery(
            "SELECT * FROM novena_days WHERE novena_id = ? AND language = ? AND day_number = ?",
            arrayOf(novenaId, language, dayNumber.toString()),
        ).firstOrNull { toNovenaDay() }

    fun getDays(novenaId: String, language: String = "en"): List<NovenaDay> =
        db.rawQuery(
            "SELECT * FROM novena_days WHERE novena_id = ? AND language = ? ORDER BY day_number",
            arrayOf(novenaId, language),
        ).mapRows { toNovenaDay() }
}
