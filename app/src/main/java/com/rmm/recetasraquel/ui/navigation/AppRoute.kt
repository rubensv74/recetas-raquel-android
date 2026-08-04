package com.rmm.recetasraquel.ui.navigation

object AppRoute {
    const val CATALOG = "catalog"
    const val SETTINGS = "settings"
    const val RECIPE_ID = "recipeId"
    const val RECIPE = "recipe/{$RECIPE_ID}"
    const val NEW_RECIPE = "recipe/new"
    const val EDIT_RECIPE = "recipe/{$RECIPE_ID}/edit"

    fun recipe(recipeId: String): String = "recipe/$recipeId"
    fun editRecipe(recipeId: String): String = "recipe/$recipeId/edit"
}
