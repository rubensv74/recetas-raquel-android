package com.rmm.recetasraquel.app

import android.content.Context
import com.rmm.recetasraquel.data.catalog.AndroidAssetCatalogTextSource
import com.rmm.recetasraquel.data.catalog.CatalogImporter
import com.rmm.recetasraquel.data.catalog.IngredientCatalogAssetReader
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.photos.LocalRecipePhotoStorage
import com.rmm.recetasraquel.data.repository.LocalCustomIngredientRepository
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.data.repository.LocalRecipeRepository
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.repository.CustomIngredientRepository
import com.rmm.recetasraquel.domain.repository.DemoDataController
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import com.rmm.recetasraquel.domain.usecase.BuildRecipeSafetySummaryUseCase
import com.rmm.recetasraquel.domain.usecase.RecipeSafetySummaryResolver
import com.rmm.recetasraquel.domain.usecase.SaveRecipeOperation
import com.rmm.recetasraquel.domain.usecase.SaveRecipeUseCase
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.SystemTimeProvider
import com.rmm.recetasraquel.util.TimeProvider
import com.rmm.recetasraquel.util.UuidIdGenerator

class AppContainer(context: Context) {
    val idGenerator: IdGenerator = UuidIdGenerator()
    val timeProvider: TimeProvider = SystemTimeProvider()
    val database: RecipeDatabase = RecipeDatabase.build(context)
    val recipeRepository: RecipeRepository = LocalRecipeRepository(
        dao = database.recipeDao(),
        idGenerator = idGenerator,
        timeProvider = timeProvider,
    )
    private val catalogDao = database.ingredientCatalogDao()
    private val catalogImporter = CatalogImporter(
        reader = IngredientCatalogAssetReader(
            source = AndroidAssetCatalogTextSource(context.assets),
        ),
        dao = catalogDao,
        timeProvider = timeProvider,
    )
    val ingredientCatalogRepository: IngredientCatalogRepository = LocalIngredientCatalogRepository(
        importer = catalogImporter,
        dao = catalogDao,
    )
    val customIngredientRepository: CustomIngredientRepository = LocalCustomIngredientRepository(
        dao = database.customIngredientDao(),
        idGenerator = idGenerator,
        timeProvider = timeProvider,
    )
    val recipeSafetySummaryResolver: RecipeSafetySummaryResolver = BuildRecipeSafetySummaryUseCase(
        catalogRepository = ingredientCatalogRepository,
        customIngredientRepository = customIngredientRepository,
        timeProvider = timeProvider,
    )
    val photoStorage: RecipePhotoStorage = LocalRecipePhotoStorage(context)
    val saveRecipeUseCase: SaveRecipeOperation = SaveRecipeUseCase(recipeRepository, photoStorage)
    val demoDataController: DemoDataController? = DemoDataControllerFactory.create(recipeRepository)
}
