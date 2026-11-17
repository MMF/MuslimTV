package com.mfouad.muslimtv.core.data.tools

import com.google.common.truth.Truth.assertThat
import com.mfouad.muslimtv.core.model.City
import com.mfouad.muslimtv.core.model.HijriDate
import java.io.File
import java.time.LocalDate
import org.junit.Test

class ToolsDataTest {
    @Test
    fun `qibla from known cities`() {
        val london = QiblaCalculator.forCity(City(1, "London", "", "GB", 51.5085, -0.1257, "Europe/London"))
        assertThat(london.bearingDegrees).isWithin(1.0).of(119.0)
        assertThat(london.distanceKm).isWithin(30.0).of(4790.0)
        val cairo = QiblaCalculator.forCity(City(2, "Cairo", "", "EG", 30.0626, 31.2497, "Africa/Cairo"))
        assertThat(cairo.bearingDegrees).isWithin(1.0).of(136.0)
        assertThat(QiblaCalculator.compassPoint(119.0)).isEqualTo("ESE")
        assertThat(london.path.last().first).isWithin(0.01).of(QiblaCalculator.KAABA_LAT)
    }

    @Test
    fun `eid and tashreeq are never recommended for fasting`() {
        val monday = LocalDate.of(2026, 11, 16)
        assertThat(IslamicCalendar.fasting(HijriDate(1447, 10, 1), monday)).isEqualTo(FastingNote.FORBIDDEN)
        assertThat(IslamicCalendar.fasting(HijriDate(1447, 12, 13), monday)).isEqualTo(FastingNote.FORBIDDEN)
        assertThat(IslamicCalendar.fasting(HijriDate(1447, 12, 9), monday)).isEqualTo(FastingNote.RECOMMENDED)
        assertThat(IslamicCalendar.occasions(HijriDate(1447, 12, 9))).containsExactly(Occasion.DAY_OF_ARAFAH)
        assertThat(IslamicCalendar.fasting(HijriDate(1448, 3, 14), LocalDate.of(2026, 11, 18))).isEqualTo(FastingNote.RECOMMENDED)
    }

    @Test
    fun `bundled remote config parses and every stream uses https`() {
        val c = RemoteConfigRepository.parse(File("src/main/assets/config/remote.json").readText())
        assertThat(c.live).hasSize(2)
        assertThat(c.radio).isNotEmpty()
        (c.live + c.radio).forEach { assertThat(it.url).startsWith("https://") }
    }
}
