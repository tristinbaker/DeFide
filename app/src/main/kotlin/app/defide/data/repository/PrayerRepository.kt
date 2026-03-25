package app.defide.data.repository

import app.defide.data.db.content.dao.PrayerDao
import app.defide.data.db.user.dao.PrayerLogDao
import app.defide.data.db.user.entity.PrayerLogEntity
import app.defide.data.model.Prayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepository @Inject constructor(
    private val prayerDao: PrayerDao,
    private val prayerLogDao: PrayerLogDao,
) {
    suspend fun getAll(): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.getAll() }

    suspend fun getCategories(): List<String> =
        withContext(Dispatchers.IO) { prayerDao.getCategories() }

    suspend fun getTags(): List<String> =
        withContext(Dispatchers.IO) { prayerDao.getTags() }

    suspend fun getByCategory(category: String): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.getByCategory(category) }

    suspend fun getByTag(tag: String): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.getByTag(tag) }

    suspend fun getById(id: String): Prayer? =
        withContext(Dispatchers.IO) { prayerDao.getById(id) }

    suspend fun search(query: String): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.search(query) }

    fun getRecentLog(limit: Int = 20): Flow<List<PrayerLogEntity>> =
        prayerLogDao.getRecent(limit)

    suspend fun logPrayer(prayerId: String) =
        withContext(Dispatchers.IO) {
            prayerLogDao.insert(
                PrayerLogEntity(
                    id = UUID.randomUUID().toString(),
                    prayerId = prayerId,
                    prayedAt = System.currentTimeMillis(),
                )
            )
        }
}
