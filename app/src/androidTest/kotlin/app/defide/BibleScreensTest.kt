package app.defide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.defide.data.model.Book
import app.defide.ui.bible.BibleHomeScreen
import app.defide.ui.bible.BibleViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class BibleScreensTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun bibleHomeScreen_showsOldAndNewTestamentSections() {
        val books = listOf(
            Book(1, "dra", 1, "OT", "Gen", "Genesis", "Genesis"),
            Book(2, "dra", 2, "OT", "Exo", "Exodus", "Exodus"),
            Book(3, "dra", 40, "NT", "Matt", "Matthew", "Matthew"),
        )
        val viewModel = mockk<BibleViewModel>(relaxed = true)
        every { viewModel.books } returns MutableStateFlow(books)
        every { viewModel.selectedTranslationId } returns MutableStateFlow("dra")
        every { viewModel.translations } returns MutableStateFlow(emptyList())

        composeRule.setContent {
            BibleHomeScreen(
                onBookSelected = { _, _ -> },
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Old Testament").assertIsDisplayed()
        composeRule.onNodeWithText("New Testament").assertIsDisplayed()
        composeRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeRule.onNodeWithText("Matthew").assertIsDisplayed()
    }

    @Test
    fun bibleHomeScreen_bookClickCallsCallback() {
        var selectedBook: Int? = null
        val books = listOf(
            Book(1, "dra", 1, "OT", "Gen", "Genesis", "Genesis"),
        )
        val viewModel = mockk<BibleViewModel>(relaxed = true)
        every { viewModel.books } returns MutableStateFlow(books)
        every { viewModel.selectedTranslationId } returns MutableStateFlow("dra")
        every { viewModel.translations } returns MutableStateFlow(emptyList())

        composeRule.setContent {
            BibleHomeScreen(
                onBookSelected = { _, bookNumber -> selectedBook = bookNumber },
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Genesis").performClick()
        assert(selectedBook == 1)
    }
}
