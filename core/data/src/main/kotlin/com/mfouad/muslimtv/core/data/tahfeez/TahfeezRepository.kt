package com.mfouad.muslimtv.core.data.tahfeez

import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.profile.ActiveProfile
import com.mfouad.muslimtv.core.database.TahfeezDao
import com.mfouad.muslimtv.core.database.TahfeezSessionEntity
import com.mfouad.muslimtv.core.model.PauseMode
import com.mfouad.muslimtv.core.model.TahfeezConfig
import com.mfouad.muslimtv.core.model.TahfeezState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class TahfeezSession(val id: Long, val config: TahfeezConfig, val state: TahfeezState, val finished: Boolean, val updatedAt: Long)

/** Saved memorization sessions, per family member. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TahfeezRepository @Inject constructor(
    private val dao: TahfeezDao,
    private val profile: ActiveProfile,
    private val time: TimeProvider,
) {
    val sessions: Flow<List<TahfeezSession>> = profile.id.flatMapLatest { id -> dao.sessions(id).map { list -> list.map { it.toModel() } } }

    suspend fun get(id: Long): TahfeezSession? = dao.get(id)?.toModel()

    /** Saves progress; returns the session id (new sessions get one on first save). */
    suspend fun save(id: Long?, config: TahfeezConfig, state: TahfeezState): Long {
        val (kind, seconds) = when (val p = config.pause) {
            PauseMode.None -> "NONE" to 0
            is PauseMode.Seconds -> "SECONDS" to p.seconds
            PauseMode.AyahLength -> "AYAH" to 0
        }
        val e = TahfeezSessionEntity(
            id = id ?: 0, profileId = profile.id.first(), surah = config.surah, fromAyah = config.fromAyah, toAyah = config.toAyah,
            reciterId = config.reciterId, repeats = config.repeatsPerAyah, rounds = config.rangeRepeats, pauseKind = kind,
            pauseSeconds = seconds, hideText = config.hideText, ayah = state.ayah, repeat = state.repeat, round = state.round,
            finished = state.phase == TahfeezState.Phase.DONE, updatedAt = time.nowMillis(),
        )
        val rowId = dao.upsert(e)
        return if (id != null && id > 0) id else rowId
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun TahfeezSessionEntity.toModel() = TahfeezSession(
        id = id,
        config = TahfeezConfig(
            surah, fromAyah, toAyah, reciterId, repeats, rounds,
            when (pauseKind) { "NONE" -> PauseMode.None; "SECONDS" -> PauseMode.Seconds(pauseSeconds); else -> PauseMode.AyahLength },
            hideText,
        ),
        state = TahfeezState(ayah = ayah, repeat = repeat, round = round, phase = if (finished) TahfeezState.Phase.DONE else TahfeezState.Phase.LISTEN),
        finished = finished,
        updatedAt = updatedAt,
    )
}
