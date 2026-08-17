package com.rmm.recetasraquel.data.repository

import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LegacyCustomIngredientTypeCompatibilityTest {

    @Test
    fun `legacy recipe free text compatibility type projects conservatively to simple`() {
        assertEquals(
            CustomIngredientType.SIMPLE,
            persistedCustomIngredientType("RECIPE_FREE_TEXT_COMPAT"),
        )
    }

    @Test
    fun `current persisted types preserve their domain identity`() {
        assertEquals(
            CustomIngredientType.SIMPLE,
            persistedCustomIngredientType("SIMPLE"),
        )
        assertEquals(
            CustomIngredientType.COMPOUND,
            persistedCustomIngredientType("COMPOUND"),
        )
        assertEquals(
            CustomIngredientType.COMMERCIAL_PRODUCT,
            persistedCustomIngredientType("COMMERCIAL_PRODUCT"),
        )
    }

    @Test
    fun `unknown persisted type remains an explicit data error`() {
        assertThrows(IllegalArgumentException::class.java) {
            persistedCustomIngredientType("UNKNOWN_PERSISTED_TYPE")
        }
    }
}
