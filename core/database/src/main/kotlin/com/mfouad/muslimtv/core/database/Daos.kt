package com.mfouad.muslimtv.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT COUNT(*) FROM ayah")
    suspend fun ayahCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(rows: List<AyahEntity>)

    @Query("SELECT * FROM ayah WHERE surah = :surah ORDER BY ayah")
    suspend fun surah(surah: Int): List<AyahEntity>

    @Query("SELECT * FROM ayah WHERE surah = :surah AND ayah = :ayah")
    suspend fun ayah(surah: Int, ayah: Int): AyahEntity?

    @Query("SELECT surah, MIN(ayah) AS ayah FROM ayah WHERE juz = :juz GROUP BY surah ORDER BY surah LIMIT 1")
    suspend fun juzStart(juz: Int): AyahStart?

    @Query("SELECT DISTINCT source FROM ayah_note")
    suspend fun noteSources(): List<String>

    @Query("SELECT COUNT(*) FROM ayah_note WHERE source = :source")
    suspend fun noteCount(source: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(rows: List<AyahNoteEntity>)

    @Query("SELECT * FROM ayah_note WHERE source = :source AND surah = :surah ORDER BY ayah")
    suspend fun notes(source: String, surah: Int): List<AyahNoteEntity>

    @Query("SELECT * FROM ayah_note WHERE source = :source AND surah = :surah AND ayah = :ayah")
    suspend fun note(source: String, surah: Int, ayah: Int): AyahNoteEntity?
}

data class AyahStart(val surah: Int, val ayah: Int)

@Dao
interface UserQuranDao {
    @Query("SELECT * FROM bookmark WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun bookmarks(profileId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(b: BookmarkEntity)

    @Query("DELETE FROM bookmark WHERE profileId = :profileId AND surah = :surah AND ayah = :ayah")
    suspend fun removeBookmark(profileId: Long, surah: Int, ayah: Int)

    @Upsert
    suspend fun savePosition(p: PositionEntity)

    @Query("SELECT * FROM position WHERE profileId = :profileId AND kind = :kind")
    fun position(profileId: Long, kind: String): Flow<PositionEntity?>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download ORDER BY createdAt")
    fun all(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download WHERE id = :id")
    suspend fun get(id: String): DownloadEntity?

    /** Next item to work on: interrupted ones first, then the oldest queued. */
    @Query("SELECT * FROM download WHERE state IN ('DOWNLOADING', 'QUEUED') ORDER BY CASE state WHEN 'DOWNLOADING' THEN 0 ELSE 1 END, createdAt LIMIT 1")
    suspend fun next(): DownloadEntity?

    @Upsert
    suspend fun upsert(e: DownloadEntity)

    @Upsert
    suspend fun upsertAll(e: List<DownloadEntity>)

    @Query("DELETE FROM download WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM download WHERE reciterId = :reciterId")
    suspend fun deleteReciter(reciterId: String)
}

@Dao
interface TahfeezDao {
    @Query("SELECT * FROM tahfeez_session WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun sessions(profileId: Long): Flow<List<TahfeezSessionEntity>>

    @Query("SELECT * FROM tahfeez_session WHERE id = :id")
    suspend fun get(id: Long): TahfeezSessionEntity?

    @Upsert
    suspend fun upsert(e: TahfeezSessionEntity): Long

    @Query("DELETE FROM tahfeez_session WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile ORDER BY id")
    fun all(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profile")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: ProfileEntity): Long

    @Query("DELETE FROM profile WHERE id = :id AND id != 1")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmark WHERE profileId = :id")
    suspend fun deleteBookmarks(id: Long)

    @Query("DELETE FROM position WHERE profileId = :id")
    suspend fun deletePositions(id: Long)

    @Query("DELETE FROM tahfeez_session WHERE profileId = :id")
    suspend fun deleteTahfeez(id: Long)

    @Query("DELETE FROM khatmah WHERE profileId = :id")
    suspend fun deleteKhatmah(id: Long)
}

@Dao
interface KhatmahDao {
    @Query("SELECT * FROM khatmah WHERE profileId = :profileId")
    fun plan(profileId: Long): Flow<KhatmahEntity?>

    @Upsert
    suspend fun save(k: KhatmahEntity)

    @Query("DELETE FROM khatmah WHERE profileId = :profileId")
    suspend fun delete(profileId: Long)
}
