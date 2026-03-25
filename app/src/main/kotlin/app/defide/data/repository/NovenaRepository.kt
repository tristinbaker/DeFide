package app.defide.data.repository

import app.defide.data.db.content.dao.NovenaContentDao
import app.defide.data.db.user.dao.NovenaProgressDao
import app.defide.data.db.user.entity.NovenaProgressEntity
import app.defide.data.model.Novena
import app.defide.data.model.NovenaDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovenaRepository @Inject constructor(
    private val novenaContentDao: NovenaContentDao,
    private val novenaProgressDao: NovenaProgressDao,
) {
    suspend fun getAll(): List<Novena> =
        withContext(Dispatchers.IO) { novenaContentDao.getAll() }

    suspend fun getById(id: String): Novena? =
        withContext(Dispatchers.IO) { novenaContentDao.getById(id) }

    suspend fun getDay(novenaId: String, dayNumber: Int): NovenaDay? =
        withContext(Dispatchers.IO) { novenaContentDao.getDay(novenaId, dayNumber) }

    // Progress
    fun getActiveProgress(): Flow<List<NovenaProgressEntity>> =
        novenaProgressDao.getActive()

    fun getCompletedProgress(): Flow<List<NovenaProgressEntity>> =
        novenaProgressDao.getCompleted()

    suspend fun getProgressForNovena(novenaId: String): NovenaProgressEntity? =
        withContext(Dispatchers.IO) { novenaProgressDao.getByNovenaId(novenaId) }

    suspend fun startNovena(
        novenaId: String,
        startDate: String,
        notificationsEnabled: Boolean = false,
        notificationTime: String? = null,
    ) = withContext(Dispatchers.IO) {
        novenaProgressDao.insert(
            NovenaProgressEntity(
                id = UUID.randomUUID().toString(),
                novenaId = novenaId,
                startDate = startDate,
                notificationsEnabled = notificationsEnabled,
                notificationTime = notificationTime,
            )
        )
    }

    suspend fun completeDay(progressId: String, day: Int) =
        withContext(Dispatchers.IO) { novenaProgressDao.advanceDay(progressId, day) }

    suspend fun completeNovena(progressId: String) =
        withContext(Dispatchers.IO) { novenaProgressDao.markComplete(progressId) }
}
