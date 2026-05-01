package com.tristinbaker.defide.data.db.user

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tristinbaker.defide.data.db.user.dao.BibleBookmarkDao
import com.tristinbaker.defide.data.db.user.dao.BibleChapterReadDao
import com.tristinbaker.defide.data.db.user.dao.BibleHighlightDao
import com.tristinbaker.defide.data.db.user.dao.FavoriteSaintDao
import com.tristinbaker.defide.data.db.user.dao.NovenaProgressDao
import com.tristinbaker.defide.data.db.user.dao.PrayerLogDao
import com.tristinbaker.defide.data.db.user.dao.RosarySessionDao
import com.tristinbaker.defide.data.db.user.entity.BibleBookmarkEntity
import com.tristinbaker.defide.data.db.user.entity.BibleChapterReadEntity
import com.tristinbaker.defide.data.db.user.entity.BibleHighlightEntity
import com.tristinbaker.defide.data.db.user.entity.FavoriteSaintEntity
import com.tristinbaker.defide.data.db.user.entity.NovenaProgressEntity
import com.tristinbaker.defide.data.db.user.entity.PrayerLogEntity
import com.tristinbaker.defide.data.db.user.entity.RosarySessionEntity

@Database(
    entities = [
        RosarySessionEntity::class,
        BibleBookmarkEntity::class,
        BibleHighlightEntity::class,
        BibleChapterReadEntity::class,
        NovenaProgressEntity::class,
        PrayerLogEntity::class,
        FavoriteSaintEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun rosarySessionDao(): RosarySessionDao
    abstract fun bibleBookmarkDao(): BibleBookmarkDao
    abstract fun bibleHighlightDao(): BibleHighlightDao
    abstract fun bibleChapterReadDao(): BibleChapterReadDao
    abstract fun novenaProgressDao(): NovenaProgressDao
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun favoriteSaintDao(): FavoriteSaintDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `bible_chapter_read_new` (
                        `book_number` INTEGER NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `read_at` INTEGER NOT NULL,
                        PRIMARY KEY(`book_number`, `chapter`)
                    )"""
                )
                db.execSQL(
                    """INSERT OR IGNORE INTO `bible_chapter_read_new` (`book_number`, `chapter`, `read_at`)
                       SELECT `book_number`, `chapter`, MIN(`read_at`)
                       FROM `bible_chapter_read`
                       GROUP BY `book_number`, `chapter`"""
                )
                db.execSQL("DROP TABLE `bible_chapter_read`")
                db.execSQL("ALTER TABLE `bible_chapter_read_new` RENAME TO `bible_chapter_read`")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `favorite_saints` (
                        `saint_id` TEXT NOT NULL,
                        PRIMARY KEY(`saint_id`)
                    )"""
                )
            }
        }
    }
}
