package app.defide

import app.defide.data.db.user.entity.NovenaProgressEntity
import app.defide.data.model.Novena
import app.defide.data.model.NovenaDay
import app.defide.data.repository.NovenaRepository
import app.defide.ui.novena.NovenaViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NovenaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleNovenas = listOf(
        Novena("immaculate-heart", "Novena to the Immaculate Heart", "A nine-day novena.", 9, "2024-08-22"),
        Novena("divine-mercy", "Divine Mercy Novena", "Good Friday to Divine Mercy Sunday.", 9, null),
    )
    private val sampleProgress = NovenaProgressEntity(
        id = "prog-1",
        novenaId = "immaculate-heart",
        startDate = "2024-08-13",
        lastCompletedDay = 3,
        completed = false,
        notificationsEnabled = false,
        notificationTime = null,
    )
    private val sampleDay = NovenaDay(1, "immaculate-heart", 4, "Day 4", "Hail Mary, full of grace...")

    private lateinit var repo: NovenaRepository
    private lateinit var viewModel: NovenaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)

        coEvery { repo.getAll() } returns sampleNovenas
        every { repo.getActiveProgress() } returns flowOf(listOf(sampleProgress))
        every { repo.getCompletedProgress() } returns flowOf(emptyList())

        viewModel = NovenaViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `novenas loaded on init`() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.novenas.value.size)
    }

    @Test
    fun `novena titles map populated on init`() = runTest {
        advanceUntilIdle()
        assertEquals("Novena to the Immaculate Heart", viewModel.novenaTitles.value["immaculate-heart"])
        assertEquals("Divine Mercy Novena", viewModel.novenaTitles.value["divine-mercy"])
    }

    @Test
    fun `loadDetail sets detail state`() = runTest {
        coEvery { repo.getById("immaculate-heart") } returns sampleNovenas[0]
        viewModel.loadDetail("immaculate-heart")
        advanceUntilIdle()
        assertNotNull(viewModel.detail.value)
        assertEquals("immaculate-heart", viewModel.detail.value?.id)
    }

    @Test
    fun `loadDetail with unknown id results in null`() = runTest {
        coEvery { repo.getById("unknown") } returns null
        viewModel.loadDetail("unknown")
        advanceUntilIdle()
        assertNull(viewModel.detail.value)
    }

    @Test
    fun `loadProgress sets progress state`() = runTest {
        coEvery { repo.getProgressForNovena("immaculate-heart") } returns sampleProgress
        viewModel.loadProgress("immaculate-heart")
        advanceUntilIdle()
        assertEquals("prog-1", viewModel.progress.value?.id)
        assertEquals(3, viewModel.progress.value?.lastCompletedDay)
    }

    @Test
    fun `startNovena calls repository then reloads progress`() = runTest {
        coEvery { repo.getProgressForNovena("divine-mercy") } returns null
        viewModel.startNovena("divine-mercy", "2024-04-07")
        advanceUntilIdle()
        coVerify { repo.startNovena("divine-mercy", "2024-04-07") }
    }

    @Test
    fun `completeDay advances day and reloads`() = runTest {
        coEvery { repo.getById("immaculate-heart") } returns sampleNovenas[0]
        coEvery { repo.getProgressForNovena("immaculate-heart") } returns sampleProgress
        coEvery { repo.getDay("immaculate-heart", 4) } returns sampleDay

        viewModel.loadDetail("immaculate-heart")
        viewModel.loadProgress("immaculate-heart")
        advanceUntilIdle()

        viewModel.completeDay("immaculate-heart")
        advanceUntilIdle()

        // Day 4 (lastCompletedDay=3, so nextDay=4)
        coVerify { repo.completeDay("prog-1", 4) }
    }
}
