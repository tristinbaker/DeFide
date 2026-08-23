package com.tristinbaker.defide.data.db.userbible

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Reader
import javax.inject.Inject
import javax.inject.Singleton

data class BibleImportResult(
    val translationId: String,
    val bookCount: Int,
    val verseCount: Int,
)

/**
 * Imports a Bible from a user-picked JSON file into [UserBibleDatabase].
 *
 * Expected shape (the format used by NRSVCE/NRSVUE JSON dumps and the
 * offline compile_content.py pipeline):
 *
 * ```
 * { "Old Testament": { "Genesis": { "1": { "1": "verse text", ... }, ... }, ... },
 *   "New Testament": { ... },
 *   "Deuterocanonical": { ... } }
 * ```
 *
 * Leading superscript verse numbers (¹ ² ³) are stripped from verse text.
 * Book names found in [BOOK_MANIFEST] get the app's canonical book numbering
 * so reading progress lines up across translations; unknown books are kept
 * and numbered after the known range.
 */
@Singleton
class UserBibleImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userBibles: UserBibleDatabase,
) {
    suspend fun import(uri: Uri, name: String): Result<BibleImportResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open $uri")
                stream.bufferedReader().use { reader -> importFrom(reader, name) }
            }
        }

    internal fun importFrom(reader: Reader, name: String): BibleImportResult {
        val translationName = name.trim()
        require(translationName.isNotEmpty()) { "Translation name is empty" }
        val translationId = UserBibleDatabase.TRANSLATION_ID_PREFIX + slugify(translationName)

        val db = userBibles.db
        val json = JsonReader(reader)
        db.beginTransaction()
        try {
            // Re-importing under the same name replaces the previous copy.
            UserBibleDatabase.deleteTranslationRows(db, translationId)

            db.execSQL(
                "INSERT INTO translations VALUES (?, ?, ?, ?)",
                arrayOf(translationId, translationName, "user", "user-imported"),
            )

            var nextBookId = maxOf(UserBibleDatabase.BOOK_ID_BASE.toLong(), queryMaxId(db, "books") + 1)
            var nextVerseId = maxOf(UserBibleDatabase.VERSE_ID_BASE.toLong(), queryMaxId(db, "verses") + 1)
            var unknownBookNumber = UNKNOWN_BOOK_NUMBER_BASE

            val bookStmt = db.compileStatement("INSERT INTO books VALUES (?, ?, ?, ?, ?, ?, ?)")
            val verseStmt = db.compileStatement("INSERT INTO verses VALUES (?, ?, ?, ?, ?)")
            val ftsStmt = db.compileStatement("INSERT INTO verses_fts(docid, text) VALUES (?, ?)")

            var bookCount = 0
            var verseCount = 0

            json.beginObject()
            while (json.hasNext()) {
                val testamentLabel = json.nextName()
                val fallbackTestament = TESTAMENT_LABELS[normalizeKey(testamentLabel)] ?: "DC"
                json.beginObject()
                while (json.hasNext()) {
                    val bookName = json.nextName().trim()
                    val entry = BOOK_MANIFEST[normalizeKey(bookName)]
                        ?: BookEntry(unknownBookNumber++, fallbackTestament, bookName, bookName, bookName)

                    val bookId = nextBookId++
                    bookStmt.bindLong(1, bookId)
                    bookStmt.bindString(2, translationId)
                    bookStmt.bindLong(3, entry.number.toLong())
                    bookStmt.bindString(4, entry.testament)
                    bookStmt.bindString(5, entry.shortName)
                    bookStmt.bindString(6, entry.fullName)
                    bookStmt.bindString(7, entry.drName)
                    bookStmt.executeInsert()
                    bookCount++

                    json.beginObject()
                    while (json.hasNext()) {
                        val chapter = json.nextName().trim().toIntOrNull()
                            ?: throw IllegalArgumentException("Non-numeric chapter key in $bookName")
                        json.beginObject()
                        while (json.hasNext()) {
                            val verse = json.nextName().trim().toIntOrNull()
                                ?: throw IllegalArgumentException("Non-numeric verse key in $bookName $chapter")
                            val text = cleanVerseText(json.nextString())
                            val verseId = nextVerseId++
                            verseStmt.bindLong(1, verseId)
                            verseStmt.bindLong(2, bookId)
                            verseStmt.bindLong(3, chapter.toLong())
                            verseStmt.bindLong(4, verse.toLong())
                            verseStmt.bindString(5, text)
                            verseStmt.executeInsert()
                            ftsStmt.bindLong(1, verseId)
                            ftsStmt.bindString(2, text)
                            ftsStmt.executeInsert()
                            verseCount++
                            require(verseCount <= MAX_VERSES) { "File has too many verses" }
                        }
                        json.endObject()
                    }
                    json.endObject()
                }
                json.endObject()
            }
            json.endObject()

            require(verseCount > 0) { "No verses found in file" }

            db.setTransactionSuccessful()
            return BibleImportResult(translationId, bookCount, verseCount)
        } finally {
            db.endTransaction()
            json.close()
        }
    }

    private fun queryMaxId(db: android.database.sqlite.SQLiteDatabase, table: String): Long =
        db.rawQuery("SELECT COALESCE(MAX(id), 0) FROM $table", null)
            .use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }

    internal data class BookEntry(
        val number: Int,
        val testament: String,
        val shortName: String,
        val fullName: String,
        val drName: String,
    )

    companion object {
        private const val MAX_VERSES = 200_000

        /** Books absent from the manifest get sequential numbers from here. */
        private const val UNKNOWN_BOOK_NUMBER_BASE = 200

        private val SUPERSCRIPT_PREFIX = Regex("^[⁰¹²³⁴⁵⁶⁷⁸⁹]+\\s*")

        internal fun cleanVerseText(raw: String): String =
            SUPERSCRIPT_PREFIX.replaceFirst(raw, "").trim()

        internal fun slugify(name: String): String =
            name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "bible" }

        private fun normalizeKey(name: String): String =
            name.trim().lowercase().replace(Regex("\\s+"), " ")

        private val TESTAMENT_LABELS = mapOf(
            "old testament" to "OT",
            "new testament" to "NT",
            "apocrypha" to "DC",
            "deuterocanonical" to "DC",
            "deuterocanon" to "DC",
        )

        /**
         * Canonical book numbering, mirroring NRSVCE_BOOK_MANIFEST in
         * scripts/compile_content.py: DRA numbers 1 to 73, DC-only extras 74+.
         * Keys are normalized (lowercase) book names, including common aliases.
         */
        private val BOOK_MANIFEST: Map<String, BookEntry> = buildMap {
            fun put(entry: BookEntry, vararg names: String) {
                names.forEach { put(normalizeKey(it), entry) }
            }
            // Old Testament
            put(BookEntry(1, "OT", "Gen", "Genesis", "Genesis"), "Genesis")
            put(BookEntry(2, "OT", "Ex", "Exodus", "Exodus"), "Exodus")
            put(BookEntry(3, "OT", "Lev", "Leviticus", "Leviticus"), "Leviticus")
            put(BookEntry(4, "OT", "Num", "Numbers", "Numbers"), "Numbers")
            put(BookEntry(5, "OT", "Deut", "Deuteronomy", "Deuteronomy"), "Deuteronomy")
            put(BookEntry(6, "OT", "Josh", "Joshua", "Joshua"), "Joshua", "Josue")
            put(BookEntry(7, "OT", "Judg", "Judges", "Judges"), "Judges")
            put(BookEntry(8, "OT", "Ruth", "Ruth", "Ruth"), "Ruth")
            put(BookEntry(9, "OT", "1 Sam", "1 Samuel", "1 Samuel"), "1 Samuel")
            put(BookEntry(10, "OT", "2 Sam", "2 Samuel", "2 Samuel"), "2 Samuel")
            put(BookEntry(11, "OT", "1 Kgs", "1 Kings", "1 Kings"), "1 Kings")
            put(BookEntry(12, "OT", "2 Kgs", "2 Kings", "2 Kings"), "2 Kings")
            put(BookEntry(13, "OT", "1 Chr", "1 Chronicles", "1 Chronicles"), "1 Chronicles", "1 Paralipomenon")
            put(BookEntry(14, "OT", "2 Chr", "2 Chronicles", "2 Chronicles"), "2 Chronicles", "2 Paralipomenon")
            put(BookEntry(15, "OT", "Ezra", "Ezra", "Ezra"), "Ezra")
            put(BookEntry(16, "OT", "Neh", "Nehemiah", "Nehemiah"), "Nehemiah")
            put(BookEntry(19, "OT", "Esth", "Esther", "Esther"), "Esther")
            put(BookEntry(22, "OT", "Job", "Job", "Job"), "Job")
            put(BookEntry(23, "OT", "Ps", "Psalms", "Psalms"), "Psalms", "Psalm")
            put(BookEntry(24, "OT", "Prov", "Proverbs", "Proverbs"), "Proverbs")
            put(BookEntry(25, "OT", "Eccl", "Ecclesiastes", "Ecclesiastes"), "Ecclesiastes", "Qoheleth")
            put(
                BookEntry(26, "OT", "Song", "Song of Songs", "Song of Solomon"),
                "Song of Solomon", "Song of Songs", "Canticles", "Canticle of Canticles",
            )
            put(BookEntry(29, "OT", "Isa", "Isaiah", "Isaiah"), "Isaiah", "Isaias")
            put(BookEntry(30, "OT", "Jer", "Jeremiah", "Jeremiah"), "Jeremiah", "Jeremias")
            put(BookEntry(31, "OT", "Lam", "Lamentations", "Lamentations"), "Lamentations")
            put(BookEntry(33, "OT", "Ezek", "Ezekiel", "Ezekiel"), "Ezekiel", "Ezechiel")
            put(BookEntry(34, "OT", "Dan", "Daniel", "Daniel"), "Daniel")
            put(BookEntry(35, "OT", "Hos", "Hosea", "Hosea"), "Hosea", "Osee")
            put(BookEntry(36, "OT", "Joel", "Joel", "Joel"), "Joel")
            put(BookEntry(37, "OT", "Amos", "Amos", "Amos"), "Amos")
            put(BookEntry(38, "OT", "Obad", "Obadiah", "Obadiah"), "Obadiah", "Abdias")
            put(BookEntry(39, "OT", "Jon", "Jonah", "Jonah"), "Jonah", "Jonas")
            put(BookEntry(40, "OT", "Mic", "Micah", "Micah"), "Micah", "Micheas")
            put(BookEntry(41, "OT", "Nah", "Nahum", "Nahum"), "Nahum")
            put(BookEntry(42, "OT", "Hab", "Habakkuk", "Habakkuk"), "Habakkuk", "Habacuc")
            put(BookEntry(43, "OT", "Zeph", "Zephaniah", "Zephaniah"), "Zephaniah", "Sophonias")
            put(BookEntry(44, "OT", "Hag", "Haggai", "Haggai"), "Haggai", "Aggeus")
            put(BookEntry(45, "OT", "Zech", "Zechariah", "Zechariah"), "Zechariah", "Zacharias")
            put(BookEntry(46, "OT", "Mal", "Malachi", "Malachi"), "Malachi", "Malachias")
            // Deuterocanon
            put(BookEntry(17, "DC", "Tob", "Tobit", "Tobit"), "Tobit", "Tobias")
            put(BookEntry(18, "DC", "Jdt", "Judith", "Judith"), "Judith")
            put(BookEntry(74, "DC", "Grk Esth", "Greek Esther", "Greek Esther"), "Greek Esther")
            put(BookEntry(20, "DC", "1 Mac", "1 Maccabees", "1 Maccabees"), "1 Maccabees", "1 Machabees")
            put(BookEntry(21, "DC", "2 Mac", "2 Maccabees", "2 Maccabees"), "2 Maccabees", "2 Machabees")
            put(BookEntry(27, "DC", "Wis", "Wisdom", "Wisdom"), "Wisdom", "Wisdom of Solomon")
            put(BookEntry(28, "DC", "Sir", "Sirach", "Sirach"), "Sirach", "Ecclesiasticus")
            put(BookEntry(32, "DC", "Bar", "Baruch", "Baruch"), "Baruch")
            put(BookEntry(75, "DC", "Pr Azar", "Prayer of Azariah", "Prayer Of Azariah"), "Prayer of Azariah")
            put(BookEntry(76, "DC", "Sus", "Susanna", "Susanna"), "Susanna")
            put(BookEntry(77, "DC", "Bel", "Bel and the Dragon", "Bel And The Dragon"), "Bel and the Dragon")
            put(BookEntry(78, "DC", "Let Jer", "Letter of Jeremiah", "Letter Of Jeremiah"), "Letter of Jeremiah")
            put(BookEntry(79, "DC", "1 Esd", "1 Esdras", "1 Esdras"), "1 Esdras")
            put(BookEntry(80, "DC", "Pr Man", "Prayer of Manasseh", "Prayer Of Manasseh"), "Prayer of Manasseh")
            put(BookEntry(81, "DC", "Ps 151", "Psalm 151", "Psalm 151"), "Psalm 151")
            put(BookEntry(82, "DC", "3 Mac", "3 Maccabees", "3 Maccabees"), "3 Maccabees")
            put(BookEntry(83, "DC", "2 Esd", "2 Esdras", "2 Esdras"), "2 Esdras")
            put(BookEntry(84, "DC", "4 Mac", "4 Maccabees", "4 Maccabees"), "4 Maccabees")
            // New Testament
            put(BookEntry(47, "NT", "Matt", "Matthew", "Matthew"), "Matthew")
            put(BookEntry(48, "NT", "Mark", "Mark", "Mark"), "Mark")
            put(BookEntry(49, "NT", "Luke", "Luke", "Luke"), "Luke")
            put(BookEntry(50, "NT", "John", "John", "John"), "John")
            put(BookEntry(51, "NT", "Acts", "Acts", "Acts"), "Acts", "Acts of the Apostles")
            put(BookEntry(52, "NT", "Rom", "Romans", "Romans"), "Romans")
            put(BookEntry(53, "NT", "1 Cor", "1 Corinthians", "1 Corinthians"), "1 Corinthians")
            put(BookEntry(54, "NT", "2 Cor", "2 Corinthians", "2 Corinthians"), "2 Corinthians")
            put(BookEntry(55, "NT", "Gal", "Galatians", "Galatians"), "Galatians")
            put(BookEntry(56, "NT", "Eph", "Ephesians", "Ephesians"), "Ephesians")
            put(BookEntry(57, "NT", "Phil", "Philippians", "Philippians"), "Philippians")
            put(BookEntry(58, "NT", "Col", "Colossians", "Colossians"), "Colossians")
            put(BookEntry(59, "NT", "1 Thess", "1 Thessalonians", "1 Thessalonians"), "1 Thessalonians")
            put(BookEntry(60, "NT", "2 Thess", "2 Thessalonians", "2 Thessalonians"), "2 Thessalonians")
            put(BookEntry(61, "NT", "1 Tim", "1 Timothy", "1 Timothy"), "1 Timothy")
            put(BookEntry(62, "NT", "2 Tim", "2 Timothy", "2 Timothy"), "2 Timothy")
            put(BookEntry(63, "NT", "Titus", "Titus", "Titus"), "Titus")
            put(BookEntry(64, "NT", "Phlm", "Philemon", "Philemon"), "Philemon")
            put(BookEntry(65, "NT", "Heb", "Hebrews", "Hebrews"), "Hebrews")
            put(BookEntry(66, "NT", "Jas", "James", "James"), "James")
            put(BookEntry(67, "NT", "1 Pet", "1 Peter", "1 Peter"), "1 Peter")
            put(BookEntry(68, "NT", "2 Pet", "2 Peter", "2 Peter"), "2 Peter")
            put(BookEntry(69, "NT", "1 Jn", "1 John", "1 John"), "1 John")
            put(BookEntry(70, "NT", "2 Jn", "2 John", "2 John"), "2 John")
            put(BookEntry(71, "NT", "3 Jn", "3 John", "3 John"), "3 John")
            put(BookEntry(72, "NT", "Jude", "Jude", "Jude"), "Jude")
            put(
                BookEntry(73, "NT", "Rev", "Revelation", "Revelation"),
                "Revelation", "Revelation of John", "Apocalypse",
            )
        }
    }
}
