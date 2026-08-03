package com.rmm.recetasraquel.ui

import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.ui.components.formatIngredient
import com.rmm.recetasraquel.ui.components.formatTotalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipePresentationTest {
    @Test
    fun totalTimeHandlesAllNullableCombinations() {
        assertEquals("30 min", formatTotalTime(10, 20))
        assertEquals("10 min", formatTotalTime(10, null))
        assertEquals("20 min", formatTotalTime(null, 20))
        assertNull(formatTotalTime(null, null))
    }

    @Test
    fun ingredientPreservesNumericAndNonNumericAmounts() {
        assertEquals("250 g · Harina", formatIngredient(ingredient("250", "g", "Harina")))
        assertEquals("al gusto · Sal", formatIngredient(ingredient("al gusto", null, "Sal")))
        assertEquals("Leche", formatIngredient(ingredient(null, null, "Leche")))
    }

    private fun ingredient(quantity: String?, unit: String?, name: String) =
        Ingredient("i", "r", quantity, unit, name, null, 0)
}
