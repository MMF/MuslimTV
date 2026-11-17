package com.mfouad.muslimtv.core.data.city

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.model.City
import org.junit.Test

class CitySearchTest {
    private fun city(id: Long, name: String, ar: String) = IndexedCity(
        City(id, name, ar, "XX", 0.0, 0.0, "UTC"),
        listOf(name, ar).map(CitySearch::normalize),
    )

    private val cities = listOf(
        city(1, "Cairo", "القاهرة"),
        city(2, "Makkah", "مكة المكرمة"),
        city(3, "Madinah", "المدينة"),
        city(4, "New Cairo", ""),
    )

    @Test
    fun `prefix matches rank before substring matches`() {
        assertThat(CitySearch.rank(cities, CitySearch.normalize("cai"), 10).map { it.id }).containsExactly(1L, 4L).inOrder()
    }

    @Test
    fun `english aliases find the right city`() {
        assertThat(CitySearch.rank(cities, CitySearch.normalize("Mecca"), 10).map { it.id }).containsExactly(2L)
        assertThat(CitySearch.rank(cities, CitySearch.normalize("medina"), 10).map { it.id }).containsExactly(3L)
    }

    @Test
    fun `arabic search ignores hamza and taa marbuta differences`() {
        assertThat(CitySearch.rank(cities, CitySearch.normalize("القاهره"), 10).map { it.id }).containsExactly(1L)
        assertThat(CitySearch.rank(cities, CitySearch.normalize("مكه"), 10).map { it.id }).containsExactly(2L)
    }
}

class CityAssetKeysTest {
    /** The build-time keys in cities.txt must match what CitySearch.normalize produces at runtime. */
    @Test
    fun `precomputed search keys match runtime normalization`() {
        val lines = java.io.File("src/main/assets/cities/cities.txt").readLines().filter { !it.startsWith("#") }
        lines.take(3000).forEach { line ->
            val p = line.split('|')
            val expected = listOf(p[1], p[2], p[3]).map(CitySearch::normalize).filter { it.isNotEmpty() }.distinct()
            assertThat(p[9].split(';').filter { it.isNotEmpty() }).containsExactlyElementsIn(expected)
        }
    }
}
