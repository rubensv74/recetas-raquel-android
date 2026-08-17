package com.rmm.recetasraquel.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecetoriaGoldenEditorialTokensTest {
    @Test
    fun officialTokensRemainExact() {
        assertEquals(Color(0xFF211B16), EspressoInk)
        assertEquals(Color(0xFF9E621C), ToastedBronze)
        assertEquals(Color(0xFFC58A2A), AntiqueGold)
        assertEquals(Color(0xFFFAF7F2), SoftIvory)
        assertEquals(Color(0xFFE8E0D6), WarmStone)
        assertEquals(Color(0xFFA95010), ConfirmedPresence)
        assertEquals(Color(0xFF33709F), MayContain)
        assertEquals(Color(0xFFB5473C), CriticalRisk)
    }

    @Test
    fun keyLightAndDarkPairsMeetWcagAa() {
        assertTrue(contrast(EspressoInk, SoftIvory) >= 4.5)
        assertTrue(contrast(ToastedBronze, SoftIvory) >= 4.5)
        assertTrue(contrast(EspressoInk, AntiqueGold) >= 4.5)
        assertTrue(contrast(ConfirmedPresence, SoftIvory) >= 4.5)
        assertTrue(contrast(MayContain, SoftIvory) >= 4.5)
        assertTrue(contrast(CriticalRisk, SoftIvory) >= 4.5)
        assertTrue(contrast(NightBackground, BronzeLight) >= 4.5)
        assertTrue(contrast(NightBackground, GoldLight) >= 4.5)
        assertTrue(contrast(NightBackground, AmberLight) >= 4.5)
        assertTrue(contrast(NightBackground, BlueLight) >= 4.5)
        assertTrue(contrast(NightBackground, RedLight) >= 4.5)
    }

    private fun contrast(first: Color, second: Color): Double {
        val lighter = maxOf(first.relativeLuminance(), second.relativeLuminance())
        val darker = minOf(first.relativeLuminance(), second.relativeLuminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun Color.relativeLuminance(): Double {
        fun channel(value: Float): Double = if (value <= 0.04045f) {
            value.toDouble() / 12.92
        } else {
            Math.pow((value.toDouble() + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
}
