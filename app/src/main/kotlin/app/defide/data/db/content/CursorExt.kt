package app.defide.data.db.content

import android.database.Cursor
import app.defide.data.model.Book
import app.defide.data.model.MysteryBead
import app.defide.data.model.Mystery
import app.defide.data.model.Novena
import app.defide.data.model.NovenaDay
import app.defide.data.model.Prayer
import app.defide.data.model.Translation
import app.defide.data.model.Verse

fun Cursor.toTranslation() = Translation(
    id = getString(getColumnIndexOrThrow("id")),
    name = getString(getColumnIndexOrThrow("name")),
    language = getString(getColumnIndexOrThrow("language")),
    license = getString(getColumnIndexOrThrow("license")),
)

fun Cursor.toBook() = Book(
    id = getInt(getColumnIndexOrThrow("id")),
    translationId = getString(getColumnIndexOrThrow("translation_id")),
    bookNumber = getInt(getColumnIndexOrThrow("book_number")),
    testament = getString(getColumnIndexOrThrow("testament")),
    shortName = getString(getColumnIndexOrThrow("short_name")),
    fullName = getString(getColumnIndexOrThrow("full_name")),
    drName = getString(getColumnIndexOrThrow("dr_name")),
)

fun Cursor.toVerse() = Verse(
    id = getInt(getColumnIndexOrThrow("id")),
    bookId = getInt(getColumnIndexOrThrow("book_id")),
    chapter = getInt(getColumnIndexOrThrow("chapter")),
    verse = getInt(getColumnIndexOrThrow("verse")),
    text = getString(getColumnIndexOrThrow("text")),
)

fun Cursor.toPrayer() = Prayer(
    id = getString(getColumnIndexOrThrow("id")),
    title = getString(getColumnIndexOrThrow("title")),
    body = getString(getColumnIndexOrThrow("body")),
    source = getStringOrNull("source"),
    category = getString(getColumnIndexOrThrow("category")),
)

fun Cursor.toNovena() = Novena(
    id = getString(getColumnIndexOrThrow("id")),
    title = getString(getColumnIndexOrThrow("title")),
    description = getStringOrNull("description"),
    totalDays = getInt(getColumnIndexOrThrow("total_days")),
    feastDay = getStringOrNull("feast_day"),
)

fun Cursor.toNovenaDay() = NovenaDay(
    id = getInt(getColumnIndexOrThrow("id")),
    novenaId = getString(getColumnIndexOrThrow("novena_id")),
    dayNumber = getInt(getColumnIndexOrThrow("day_number")),
    title = getStringOrNull("title"),
    body = getString(getColumnIndexOrThrow("body")),
)

fun Cursor.toMystery() = Mystery(
    id = getString(getColumnIndexOrThrow("id")),
    name = getString(getColumnIndexOrThrow("name")),
    traditionalDays = getStringOrNull("traditional_days"),
)

fun Cursor.toMysteryBead() = MysteryBead(
    id = getInt(getColumnIndexOrThrow("id")),
    mysteryId = getString(getColumnIndexOrThrow("mystery_id")),
    position = getInt(getColumnIndexOrThrow("position")),
    prayerId = getStringOrNull("prayer_id"),
    mysteryNumber = getIntOrNull("mystery_number"),
    mysteryTitle = getStringOrNull("mystery_title"),
    mysteryScripture = getStringOrNull("mystery_scripture"),
    mysteryMeditation = getStringOrNull("mystery_meditation"),
)

private fun Cursor.getStringOrNull(column: String): String? {
    val idx = getColumnIndexOrThrow(column)
    return if (isNull(idx)) null else getString(idx)
}

private fun Cursor.getIntOrNull(column: String): Int? {
    val idx = getColumnIndexOrThrow(column)
    return if (isNull(idx)) null else getInt(idx)
}

/** Iterates all rows, applying [transform] to each, then closes the cursor. */
inline fun <T> Cursor.mapRows(transform: Cursor.() -> T): List<T> = use {
    val result = mutableListOf<T>()
    while (moveToNext()) result.add(transform())
    result
}

/** Returns the first row or null, then closes the cursor. */
inline fun <T> Cursor.firstOrNull(transform: Cursor.() -> T): T? = use {
    if (moveToFirst()) transform() else null
}
