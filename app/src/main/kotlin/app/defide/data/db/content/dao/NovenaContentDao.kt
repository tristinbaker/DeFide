package app.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import app.defide.data.db.content.firstOrNull
import app.defide.data.db.content.mapRows
import app.defide.data.db.content.toNovena
import app.defide.data.db.content.toNovenaDay
import app.defide.data.model.Novena
import app.defide.data.model.NovenaDay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovenaContentDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getAll(): List<Novena> =
        db.rawQuery("SELECT * FROM novenas ORDER BY title", null)
            .mapRows { toNovena() }

    fun getById(id: String): Novena? =
        db.rawQuery("SELECT * FROM novenas WHERE id = ?", arrayOf(id))
            .firstOrNull { toNovena() }

    fun getDay(novenaId: String, dayNumber: Int): NovenaDay? =
        db.rawQuery(
            "SELECT * FROM novena_days WHERE novena_id = ? AND day_number = ?",
            arrayOf(novenaId, dayNumber.toString()),
        ).firstOrNull { toNovenaDay() }

    fun getDays(novenaId: String): List<NovenaDay> =
        db.rawQuery(
            "SELECT * FROM novena_days WHERE novena_id = ? ORDER BY day_number",
            arrayOf(novenaId),
        ).mapRows { toNovenaDay() }
}
