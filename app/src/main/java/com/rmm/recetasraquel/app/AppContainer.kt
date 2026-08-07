package com.rmm.recetasraquel.app

import android.content.Context
import com.rmm.recetasraquel.data.catalog.AndroidAssetCatalogTextSource
import com.rmm.recetasraquel.data.catalog.CatalogImporter
import com.rmm.recetasraquel.data.catalog.IngredientCatalogAssetReader
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.photos.LocalRecipePhotoStorage
import com.rmm.recetasraquel.data.repository.LocalIngredientCatalogRepository
import com.rmm.recetasraquel.data.repository.LocalRecipeRepository
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.repository.DemoDataController
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository
import com.rmm.recetasraquel.domain.repository.RecipeRepository
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
    private val catalogImporter = CatalogImporter(
        reader = IngredientCatalogAssetReader(
            source = AndroidAssetCatalogTextSource(context.assets),
        ),
        dao = database.ingredientCatalogDao(),
        timeProvider = timeProvider,
    )
    val ingredientCatalogRepository: IngredientCatalogRepository = LocalIngredientCatalogRepository(catalogImporter)
    val photoStorage: RecipePhotoStorage = LocalRecipePhotoStorage(context)
    val saveRecipeUseCase: SaveRecipeOperation = SaveRecipeUseCase(recipeRepository, photoStorage)
    val demoDataController: DemoDataController? = DemoDataControllerFactory.create(recipeRepository)
}
