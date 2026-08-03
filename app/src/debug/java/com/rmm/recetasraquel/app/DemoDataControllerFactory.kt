package com.rmm.recetasraquel.app

import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.repository.DemoDataController
import com.rmm.recetasraquel.domain.repository.RecipeRepository

object DemoDataControllerFactory {
    fun create(repository: RecipeRepository): DemoDataController = DebugDemoDataController(repository)
}

private class DebugDemoDataController(
    private val repository: RecipeRepository,
) : DemoDataController {
    override suspend fun load(): Result<Unit> {
        for (recipe in DEMO_RECIPES) {
            if (repository.getRecipe(recipe.id) == null) {
                val result = repository.updateRecipe(recipe)
                if (result.isFailure) return result
            }
        }
        return Result.success(Unit)
    }

    override suspend fun remove(): Result<Unit> {
        for (recipe in DEMO_RECIPES) {
            if (repository.getRecipe(recipe.id) != null) {
                val result = repository.deleteRecipe(recipe.id)
                if (result.isFailure) return result
            }
        }
        return Result.success(Unit)
    }
}

private const val DEMO_TIME = 1_700_000_000_000L

private val DEMO_RECIPES = listOf(
    demoRecipe(
        id = "10000000-0000-4000-8000-000000000001",
        name = "Tortilla de patatas",
        category = "Platos principales",
        description = "Tortilla jugosa de patata y cebolla.",
        servings = 4,
        preparation = 20,
        cooking = 25,
        favorite = true,
        ingredients = listOf("4" to "Patatas", "1" to "Cebolla", "6" to "Huevos", "al gusto" to "Sal"),
        steps = listOf("Cortar las patatas y la cebolla.", "Pochar a fuego medio.", "Mezclar con el huevo y cuajar."),
    ),
    demoRecipe(
        id = "10000000-0000-4000-8000-000000000002",
        name = "Arroz con pollo",
        category = "Platos principales",
        servings = 4,
        preparation = 15,
        cooking = 35,
        ingredients = listOf("300 g" to "Arroz", "cantidad necesaria" to "Caldo de pollo", "500 g" to "Pollo"),
        steps = listOf("Dorar el pollo.", "Añadir el arroz y el caldo.", "Cocer hasta que el arroz esté listo."),
    ),
    demoRecipe(
        id = "10000000-0000-4000-8000-000000000003",
        name = "Crema de calabaza",
        category = "Sopas",
        description = "Crema suave para servir caliente.",
        servings = 3,
        cooking = 30,
        favorite = true,
        ingredients = listOf("1/2 kg" to "Calabaza", "una pizca" to "Nuez moscada", null to "Caldo vegetal"),
        steps = listOf("Trocear la calabaza.", "Cocer con el caldo.", "Triturar y sazonar."),
    ),
    demoRecipe(
        id = "10000000-0000-4000-8000-000000000004",
        name = "Tarta de queso",
        category = "Postres",
        preparation = 20,
        cooking = 45,
        ingredients = listOf("500 g" to "Queso crema", "3" to "Huevos", "200 ml" to "Nata"),
        steps = listOf("Mezclar los ingredientes.", "Verter en el molde.", "Hornear y dejar enfriar."),
    ),
    demoRecipe(
        id = "10000000-0000-4000-8000-000000000005",
        name = "Salsa bechamel",
        category = "Salsas",
        ingredients = listOf("50 g" to "Mantequilla", "50 g" to "Harina", "1/2 l" to "Leche", "al gusto" to "Sal"),
        steps = listOf("Fundir la mantequilla.", "Incorporar la harina.", "Añadir la leche poco a poco."),
    ),
)

private fun demoRecipe(
    id: String,
    name: String,
    category: String,
    description: String? = null,
    servings: Int? = null,
    preparation: Int? = null,
    cooking: Int? = null,
    favorite: Boolean = false,
    ingredients: List<Pair<String?, String>>,
    steps: List<String>,
): Recipe = Recipe(
    id = id,
    name = name,
    description = description,
    category = category,
    servings = servings,
    preparationMinutes = preparation,
    cookingMinutes = cooking,
    notes = null,
    isFavorite = favorite,
    coverPhotoPath = null,
    ingredients = ingredients.mapIndexed { index, (amount, ingredientName) ->
        Ingredient("$id-i$index", id, amount, null, ingredientName, null, index)
    },
    steps = steps.mapIndexed { index, instruction ->
        RecipeStep("$id-s$index", id, instruction, null, null, index)
    },
    createdAt = DEMO_TIME,
    updatedAt = DEMO_TIME,
)
