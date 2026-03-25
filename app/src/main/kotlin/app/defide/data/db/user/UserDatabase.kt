package app.defide.data.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import app.defide.data.db.user.dao.BibleBookmarkDao
import app.defide.data.db.user.dao.BibleHighlightDao
import app.defide.data.db.user.dao.NovenaProgressDao
import app.defide.data.db.user.dao.PrayerLogDao
import app.defide.data.db.user.dao.RosarySessionDao
import app.defide.data.db.user.entity.BibleBookmarkEntity
import app.defide.data.db.user.entity.BibleHighlightEntity
import app.defide.data.db.user.entity.NovenaProgressEntity
import app.defide.data.db.user.entity.PrayerLogEntity
import app.defide.data.db.user.entity.RosarySessionEntity

@Database(
    entities = [
        RosarySessionEntity::class,
        BibleBookmarkEntity::class,
        BibleHighlightEntity::class,
        NovenaProgressEntity::class,
        PrayerLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun rosarySessionDao(): RosarySessionDao
    abstract fun bibleBookmarkDao(): BibleBookmarkDao
    abstract fun bibleHighlightDao(): BibleHighlightDao
    abstract fun novenaProgressDao(): NovenaProgressDao
    abstract fun prayerLogDao(): PrayerLogDao
}
