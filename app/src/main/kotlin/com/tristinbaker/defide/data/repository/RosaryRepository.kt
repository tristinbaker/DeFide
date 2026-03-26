package com.tristinbaker.defide.data.repository

import com.tristinbaker.defide.data.db.content.dao.RosaryContentDao
import com.tristinbaker.defide.data.db.user.dao.RosarySessionDao
import com.tristinbaker.defide.data.db.user.entity.RosarySessionEntity
import com.tristinbaker.defide.data.model.Mystery
import com.tristinbaker.defide.data.model.MysteryBead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    fun getCompletedSessions(): Flow<List<RosarySessionEntity>> =
        rosarySessionDao.getAllCompleted()

    companion object {
        fun computeRosaryStreak(sessions: List<RosarySessionEntity>): Int {
            val zone = ZoneId.systemDefault()
            val datesWithSession = sessions
                .mapNotNull { it.completedAt }
                .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                .toSet()
            val today = LocalDate.now()
            var streak = 0
            var day = today
            while (datesWithSession.contains(day)) {
                streak++
                day = day.minusDays(1)
            }
            return streak
        }
    }
}
