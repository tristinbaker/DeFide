package app.defide.data.repository

import app.defide.data.db.content.dao.RosaryContentDao
import app.defide.data.db.user.dao.RosarySessionDao
import app.defide.data.db.user.entity.RosarySessionEntity
import app.defide.data.model.Mystery
import app.defide.data.model.MysteryBead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RosaryRepository @Inject constructor(
    private val rosaryContentDao: RosaryContentDao,
    private val rosarySessionDao: RosarySessionDao,
) {
    suspend fun getMysteries(): List<Mystery> =
        withContext(Dispatchers.IO) { rosaryContentDao.getMysteries() }

    suspend fun getMystery(id: String): Mystery? =
        withContext(Dispatchers.IO) { rosaryContentDao.getMystery(id) }

    suspend fun getBeads(mysteryId: String): List<MysteryBead> =
        withContext(Dispatchers.IO) { rosaryContentDao.getBeads(mysteryId) }

    fun getRecentSessions(limit: Int = 10): Flow<List<RosarySessionEntity>> =
        rosarySessionDao.getRecent(limit)

    suspend fun startSession(mysteryId: String): String =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            rosarySessionDao.insert(
                RosarySessionEntity(
                    id = id,
                    mysteryId = mysteryId,
                    startedAt = System.currentTimeMillis(),
                    completedAt = null,
                )
            )
            id
        }

    suspend fun completeSession(sessionId: String) =
        withContext(Dispatchers.IO) {
            rosarySessionDao.markComplete(sessionId, System.currentTimeMillis())
        }
}
