package app.defide

import app.defide.data.model.Book
import app.defide.data.model.Translation
import app.defide.data.model.Verse
import app.defide.data.preferences.AppTheme
import app.defide.data.preferences.UserPreferences
import app.defide.data.preferences.UserPreferencesRepository
import app.defide.data.repository.BibleRepository
import app.defide.ui.bible.BibleViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BibleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val translations = listOf(
        Translation("dra", "Douay-Rheims 1899", "en", "Public domain"),
        Translation("vulgate", "Latin Vulgate", "la", "Public domain"),
    )
    private val books = listOf(
        Book(1, "dra", 1, "OT", "Gen", "Genesis", "Genesis"),
        Book(2, "dra", 40, "NT", "Matt", "Matthew", "Matthew"),
    )
    private val verses = listOf(
        Verse(1, 1, 1, 1, "In the beginning God created the heaven and the earth."),
        Verse(2, 1, 1, 2, "And the earth was void and empty."),
    )

    private lateinit var bibleRepo: BibleRepository
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var viewModel: BibleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        bibleRepo = mockk(relaxed = true)
        prefsRepo = mockk()

        coEvery { bibleRepo.getTranslations() } returns translations
        coEvery { bibleRepo.getBooks("dra") } returns books
        coEvery { bibleRepo.getBooks("vulgate") } returns emptyList()
        coEvery { bibleRepo.getChapterCount(any()) } returns 50
        coEvery { bibleRepo.getVerses(any(), any()) } returns verses
        coEvery { bibleRepo.search("dra", any()) } answers {
            val query = secondArg<String>()
            verses.filter { it.text.contains(query, ignoreCase = true) }
        }

        every { prefsRepo.preferences } returns flowOf(
            UserPreferences(theme = AppTheme.SYSTEM, bibleTranslationId = "dra")
        )

        viewModel = BibleViewModel(bibleRepo, prefsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial translation is dra from preferences`() = runTest {
        advanceUntilIdle()
        assertEquals("dra", viewModel.selectedTranslationId.value)
    }

    @Test
    fun `books are loaded for dra on init`() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.books.value.size)
        assertEquals("Genesis", viewModel.books.value[0].fullName)
    }

    @Test
    fun `loadVerses populates verses`() = runTest {
        viewModel.loadVerses(bookId = 1, chapter = 1)
        advanceUntilIdle()
        assertEquals(2, viewModel.verses.value.size)
    }

    @Test
    fun `loadChapterCount updates chapterCount`() = runTest {
        viewModel.loadChapterCount(bookId = 1)
        advanceUntilIdle()
        assertEquals(50, viewModel.chapterCount.value)
    }

    @Test
    fun `search returns matching verses`() = runTest {
        viewModel.search("beginning")
        advanceUntilIdle()
        assertEquals(1, viewModel.searchResults.value.size)
        assertTrue(viewModel.searchResults.value[0].text.contains("beginning", ignoreCase = true))
    }

    @Test
    fun `blank search clears results`() = runTest {
        viewModel.search("beginning")
        advanceUntilIdle()
        viewModel.search("  ")
        advanceUntilIdle()
        assertTrue(viewModel.searchResults.value.isEmpty())
    }

    @Test
    fun `addBookmark delegates to repository`() = runTest {
        advanceUntilIdle()
        viewModel.addBookmark(bookNumber = 1, chapter = 3, verse = 16)
        advanceUntilIdle()
        coVerify { bibleRepo.addBookmark("dra", 1, 3, 16, null) }
    }

    @Test
    fun `addHighlight delegates to repository`() = runTest {
        viewModel.addHighlight(verseId = 99, color = "blue")
        advanceUntilIdle()
        coVerify { bibleRepo.addHighlight(99, "blue") }
    }
}
