package com.rmm.recetasraquel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rmm.recetasraquel.app.RecetasRaquelApp
import com.rmm.recetasraquel.ui.theme.RecetasRaquelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RecetasRaquelApplication).container
        setContent {
            RecetasRaquelTheme {
                RecetasRaquelApp(
                    repository = container.recipeRepository,
                    ingredientCatalogRepository = container.ingredientCatalogRepository,
                    customIngredientRepository = container.customIngredientRepository,
                    idGenerator = container.idGenerator,
                    photoStorage = container.photoStorage,
                    saveRecipeUseCase = container.saveRecipeUseCase,
                    demoDataController = container.demoDataController,
                )
            }
        }
    }
}
