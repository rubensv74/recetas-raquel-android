package com.rmm.recetasraquel.data.repository

import com.rmm.recetasraquel.data.catalog.CatalogImportResult
import com.rmm.recetasraquel.data.catalog.CatalogImporter
import com.rmm.recetasraquel.domain.repository.IngredientCatalogImportSummary
import com.rmm.recetasraquel.domain.repository.IngredientCatalogRepository

class LocalIngredientCatalogRepository(
    private val importer: CatalogImporter,
) : IngredientCatalogRepository {
    override suspend fun ensureCatalogImported(): Result<IngredientCatalogImportSummary> = runCatching {
        when (val result = importer.ensureImported()) {
            is CatalogImportResult.Imported -> IngredientCatalogImportSummary(
                catalogVersion = result.catalogVersion,
                status = IngredientCatalogImportSummary.Status.IMPORTED,
            )
            is CatalogImportResult.AlreadyCurrent -> IngredientCatalogImportSummary(
                catalogVersion = result.catalogVersion,
                status = IngredientCatalogImportSummary.Status.ALREADY_CURRENT,
            )
            is CatalogImportResult.DatabaseNewer -> IngredientCatalogImportSummary(
                catalogVersion = result.databaseVersion,
                status = IngredientCatalogImportSummary.Status.DATABASE_NEWER,
            )
        }
    }
}
