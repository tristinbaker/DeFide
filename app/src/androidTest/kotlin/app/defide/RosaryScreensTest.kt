package app.defide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.defide.data.model.Mystery
import app.defide.ui.rosary.RosaryHomeScreen
import app.defide.ui.rosary.RosaryViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class RosaryScreensTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun rosaryHomeScreen_showsAllFourMysteries() {
        val mysteries = listOf(
            Mystery("joyful", "Joyful Mysteries", "Monday,Saturday"),
            Mystery("sorrowful", "Sorrowful Mysteries", "Tuesday,Friday"),
            Mystery("glorious", "Glorious Mysteries", "Wednesday,Sunday"),
            Mystery("luminous", "Luminous Mysteries", "Thursday"),
        )
        val viewModel = mockk<RosaryViewModel>(relaxed = true)
        every { viewModel.mysteries } returns MutableStateFlow(mysteries)
        every { viewModel.todaysMysteryId } returns "joyful"

        composeRule.setContent {
            RosaryHomeScreen(
                onStartSession = {},
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Joyful Mysteries").assertIsDisplayed()
        composeRule.onNodeWithText("Sorrowful Mysteries").assertIsDisplayed()
        composeRule.onNodeWithText("Glorious Mysteries").assertIsDisplayed()
        composeRule.onNodeWithText("Luminous Mysteries").assertIsDisplayed()
    }
}
