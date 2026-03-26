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

    fun getMysteries(): List<Mystery> =
        db.rawQuery("SELECT * FROM mysteries ORDER BY id", null)
            .mapRows { toMystery() }

    fun getMystery(id: String): Mystery? =
        db.rawQuery("SELECT * FROM mysteries WHERE id = ?", arrayOf(id))
            .firstOrNull { toMystery() }

    fun getBeads(mysteryId: String): List<MysteryBead> =
        db.rawQuery(
            "SELECT * FROM mystery_beads WHERE mystery_id = ? ORDER BY position",
            arrayOf(mysteryId),
        ).mapRows { toMysteryBead() }
}
