package com.mfouad.muslimtv.core.data.family

import com.mfouad.muslimtv.core.common.TimeProvider
import com.mfouad.muslimtv.core.data.profile.ActiveProfile
import com.mfouad.muslimtv.core.data.quran.QuranMetadata
import com.mfouad.muslimtv.core.database.KhatmahDao
import com.mfouad.muslimtv.core.database.KhatmahEntity
import com.mfouad.muslimtv.core.database.ProfileDao
import com.mfouad.muslimtv.core.database.ProfileEntity
import com.mfouad.muslimtv.core.datastore.SettingsDataSource
import com.mfouad.muslimtv.core.model.KhatmahPlan
import com.mfouad.muslimtv.core.model.KhatmahPlanner
import com.mfouad.muslimtv.core.model.KhatmahPortion
import com.mfouad.muslimtv.core.model.Profile
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Family profiles and each profile's khatmah plan. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FamilyRepository @Inject constructor(
    private val profiles: ProfileDao,
    private val khatmahDao: KhatmahDao,
    private val settings: SettingsDataSource,
    private val active: ActiveProfile,
    private val metadata: QuranMetadata,
    private val time: TimeProvider,
) {
    val all: Flow<List<Profile>> = profiles.all()
        .onStart { ensureDefault() }
        .map { list -> list.map { Profile(it.id, it.name) } }

    val activeId: Flow<Long> = active.id

    private suspend fun ensureDefault() {
        if (profiles.count() == 0) profiles.insert(ProfileEntity(id = 1, name = DEFAULT_NAME, createdAt = time.nowMillis()))
    }

    suspend fun add(name: String): Long {
        ensureDefault()
        return profiles.insert(ProfileEntity(name = name.trim().take(MAX_NAME), createdAt = time.nowMillis()))
    }

    suspend fun switchTo(id: Long) = settings.setActiveProfile(id)

    /** Deletes a family member and everything that was theirs. The default profile stays. */
    suspend fun delete(id: Long) {
        if (id == 1L) return
        profiles.deleteBookmarks(id); profiles.deletePositions(id); profiles.deleteTahfeez(id); profiles.deleteKhatmah(id)
        profiles.delete(id)
        if (active.id.first() == id) settings.setActiveProfile(1)
    }

    // ---- Khatmah ------------------------------------------------------------------------------

    val khatmah: Flow<KhatmahPlan?> = active.id.flatMapLatest { id ->
        khatmahDao.plan(id).map { e -> e?.let { KhatmahPlan(it.days, it.startEpochDay, it.completedPortions) } }
    }

    suspend fun startKhatmah(days: Int, today: LocalDate) =
        khatmahDao.save(KhatmahEntity(active.id.first(), days.coerceIn(1, KhatmahPlanner.PAGES), today.toEpochDay(), 0, time.nowMillis()))

    suspend fun markPortionDone() {
        val plan = khatmah.first() ?: return
        khatmahDao.save(KhatmahEntity(active.id.first(), plan.days, plan.startEpochDay, (plan.completed + 1).coerceAtMost(plan.days), time.nowMillis()))
    }

    suspend fun stopKhatmah() = khatmahDao.delete(active.id.first())

    fun portion(plan: KhatmahPlan, day: Int): KhatmahPortion = KhatmahPlanner.portion(day.coerceIn(1, plan.days), plan.days, metadata.pageStarts)

    companion object {
        const val DEFAULT_NAME = "Family"
        const val MAX_NAME = 20
    }
}
