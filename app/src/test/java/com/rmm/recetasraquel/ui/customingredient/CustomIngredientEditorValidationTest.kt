package com.rmm.recetasraquel.ui.customingredient

import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomIngredientEditorValidationTest {
    @Test
    fun duplicateGroupAndRelationIsRejected() {
        val rows = listOf(
            CustomIngredientSafetyRow(
                key = "row-1",
                safetyGroupId = "sg-eu-milk",
                relationType = CustomIngredientSafetyRelationType.CONTAINS,
            ),
            CustomIngredientSafetyRow(
                key = "row-2",
                safetyGroupId = "sg-eu-milk",
                relationType = CustomIngredientSafetyRelationType.CONTAINS,
            ),
        )

        assertTrue(hasDuplicateCustomSafetyRelation(rows))
    }

    @Test
    fun sameGroupWithDifferentRelationTypesIsAllowed() {
        val rows = listOf(
            CustomIngredientSafetyRow(
                key = "row-1",
                safetyGroupId = "sg-eu-milk",
                relationType = CustomIngredientSafetyRelationType.CONTAINS,
            ),
            CustomIngredientSafetyRow(
                key = "row-2",
                safetyGroupId = "sg-eu-milk",
                relationType = CustomIngredientSafetyRelationType.DECLARED_MAY_CONTAIN,
            ),
        )

        assertFalse(hasDuplicateCustomSafetyRelation(rows))
    }
}
