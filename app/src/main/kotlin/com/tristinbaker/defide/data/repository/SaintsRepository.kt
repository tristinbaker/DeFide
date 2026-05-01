package com.tristinbaker.defide.data.repository

import com.tristinbaker.defide.data.db.content.dao.SaintsContentDao
import com.tristinbaker.defide.data.db.user.dao.FavoriteSaintDao
import com.tristinbaker.defide.data.db.user.entity.FavoriteSaintEntity
import com.tristinbaker.defide.data.model.Saint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaintsRepository @Inject constructor(
    private val saintsContentDao: SaintsContentDao,
    private val favoriteSaintDao: FavoriteSaintDao,
) {
    suspend fun getAll(language: String = "en"): List<Saint> =
        withContext(Dispatchers.IO) { saintsContentDao.getAll(language) }

    suspend fun getById(id: String, language: String = "en"): Saint? =
        withContext(Dispatchers.IO) { saintsContentDao.getById(id, language) }

    fun getFavoriteIds(): Flow<Set<String>> =
        favoriteSaintDao.getAll().map { list -> list.map { it.saintId }.toSet() }

    suspend fun toggleFavorite(saintId: String) {
        withContext(Dispatchers.IO) {
            if (favoriteSaintDao.isFavorite(saintId) > 0) {
                favoriteSaintDao.delete(saintId)
            } else {
                favoriteSaintDao.insert(FavoriteSaintEntity(saintId))
            }
        }
    }
}
