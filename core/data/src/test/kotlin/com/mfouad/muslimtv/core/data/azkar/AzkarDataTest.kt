package com.mfouad.muslimtv.core.data.azkar

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.json.JSONObject
import org.junit.Test

class AzkarDataTest {
    private val hisn = JSONObject(File("src/main/assets/azkar/hisn.json").readText())
    private val titles = hisn.keys().asSequence().toList()

    @Test
    fun `every daily category matches exactly one Hisn al-Muslim chapter`() {
        AzkarRepository.CATEGORY_TITLES.forEach { (category, key) ->
            assertThat(titles.count { AzkarRepository.letters(it) == key } to category).isEqualTo(1 to category)
        }
    }

    @Test
    fun `every zikr has a source reference and a positive count`() {
        titles.forEach { t ->
            val arr = hisn.getJSONObject(t).getJSONArray("Adhkar")
            (0 until arr.length()).forEach { i ->
                val o = arr.getJSONObject(i)
                assertThat(o.optString("Reference")).isNotEmpty()
                assertThat(o.optInt("Count", 1)).isAtLeast(1)
            }
        }
    }

    @Test
    fun `ninety nine names are bundled`() {
        assertThat(JSONObject(File("src/main/assets/azkar/asma.json").readText()).getJSONArray("names").length()).isEqualTo(99)
    }
}
