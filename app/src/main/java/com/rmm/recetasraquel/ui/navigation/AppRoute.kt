package com.rmm.recetasraquel.ui.navigation

object AppRoute {
    const val CATALOG = "catalog"
    const val SETTINGS = "settings"
    const val RECIPE_ID = "recipeId"
    const val RECIPE = "recipe/{$RECIPE_ID}"

    fun recipe(recipeId: String): String = "recipe/$recipeId"
}
