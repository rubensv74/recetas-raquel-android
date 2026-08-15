package com.rmm.recetasraquel.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEditorOptionsTest {
    @Test
    fun categoriesCoverCommonHomeRecipeFamiliesWithoutDuplicates() {
        val expected = setOf(
            "Postres",
            "Guisos y estofados",
            "Bocadillos, sándwiches y wraps",
            "Pizzas, empanadas y tartas saladas",
            "Helados y postres fríos",
            "Bebidas",
        )

        assertTrue(RecipeEditorOptions.categories.containsAll(expected))
        assertEquals(
            RecipeEditorOptions.categories.size,
            RecipeEditorOptions.categories.distinct().size,
        )
    }
}
