package com.tristinbaker.defide.data.repository

import com.tristinbaker.defide.data.db.content.dao.PrayerDao
import com.tristinbaker.defide.data.db.user.dao.PrayerLogDao
import com.tristinbaker.defide.data.db.user.entity.PrayerLogEntity
import com.tristinbaker.defide.data.model.Prayer
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
    suspend fun getAll(language: String = "en"): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.getAll(language) }

    suspend fun getCategories(language: String = "en"): List<String> =
        withContext(Dispatchers.IO) { prayerDao.getCategories(language) }

    suspend fun getTags(language: String = "en"): List<String> =
        withContext(Dispatchers.IO) { prayerDao.getTags(language) }

    suspend fun getByCategory(category: String, language: String = "en"): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.getByCategory(category, language) }

    suspend fun getByTag(tag: String, language: String = "en"): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.getByTag(tag, language) }

    suspend fun getById(id: String, language: String = "en"): Prayer? =
        withContext(Dispatchers.IO) { prayerDao.getById(id, language) }

    suspend fun search(query: String, language: String = "en"): List<Prayer> =
        withContext(Dispatchers.IO) { prayerDao.search(query, language) }

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
