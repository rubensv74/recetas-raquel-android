package com.rmm.recetasraquel.ui.navigation

object AppRoute {
    const val CATALOG = "catalog"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val RECIPE_ID = "recipeId"
    const val RECIPE = "recipe/{$RECIPE_ID}"
    const val NEW_RECIPE = "recipe/new"
    const val EDIT_RECIPE = "recipe/{$RECIPE_ID}/edit"
    const val COOK_RECIPE = "recipe/{$RECIPE_ID}/cook"
    const val INGREDIENT_LIBRARY = "ingredient-library"
    const val CUSTOM_INGREDIENT = "ingredient-custom/new"

    const val SELECTED_CATALOG_INGREDIENT_ID = "selectedCatalogIngredientId"
    const val SELECTED_CATALOG_INGREDIENT_NAME = "selectedCatalogIngredientName"
    const val SELECTED_CATALOG_INGREDIENT_UNIT = "selectedCatalogIngredientUnit"
    const val SELECTED_CUSTOM_INGREDIENT_ID = "selectedCustomIngredientId"
    const val SELECTED_CUSTOM_INGREDIENT_NAME = "selectedCustomIngredientName"
    const val SELECTED_CUSTOM_INGREDIENT_UNIT = "selectedCustomIngredientUnit"

    fun recipe(recipeId: String): String = "recipe/$recipeId"
    fun editRecipe(recipeId: String): String = "recipe/$recipeId/edit"
    fun cookRecipe(recipeId: String): String = "recipe/$recipeId/cook"
}
