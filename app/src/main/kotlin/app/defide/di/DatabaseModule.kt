package app.defide.di

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import app.defide.data.db.content.ContentDatabase
import app.defide.data.db.content.dao.BibleDao
import app.defide.data.db.content.dao.CatechismDao
import app.defide.data.db.content.dao.NovenaContentDao
import app.defide.data.db.content.dao.PrayerDao
import app.defide.data.db.content.dao.RosaryContentDao
import app.defide.data.db.user.UserDatabase
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
            .build()

    @Provides
    @Singleton
    fun provideContentSQLiteDatabase(contentDatabase: ContentDatabase): SQLiteDatabase =
        contentDatabase.db

    @Provides
    fun provideBibleDao(db: SQLiteDatabase) = BibleDao(db)

    @Provides
    fun provideCatechismDao(db: SQLiteDatabase) = CatechismDao(db)

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
    fun provideNovenaProgressDao(db: UserDatabase) = db.novenaProgressDao()

    @Provides
    fun providePrayerLogDao(db: UserDatabase) = db.prayerLogDao()
}
