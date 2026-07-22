package com.tristinbaker.defide.data.repository

import com.tristinbaker.defide.data.db.user.dao.SinHabitDao
import com.tristinbaker.defide.data.db.user.entity.SinHabitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SinHabitRepository @Inject constructor(
    private val sinHabitDao: SinHabitDao,
) {
    fun getAll(): Flow<List<SinHabitEntity>> = sinHabitDao.getAll()

    suspend fun addHabit(name: String) = withContext(Dispatchers.IO) {
        sinHabitDao.insert(
            SinHabitEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                createdAt = System.currentTimeMillis(),
                lastRelapseAt = null,
            )
        )
    }

    suspend fun logRelapse(id: String) = withContext(Dispatchers.IO) {
        sinHabitDao.logRelapse(id, System.currentTimeMillis())
    }

    suspend fun removeHabit(id: String) = withContext(Dispatchers.IO) {
        sinHabitDao.delete(id)
    }

    companion object {
        /** Days since the habit's last relapse, or since it was created if there's been none. */
        fun computeStreak(habit: SinHabitEntity): Int {
            val zone = ZoneId.systemDefault()
            val since = Instant.ofEpochMilli(habit.lastRelapseAt ?: habit.createdAt).atZone(zone).toLocalDate()
            val today = LocalDate.now()
            return ChronoUnit.DAYS.between(since, today).toInt().coerceAtLeast(0)
        }
    }
}
