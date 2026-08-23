package com.tristinbaker.defide

import androidx.test.platform.app.InstrumentationRegistry
import com.tristinbaker.defide.data.db.content.mapRows
import com.tristinbaker.defide.data.db.content.toBook
import com.tristinbaker.defide.data.db.content.toVerse
import com.tristinbaker.defide.data.db.userbible.UserBibleDatabase
import com.tristinbaker.defide.data.db.userbible.UserBibleImporter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.StringReader

private const val TEST_DB = "test_user_bibles.db"

private const val SAMPLE_JSON = """
{
  "Old Testament": {
    "Genesis": {
      "1": {
        "1": "¹ In the beginning God created the heavens and the earth. ",
        "2": "² The earth was without form."
      },
      "2": { "1": "Thus the heavens were finished." }
    }
  },
  "New Testament": {
    "Matthew": { "1": { "1": "The book of the genealogy." } }
  },
  "Apocrypha": {
    "Tobit": { "1": { "1": "The book of the words of Tobit." } }
  }
}
"""

class UserBibleImporterTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var userBibles: UserBibleDatabase
    private lateinit var importer: UserBibleImporter

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
        userBibles = UserBibleDatabase(context, TEST_DB)
        importer = UserBibleImporter(context, userBibles)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun import_createsTranslationBooksAndVerses() {
        val result = importer.importFrom(StringReader(SAMPLE_JSON), "Test Bible")

        assertEquals("user_test-bible", result.translationId)
        assertEquals(3, result.bookCount)
        assertEquals(5, result.verseCount)

        val translations = userBibles.getTranslations()
        assertEquals(1, translations.size)
        assertEquals("Test Bible", translations[0].name)

        val books = userBibles.db.rawQuery(
            "SELECT * FROM books WHERE translation_id = ? ORDER BY book_number",
            arrayOf(result.translationId),
        ).mapRows { toBook() }
        assertEquals(listOf(1, 17, 47), books.map { it.bookNumber })
        assertEquals(listOf("OT", "DC", "NT"), books.map { it.testament })
        assertEquals(listOf("Genesis", "Tobit", "Matthew"), books.map { it.fullName })
        assertTrue(books.all { it.id >= UserBibleDatabase.BOOK_ID_BASE })
    }

    @Test
    fun import_stripsSuperscriptVerseNumbers() {
        val result = importer.importFrom(StringReader(SAMPLE_JSON), "Test Bible")
        val genesis = userBibles.db.rawQuery(
            "SELECT * FROM books WHERE translation_id = ? AND book_number = 1",
            arrayOf(result.translationId),
        ).mapRows { toBook() }.single()

        val verses = userBibles.db.rawQuery(
            "SELECT * FROM verses WHERE book_id = ? AND chapter = 1 ORDER BY verse",
            arrayOf(genesis.id.toString()),
        ).mapRows { toVerse() }
        assertEquals("In the beginning God created the heavens and the earth.", verses[0].text)
        assertEquals("The earth was without form.", verses[1].text)
        assertTrue(verses.all { it.id >= UserBibleDatabase.VERSE_ID_BASE })
    }

    @Test
    fun import_populatesFullTextSearch() {
        importer.importFrom(StringReader(SAMPLE_JSON), "Test Bible")
        val matches = userBibles.db.rawQuery(
            "SELECT docid FROM verses_fts WHERE verses_fts MATCH ?",
            arrayOf("genealogy"),
        ).use { c ->
            generateSequence { if (c.moveToNext()) c.getLong(0) else null }.count()
        }
        assertEquals(1, matches)
    }

    @Test
    fun reimport_underSameName_replacesPreviousCopy() {
        importer.importFrom(StringReader(SAMPLE_JSON), "Test Bible")
        val result = importer.importFrom(StringReader(SAMPLE_JSON), "Test Bible")

        assertEquals(1, userBibles.getTranslations().size)
        val bookCount = userBibles.db.rawQuery(
            "SELECT COUNT(*) FROM books WHERE translation_id = ?",
            arrayOf(result.translationId),
        ).use { c -> c.moveToFirst(); c.getInt(0) }
        assertEquals(3, bookCount)
    }

    @Test
    fun deleteTranslation_removesAllRows() {
        val result = importer.importFrom(StringReader(SAMPLE_JSON), "Test Bible")
        userBibles.deleteTranslation(result.translationId)

        for (table in listOf("translations", "books", "verses", "verses_fts")) {
            val count = userBibles.db.rawQuery("SELECT COUNT(*) FROM $table", null)
                .use { c -> c.moveToFirst(); c.getInt(0) }
            assertEquals("$table not empty", 0, count)
        }
    }

    /** End-to-end check against a real NRSVUE-style dump; skipped unless the file was pushed. */
    @Test
    fun import_realDump_ifPresent() {
        val file = java.io.File(context.filesDir, "bible.json")
        org.junit.Assume.assumeTrue(file.exists())

        val result = importer.importFrom(file.reader(), "NRSVUE")
        assertEquals(84, result.bookCount)
        assertTrue("expected a full bible, got ${result.verseCount} verses", result.verseCount > 30_000)

        val unknownBooks = userBibles.db.rawQuery(
            "SELECT full_name FROM books WHERE translation_id = ? AND book_number >= 200",
            arrayOf(result.translationId),
        ).mapRows { getString(0) }
        assertEquals(emptyList<String>(), unknownBooks)
    }

    @Test
    fun import_rejectsMalformedJson() {
        var failed = false
        try {
            importer.importFrom(StringReader("{\"not\": \"a bible\"}"), "Broken")
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
        assertEquals(0, userBibles.getTranslations().size)
    }
}
