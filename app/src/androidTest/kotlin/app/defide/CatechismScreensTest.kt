package app.defide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.defide.data.model.CccSection
import app.defide.ui.catechism.CatechismHomeScreen
import app.defide.ui.catechism.CatechismViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CatechismScreensTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun catechismHomeScreen_topBarTitle_isDisplayed() {
        val viewModel = mockk<CatechismViewModel>(relaxed = true)
        every { viewModel.parts } returns MutableStateFlow(listOf(1, 2, 3, 4))
        every { viewModel.sections } returns MutableStateFlow(emptyList())
        every { viewModel.searchResults } returns MutableStateFlow(emptyList())

        composeRule.setContent {
            CatechismHomeScreen(
                onSectionSelected = {},
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Catechism").assertIsDisplayed()
    }

    @Test
    fun catechismHomeScreen_withSections_showsSectionHeadings() {
        val sections = listOf(
            CccSection(1, 1, null, null, null, "The Profession of Faith", "Body of paragraph 1."),
            CccSection(2, 1, null, null, null, "The Celebration of the Christian Mystery", "Body of paragraph 2."),
        )
        val viewModel = mockk<CatechismViewModel>(relaxed = true)
        every { viewModel.parts } returns MutableStateFlow(emptyList())
        every { viewModel.sections } returns MutableStateFlow(sections)
        every { viewModel.searchResults } returns MutableStateFlow(emptyList())

        composeRule.setContent {
            CatechismHomeScreen(
                onSectionSelected = {},
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("The Profession of Faith").assertIsDisplayed()
    }
}
