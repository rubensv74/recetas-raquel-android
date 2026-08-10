package com.rmm.recetasraquel.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rmm.recetasraquel.data.local.entity.CatalogIngredientEntity
import com.rmm.recetasraquel.data.local.entity.FoodSafetyGroupEntity
import com.rmm.recetasraquel.data.local.entity.IngredientCategoryEntity
import com.rmm.recetasraquel.data.local.entity.RegulatoryExemptionEntity
import com.rmm.recetasraquel.data.local.entity.SafetySourceEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegulatoryExemptionApplicabilityDaoTest {
    private lateinit var context: Context
    private lateinit var database: RecipeDatabase

    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun queryReturnsOnlyActiveExemptionsApplicableToJurisdictionAndDate() = runBlocking {
        val dao = database.ingredientCatalogDao()
        dao.upsertCategories(listOf(category()))
        dao.upsertSafetyGroups(
            listOf(
                safetyGroup("sg-valid"),
                safetyGroup("sg-foreign"),
                safetyGroup("sg-future"),
                safetyGroup("sg-expired"),
                safetyGroup("sg-inactive"),
            ),
        )
        dao.upsertSafetySources(listOf(source()))
        dao.upsertIngredients(listOf(ingredient()))
        dao.insertRegulatoryExemptions(
            listOf(
                exemption(
                    id = "valid",
                    safetyGroupId = "sg-valid",
                    jurisdiction = "EU-ES",
                    effectiveFrom = "2025-04-01",
                    effectiveTo = null,
                ),
                exemption(
                    id = "foreign",
                    safetyGroupId = "sg-foreign",
                    jurisdiction = "US",
                    effectiveFrom = null,
                    effectiveTo = null,
                ),
                exemption(
                    id = "future",
                    safetyGroupId = "sg-future",
                    jurisdiction = "EU-ES",
                    effectiveFrom = "2027-01-01",
                    effectiveTo = null,
                ),
                exemption(
                    id = "expired",
                    safetyGroupId = "sg-expired",
                    jurisdiction = "EU-ES",
                    effectiveFrom = null,
                    effectiveTo = "2025-12-31",
                ),
                exemption(
                    id = "inactive",
                    safetyGroupId = "sg-inactive",
                    jurisdiction = "EU-ES",
                    effectiveFrom = null,
                    effectiveTo = null,
                    isActive = false,
                ),
            ),
        )

        val applicable = dao.getApplicableRegulatoryExemptionsForIngredient(
            ingredientId = INGREDIENT_ID,
            jurisdiction = "EU-ES",
            asOfDate = "2026-08-10",
        )

        assertEquals(listOf("valid"), applicable.map { it.id })
    }

    private fun category() = IngredientCategoryEntity(
        id = CATEGORY_ID,
        code = "TEST",
        name = "Test",
        sortOrder = 0,
        iconKey = null,
    )

    private fun safetyGroup(id: String) = FoodSafetyGroupEntity(
        id = id,
        code = id.uppercase(),
        displayName = id,
        conditionType = "ALLERGY",
        regulatoryStatus = "TEST",
        jurisdiction = "EU-ES",
        description = null,
    )

    private fun source() = SafetySourceEntity(
        id = SOURCE_ID,
        organization = "Test",
        title = "Test source",
        officialReference = "TEST-REF",
        jurisdiction = "EU-ES",
        publicationDate = null,
        reviewDate = "2026-08-10",
        documentStatus = "TEST",
        officialUrl = null,
    )

    private fun ingredient() = CatalogIngredientEntity(
        id = INGREDIENT_ID,
        canonicalName = "Ingrediente regulado",
        normalizedName = "ingrediente regulado",
        categoryId = CATEGORY_ID,
        defaultUnit = null,
        description = null,
        catalogVersion = 10,
        verificationStatus = "REVIEWED",
        compositionVariability = "LOW",
        sourceUpdatedAt = null,
    )

    private fun exemption(
        id: String,
        safetyGroupId: String,
        jurisdiction: String,
        effectiveFrom: String?,
        effectiveTo: String?,
        isActive: Boolean = true,
    ) = RegulatoryExemptionEntity(
        id = id,
        ingredientId = INGREDIENT_ID,
        safetyGroupId = safetyGroupId,
        jurisdiction = jurisdiction,
        regulatoryEffect = "EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION",
        conditions = "Condición regulatoria de prueba",
        sourceId = SOURCE_ID,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        reviewedAt = "2026-08-10",
        notes = null,
        isActive = isActive,
    )

    private companion object {
        const val CATEGORY_ID = "cat-test"
        const val INGREDIENT_ID = "ing-test"
        const val SOURCE_ID = "source-test"
    }
}
