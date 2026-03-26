package com.tristinbaker.defide.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.tristinbaker.defide.data.db.content.ContentDatabase
import com.tristinbaker.defide.data.db.content.dao.BibleDao
import com.tristinbaker.defide.data.db.content.dao.NovenaContentDao
import com.tristinbaker.defide.data.db.content.dao.PrayerDao
import com.tristinbaker.defide.data.db.content.dao.RosaryContentDao
import com.tristinbaker.defide.data.db.user.UserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase =
        Room.databaseBuilder(context, UserDatabase::class.java, "defide_user.db")
            .addMigrations(UserDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideContentSQLiteDatabase(contentDatabase: ContentDatabase): SQLiteDatabase =
        contentDatabase.db

    @Provides
    fun provideBibleDao(db: SQLiteDatabase) = BibleDao(db)

    @Provides
    fun providePrayerDao(db: SQLiteDatabase) = PrayerDao(db)

    @Provides
    fun provideNovenaContentDao(db: SQLiteDatabase) = NovenaContentDao(db)

    @Provides
    fun provideRosaryContentDao(db: SQLiteDatabase) = RosaryContentDao(db)

    @Provides
    fun provideRosarySessionDao(db: UserDatabase) = db.rosarySessionDao()

    @Provides
    fun provideBibleBookmarkDao(db: UserDatabase) = db.bibleBookmarkDao()

    @Provides
    fun provideBibleHighlightDao(db: UserDatabase) = db.bibleHighlightDao()

    @Provides
    fun provideBibleChapterReadDao(db: UserDatabase) = db.bibleChapterReadDao()

    @Provides
    fun provideNovenaProgressDao(db: UserDatabase) = db.novenaProgressDao()

    @Provides
    fun providePrayerLogDao(db: UserDatabase) = db.prayerLogDao()
}
