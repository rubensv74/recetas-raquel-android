package com.rmm.recetasraquel.data.custom

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.catalog.AndroidAssetCatalogTextSource
import com.rmm.recetasraquel.data.catalog.CatalogImporter
import com.rmm.recetasraquel.data.catalog.IngredientCatalogAssetReader
import com.rmm.recetasraquel.data.local.RecipeDatabase
import com.rmm.recetasraquel.data.repository.LocalCustomIngredientRepository
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientDraft
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyDeclaration
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyEvidence
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType
import com.rmm.recetasraquel.util.IdGenerator
import com.rmm.recetasraquel.util.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomIngredientRepositoryTest {
    @Test
    fun createPersistsCustomIdentityAliasesAndUserDeclaredSafetyWithoutCatalogInference() = runBlocking {
        val fixture = fixture()
        try {
            val result = fixture.repository.createIngredient(
                CustomIngredientDraft(
                    name = "Crema de prueba",
                    categoryId = "cat-prepared-compound",
                    defaultUnit = "g",
                    type = CustomIngredientType.COMMERCIAL_PRODUCT,
                    aliases = listOf("Crema especial", "crema especial"),
                    brand = "Marca de prueba",
                    tradeName = "Producto de prueba",
                    compositionKnown = false,
                    labelReadAt = "2026-08-09",
                    notes = "Datos de prueba",
                    safetyDeclarations = listOf(
                        CustomIngredientSafetyDeclaration(
                            safetyGroupId = "sg-eu-milk",
                            relationType = CustomIngredientSafetyRelationType.CONTAINS,
                            evidenceLevel = CustomIngredientSafetyEvidence.USER_DECLARED,
                            sourceDetails = "Declarado manualmente a partir de la etiqueta",
                        ),
                    ),
                ),
            )

            val id = result.getOrThrow()
            val stored = requireNotNull(fixture.repository.getIngredient(id).getOrThrow())
            assertEquals("Crema de prueba", stored.name)
            assertEquals(CustomIngredientType.COMMERCIAL_PRODUCT, stored.type)
            assertFalse(stored.compositionKnown)
            assertEquals(listOf("Crema especial"), stored.aliases)
            assertEquals(1, stored.safetyRelations.size)
            assertEquals(CustomIngredientSafetyEvidence.USER_DECLARED, stored.safetyRelations.single().evidenceLevel)
            assertEquals(LocalCustomIngredientRepository.LOCAL_USER_DECLARED_SOURCE_ID, stored.safetyRelations.single().sourceId)

            assertNull(fixture.database.ingredientCatalogDao().getActiveIngredientSummary(id))
            assertEquals(1, fixture.database.customIngredientDao().countActiveIngredients())
        } finally {
            fixture.database.close()
        }
    }

    @Test
    fun invalidSafetyGroupFailsBeforeLeavingPartialCustomData() = runBlocking {
        val fixture = fixture()
        try {
            val result = fixture.repository.createIngredient(
                CustomIngredientDraft(
                    name = "Ingrediente inválido",
                    type = CustomIngredientType.SIMPLE,
                    compositionKnown = true,
                    safetyDeclarations = listOf(
                        CustomIngredientSafetyDeclaration(
                            safetyGroupId = "sg-does-not-exist",
                            relationType = CustomIngredientSafetyRelationType.UNKNOWN,
                            evidenceLevel = CustomIngredientSafetyEvidence.UNVERIFIED,
                        ),
                    ),
                ),
            )

            assertTrue(result.isFailure)
            assertEquals(0, fixture.database.customIngredientDao().countActiveIngredients())
        } finally {
            fixture.database.close()
        }
    }

    @Test
    fun updatePreservesIdentityAndCreatedAtWhileReplacingDeclaredMetadata() = runBlocking {
        val fixture = fixture()
        try {
            val id = fixture.repository.createIngredient(
                CustomIngredientDraft(
                    name = "Ingrediente inicial",
                    type = CustomIngredientType.SIMPLE,
                    compositionKnown = true,
                    aliases = listOf("Inicial"),
                ),
            ).getOrThrow()
            val before = requireNotNull(fixture.repository.getIngredient(id).getOrThrow())

            fixture.clock += 86_400_000L
            fixture.repository.updateIngredient(
                id,
                CustomIngredientDraft(
                    name = "Ingrediente revisado",
                    type = CustomIngredientType.COMPOUND,
                    compositionKnown = false,
                    aliases = listOf("Revisado"),
                    safetyDeclarations = listOf(
                        CustomIngredientSafetyDeclaration(
                            safetyGroupId = "sg-eu-eggs",
                            relationType = CustomIngredientSafetyRelationType.DECLARED_MAY_CONTAIN,
                            evidenceLevel = CustomIngredientSafetyEvidence.UNVERIFIED,
                        ),
                    ),
                ),
            ).getOrThrow()

            val after = requireNotNull(fixture.repository.getIngredient(id).getOrThrow())
            assertEquals(id, after.id)
            assertEquals(before.createdAt, after.createdAt)
            assertTrue(after.updatedAt > before.updatedAt)
            assertEquals("Ingrediente revisado", after.name)
            assertEquals(listOf("Revisado"), after.aliases)
            assertEquals(CustomIngredientType.COMPOUND, after.type)
            assertEquals(CustomIngredientSafetyEvidence.UNVERIFIED, after.safetyRelations.single().evidenceLevel)
        } finally {
            fixture.database.close()
        }
    }

    @Test
    fun repositoryExposesOnlyUserDeclaredAndUnverifiedEvidenceChoices() {
        assertEquals(
            setOf(CustomIngredientSafetyEvidence.USER_DECLARED, CustomIngredientSafetyEvidence.UNVERIFIED),
            CustomIngredientSafetyEvidence.values().toSet(),
        )
    }

    private suspend fun fixture(): Fixture {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        val importer = CatalogImporter(
            reader = IngredientCatalogAssetReader(AndroidAssetCatalogTextSource(context.assets)),
            dao = database.ingredientCatalogDao(),
            timeProvider = TimeProvider { 1_786_233_600_000L },
        )
        importer.ensureImported()

        var idCounter = 0
        var now = 1_786_233_600_000L
        val repository = LocalCustomIngredientRepository(
            dao = database.customIngredientDao(),
            idGenerator = IdGenerator { "custom-test-${idCounter++}" },
            timeProvider = TimeProvider { now },
        )
        return Fixture(
            database = database,
            repository = repository,
            getClock = { now },
            setClock = { now = it },
        )
    }

    private class Fixture(
        val database: RecipeDatabase,
        val repository: LocalCustomIngredientRepository,
        private val getClock: () -> Long,
        private val setClock: (Long) -> Unit,
    ) {
        var clock: Long
            get() = getClock()
            set(value) = setClock(value)
    }
}
