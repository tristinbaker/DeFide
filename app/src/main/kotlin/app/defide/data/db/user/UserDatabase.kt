package app.defide.data.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.defide.data.db.user.dao.BibleBookmarkDao
import app.defide.data.db.user.dao.BibleChapterReadDao
import app.defide.data.db.user.dao.BibleHighlightDao
import app.defide.data.db.user.dao.NovenaProgressDao
import app.defide.data.db.user.dao.PrayerLogDao
import app.defide.data.db.user.dao.RosarySessionDao
import app.defide.data.db.user.entity.BibleBookmarkEntity
import app.defide.data.db.user.entity.BibleChapterReadEntity
import app.defide.data.db.user.entity.BibleHighlightEntity
import app.defide.data.db.user.entity.NovenaProgressEntity
import app.defide.data.db.user.entity.PrayerLogEntity
import app.defide.data.db.user.entity.RosarySessionEntity

@Database(
    entities = [
        RosarySessionEntity::class,
        BibleBookmarkEntity::class,
        BibleHighlightEntity::class,
        BibleChapterReadEntity::class,
        NovenaProgressEntity::class,
        PrayerLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun rosarySessionDao(): RosarySessionDao
    abstract fun bibleBookmarkDao(): BibleBookmarkDao
    abstract fun bibleHighlightDao(): BibleHighlightDao
    abstract fun bibleChapterReadDao(): BibleChapterReadDao
    abstract fun novenaProgressDao(): NovenaProgressDao
    abstract fun prayerLogDao(): PrayerLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `bible_chapter_read` (
                        `translation_id` TEXT NOT NULL,
                        `book_number` INTEGER NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `read_at` INTEGER NOT NULL,
                        PRIMARY KEY(`translation_id`, `book_number`, `chapter`)
                    )"""
                )
            }
        }
    }
}
