package com.tristinbaker.defide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tristinbaker.defide.data.db.user.entity.NovenaProgressEntity
import com.tristinbaker.defide.data.model.Novena
import com.tristinbaker.defide.ui.novena.NovenaListScreen
import com.tristinbaker.defide.ui.novena.NovenaProgressScreen
import com.tristinbaker.defide.ui.novena.NovenaViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NovenaScreensTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun novenaListScreen_showsNovenas() {
        val novenas = listOf(
            Novena("divine-mercy", "Divine Mercy Novena", "Description.", 9, null),
            Novena("sacred-heart", "Sacred Heart Novena", null, 9, "2024-06-07"),
        )
        val viewModel = mockk<NovenaViewModel>(relaxed = true)
        every { viewModel.novenas } returns MutableStateFlow(novenas)
        every { viewModel.activeNovenas } returns MutableStateFlow(emptyList())
        every { viewModel.completedNovenas } returns MutableStateFlow(emptyList())
        every { viewModel.novenaTitles } returns MutableStateFlow(emptyMap())

        composeRule.setContent {
            NovenaListScreen(
                onNovenaSelected = {},
                onProgressSelected = {},
                onOpenDrawer = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("Divine Mercy Novena").assertIsDisplayed()
        composeRule.onNodeWithText("Sacred Heart Novena").assertIsDisplayed()
    }

    @Test
    fun novenaProgressScreen_withNoNovenas_showsEmptyMessage() {
        val viewModel = mockk<NovenaViewModel>(relaxed = true)
        every { viewModel.novenas } returns MutableStateFlow(emptyList())
        every { viewModel.activeNovenas } returns MutableStateFlow(emptyList())
        every { viewModel.completedNovenas } returns MutableStateFlow(emptyList())
        every { viewModel.novenaTitles } returns MutableStateFlow(emptyMap())

        composeRule.setContent {
            NovenaProgressScreen(
                onNovenaSelected = { _, _ -> },
                onBack = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("No novenas started yet.").assertIsDisplayed()
    }

    @Test
    fun novenaProgressScreen_withActiveNovena_showsInProgressSection() {
        val progress = NovenaProgressEntity(
            id = "p1",
            novenaId = "divine-mercy",
            startDate = "2024-04-07",
            lastCompletedDay = 2,
            completed = false,
            notificationsEnabled = false,
            notificationTime = null,
        )
        val viewModel = mockk<NovenaViewModel>(relaxed = true)
        every { viewModel.novenas } returns MutableStateFlow(emptyList())
        every { viewModel.activeNovenas } returns MutableStateFlow(listOf(progress))
        every { viewModel.completedNovenas } returns MutableStateFlow(emptyList())
        every { viewModel.novenaTitles } returns MutableStateFlow(mapOf("divine-mercy" to "Divine Mercy Novena"))

        composeRule.setContent {
            NovenaProgressScreen(
                onNovenaSelected = { _, _ -> },
                onBack = {},
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithText("In Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Divine Mercy Novena").assertIsDisplayed()
    }
}
