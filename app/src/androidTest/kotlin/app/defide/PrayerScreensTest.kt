package app.defide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.defide.data.model.Prayer
import app.defide.ui.prayers.PrayerSearchScreen
import app.defide.ui.prayers.PrayerViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PrayerScreensTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun prayerSearchScreen_showsPrayers() {
        val prayers = listOf(
            Prayer("hail-mary", "Hail Mary", "Hail Mary, full of grace...", null, "devotional", listOf("mary")),
            Prayer("our-father", "Our Father", "Our Father, who art in heaven...", null, "devotional", listOf()),
        )
        val viewModel = mockk<PrayerViewModel>(relaxed = true)
        every { viewModel.results } returns MutableStateFlow(prayers)
        every { viewModel.tags } returns MutableStateFlow(emptyList())

        composeRule.setContent {
            PrayerSearchScreen(
                onPrayerSelected = {},
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Hail Mary").assertIsDisplayed()
        composeRule.onNodeWithText("Our Father").assertIsDisplayed()
    }

    @Test
    fun prayerSearchScreen_topBarTitle_isDisplayed() {
        val viewModel = mockk<PrayerViewModel>(relaxed = true)
        every { viewModel.results } returns MutableStateFlow(emptyList())
        every { viewModel.tags } returns MutableStateFlow(emptyList())

        composeRule.setContent {
            PrayerSearchScreen(
                onPrayerSelected = {},
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Prayers").assertIsDisplayed()
    }
}
