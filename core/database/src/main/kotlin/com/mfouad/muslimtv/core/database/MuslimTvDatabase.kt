package com.mfouad.muslimtv.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [AyahEntity::class, AyahNoteEntity::class, BookmarkEntity::class, PositionEntity::class, DownloadEntity::class, TahfeezSessionEntity::class, ProfileEntity::class, KhatmahEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class MuslimTvDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun userQuranDao(): UserQuranDao
    abstract fun downloadDao(): DownloadDao
    abstract fun tahfeezDao(): TahfeezDao
    abstract fun profileDao(): ProfileDao
    abstract fun khatmahDao(): KhatmahDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MuslimTvDatabase =
        Room.databaseBuilder(context, MuslimTvDatabase::class.java, "muslimtv.db").build()

    @Provides fun quranDao(db: MuslimTvDatabase): QuranDao = db.quranDao()

    @Provides fun userQuranDao(db: MuslimTvDatabase): UserQuranDao = db.userQuranDao()

    @Provides fun downloadDao(db: MuslimTvDatabase): DownloadDao = db.downloadDao()

    @Provides fun tahfeezDao(db: MuslimTvDatabase): TahfeezDao = db.tahfeezDao()

    @Provides fun profileDao(db: MuslimTvDatabase): ProfileDao = db.profileDao()

    @Provides fun khatmahDao(db: MuslimTvDatabase): KhatmahDao = db.khatmahDao()
}
