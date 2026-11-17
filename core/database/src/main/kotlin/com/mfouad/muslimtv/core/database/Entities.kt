package com.mfouad.muslimtv.core.database

import androidx.room.Entity
import androidx.room.Index

/** One ayah of the Tanzil Uthmani text, stored verbatim. */
@Entity(tableName = "ayah", primaryKeys = ["surah", "ayah"], indices = [Index("juz")])
data class AyahEntity(
    val surah: Int,
    val ayah: Int,
    val juz: Int,
    val text: String,
)

/** Translation or tafsir text for an ayah, keyed by source id (e.g. "en.pickthall"). */
@Entity(tableName = "ayah_note", primaryKeys = ["source", "surah", "ayah"])
data class AyahNoteEntity(
    val source: String,
    val surah: Int,
    val ayah: Int,
    val text: String,
)

@Entity(tableName = "bookmark", primaryKeys = ["profileId", "surah", "ayah"])
data class BookmarkEntity(
    val profileId: Long,
    val surah: Int,
    val ayah: Int,
    val createdAt: Long,
)

/** Last reading or listening position per profile. kind = "read" or "listen". */
@Entity(tableName = "position", primaryKeys = ["profileId", "kind"])
data class PositionEntity(
    val profileId: Long,
    val kind: String,
    val surah: Int,
    val ayah: Int,
    val reciterId: String?,
    val updatedAt: Long,
)

/**
 * One item in the download manager. id = "audio:<reciter>:<surah>" for recitations, or
 * "extra:<kind>:<id>" for translations, timing data and nature scenes.
 */
@Entity(tableName = "download")
data class DownloadEntity(
    @androidx.room.PrimaryKey val id: String,
    val kind: String,
    val reciterId: String?,
    val surah: Int?,
    val url: String?,
    val title: String,
    val state: String,
    val filesDone: Int,
    val filesTotal: Int,
    val bytesDone: Long,
    val bytesEstimate: Long,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A memorization session, saved so it can be resumed. pauseKind: NONE, SECONDS or AYAH. */
@Entity(tableName = "tahfeez_session", indices = [Index("profileId")])
data class TahfeezSessionEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val reciterId: String,
    val repeats: Int,
    val rounds: Int,
    val pauseKind: String,
    val pauseSeconds: Int,
    val hideText: Boolean,
    val ayah: Int,
    val repeat: Int,
    val round: Int,
    val finished: Boolean,
    val updatedAt: Long,
)

/** A family member. Profile 1 always exists ("Family"). */
@Entity(tableName = "profile")
data class ProfileEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** One khatmah plan per profile: finish the Quran in [days] days, one portion per day. */
@Entity(tableName = "khatmah", primaryKeys = ["profileId"])
data class KhatmahEntity(
    val profileId: Long,
    val days: Int,
    val startEpochDay: Long,
    val completedPortions: Int,
    val updatedAt: Long,
)
