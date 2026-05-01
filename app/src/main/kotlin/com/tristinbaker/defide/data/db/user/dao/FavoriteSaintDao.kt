package com.tristinbaker.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tristinbaker.defide.data.db.user.entity.FavoriteSaintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSaintDao {
    @Query("SELECT * FROM favorite_saints")
    fun getAll(): Flow<List<FavoriteSaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteSaintEntity)

    @Query("DELETE FROM favorite_saints WHERE saint_id = :saintId")
    suspend fun delete(saintId: String)

    @Query("SELECT COUNT(*) FROM favorite_saints WHERE saint_id = :saintId")
    suspend fun isFavorite(saintId: String): Int
}
