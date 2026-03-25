package app.defide.data.db.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.defide.data.db.user.entity.PrayerLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PrayerLogEntity)

    @Query("SELECT * FROM prayer_log ORDER BY prayed_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<PrayerLogEntity>>

    @Query("SELECT COUNT(*) FROM prayer_log WHERE prayer_id = :prayerId")
    suspend fun getCountForPrayer(prayerId: String): Int
}
