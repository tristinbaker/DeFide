package app.defide.data.repository

import app.defide.data.db.content.dao.CatechismDao
import app.defide.data.model.CccSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatechismRepository @Inject constructor(
    private val catechismDao: CatechismDao,
) {
    suspend fun getParts(): List<Int> =
        withContext(Dispatchers.IO) { catechismDao.getParts() }

    suspend fun getSectionsByPart(part: Int): List<CccSection> =
        withContext(Dispatchers.IO) { catechismDao.getSectionsByPart(part) }

    suspend fun getById(id: Int): CccSection? =
        withContext(Dispatchers.IO) { catechismDao.getById(id) }

    suspend fun search(query: String): List<CccSection> =
        withContext(Dispatchers.IO) { catechismDao.search(query) }

    suspend fun getPrevId(id: Int): Int? =
        withContext(Dispatchers.IO) { catechismDao.getPrevId(id) }

    suspend fun getNextId(id: Int): Int? =
        withContext(Dispatchers.IO) { catechismDao.getNextId(id) }

    suspend fun getAll(limit: Int = 50, offset: Int = 0): List<CccSection> =
        withContext(Dispatchers.IO) { catechismDao.getAll(limit, offset) }
}
