package com.rmm.recetasraquel.ui.navigation

object AppRoute {
    const val CATALOG = "catalog"
    const val SETTINGS = "settings"
    const val RECIPE_ID = "recipeId"
    const val RECIPE = "recipe/{$RECIPE_ID}"
    const val NEW_RECIPE = "recipe/new"
    const val EDIT_RECIPE = "recipe/{$RECIPE_ID}/edit"
    const val COOK_RECIPE = "recipe/{$RECIPE_ID}/cook"
    const val INGREDIENT_LIBRARY = "ingredient-library"

    const val SELECTED_CATALOG_INGREDIENT_ID = "selectedCatalogIngredientId"
    const val SELECTED_CATALOG_INGREDIENT_NAME = "selectedCatalogIngredientName"
    const val SELECTED_CATALOG_INGREDIENT_UNIT = "selectedCatalogIngredientUnit"
    const val ADD_MANUAL_INGREDIENT = "addManualIngredient"

    fun recipe(recipeId: String): String = "recipe/$recipeId"
    fun editRecipe(recipeId: String): String = "recipe/$recipeId/edit"
    fun cookRecipe(recipeId: String): String = "recipe/$recipeId/cook"
}
