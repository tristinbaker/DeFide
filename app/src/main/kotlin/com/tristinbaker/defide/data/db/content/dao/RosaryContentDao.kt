package com.tristinbaker.defide.data.db.content.dao

import android.database.sqlite.SQLiteDatabase
import com.tristinbaker.defide.data.db.content.firstOrNull
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toMystery
import com.tristinbaker.defide.data.db.content.toMysteryBead
import com.tristinbaker.defide.data.model.Mystery
import com.tristinbaker.defide.data.model.MysteryBead
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosaryContentDao @Inject constructor(private val db: SQLiteDatabase) {

    fun getMysteries(language: String = "en"): List<Mystery> =
        db.rawQuery("SELECT * FROM mysteries WHERE language = ? ORDER BY id", arrayOf(language))
            .mapRows { toMystery() }

    fun getMystery(id: String, language: String = "en"): Mystery? =
        db.rawQuery("SELECT * FROM mysteries WHERE id = ? AND language = ?", arrayOf(id, language))
            .firstOrNull { toMystery() }

    fun getBeads(mysteryId: String, language: String = "en", variant: String = "dominican"): List<MysteryBead> =
        db.rawQuery(
            "SELECT * FROM mystery_beads WHERE mystery_id = ? AND language = ? AND variant = ? ORDER BY position",
            arrayOf(mysteryId, language, variant),
        ).mapRows { toMysteryBead() }
}
