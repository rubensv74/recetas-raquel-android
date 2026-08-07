package com.rmm.recetasraquel.domain.usecase

import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.domain.model.RecipeDraft
import com.rmm.recetasraquel.domain.model.RecipeStep
import com.rmm.recetasraquel.domain.model.RecipeStepDraft
import com.rmm.recetasraquel.domain.model.RecipeCatalogFilter
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.domain.photos.PhotoDestination
import com.rmm.recetasraquel.domain.photos.RecipePhotoStorage
import com.rmm.recetasraquel.domain.photos.StagedPhoto
import com.rmm.recetasraquel.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SaveRecipeUseCaseTest {

    private val stagedPhoto = StagedPhoto(File("staging/photo.jpg"), "recipe_photos/staging/photo.jpg")
    private val stepStagedPhoto = StagedPhoto(File("staging/step.jpg"), "recipe_photos/staging/step.jpg")

    private fun baseDraft(name: String = "Test Recipe") = RecipeDraft(
        name = name,
        description = "A test recipe",
        category = "Test",
    )

    private fun draftWithCover(name: String = "Test Recipe") = baseDraft(name).copy(
        coverPhotoPath = null,
    )

    private fun draftWithSteps(name: String = "Test Recipe") = baseDraft(name).copy(
        steps = listOf(
            RecipeStepDraft(id = "step-1", stepKey = "key-1", instruction = "Step 1"),
            RecipeStepDraft(id = "step-2", stepKey = "key-2", instruction = "Step 2"),
        ),
    )

    // ── 1. Create without photos ──────────────────────────────

    @Test
    fun createWithoutPhotosPersistsInSingleWrite() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(draft = baseDraft().copy(id = "recipe-1"))
        val result = useCase.create(input)

        assertTrue(result.isSuccess)
        assertEquals("recipe-1", result.getOrThrow())
        assertEquals(1, repo.createdDrafts.size)
        assertTrue(photoStorage.promoted.isEmpty())
        assertTrue(photoStorage.deleted.isEmpty())
    }

    // ── 2. Create with cover photo ────────────────────────────

    @Test
    fun createWithCoverPhotoPromotesAndPersists() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = draftWithCover().copy(id = "recipe-2"),
            stagedCover = stagedPhoto,
        )
        val result = useCase.create(input)

        assertTrue(result.isSuccess)
        assertEquals(1, photoStorage.promoted.size)
        assertTrue(photoStorage.promoted[0].second is PhotoDestination.Cover)
        assertEquals(1, repo.createdDrafts.size)
        val draft = repo.createdDrafts[0]
        assertNotNull(draft.coverPhotoPath)
        assertTrue(draft.coverPhotoPath!!.startsWith("recipe_photos/recipe-2/"))
    }

    // ── 3. Create with step photos ────────────────────────────

    @Test
    fun createWithStepPhotosPromotesAndPersists() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = draftWithSteps().copy(id = "recipe-3"),
            stagedSteps = mapOf("key-1" to stepStagedPhoto, "key-2" to null),
        )
        val result = useCase.create(input)

        assertTrue(result.isSuccess)
        assertEquals(1, photoStorage.promoted.size)
        assertTrue(photoStorage.promoted[0].second is PhotoDestination.Step)
        assertEquals(1, repo.createdDrafts.size)
        val draft = repo.createdDrafts[0]
        assertEquals(2, draft.steps.size)
        assertNotNull(draft.steps[0].photoPath)
        assertNull(draft.steps[1].photoPath)
    }

    // ── 4. Create with both cover and step photos ─────────────

    @Test
    fun createWithBothCoverAndStepPhotosPromotesAll() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = draftWithSteps().copy(id = "recipe-4"),
            stagedCover = stagedPhoto,
            stagedSteps = mapOf("key-1" to stepStagedPhoto),
        )
        val result = useCase.create(input)

        assertTrue(result.isSuccess)
        assertEquals(2, photoStorage.promoted.size)
        assertEquals(1, repo.createdDrafts.size)
    }

    // ── 5. Create Room failure cleans promoted files ───────────

    @Test
    fun createRoomFailureCleansPromotedFiles() = runTest {
        val repo = FakeRepo(failCreate = true)
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = draftWithCover().copy(id = "recipe-5"),
            stagedCover = stagedPhoto,
        )
        val result = useCase.create(input)

        assertTrue(result.isFailure)
        assertEquals(1, photoStorage.promoted.size)
        assertEquals(1, photoStorage.deleted.size)
        assertTrue(photoStorage.deleted[0].startsWith("recipe_photos/recipe-5/"))
    }

    // ── 6. Create promote failure returns error ────────────────

    @Test
    fun createPromoteFailureReturnsError() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore(failPromote = true)
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = draftWithCover().copy(id = "recipe-6"),
            stagedCover = stagedPhoto,
        )
        val result = useCase.create(input)

        assertTrue(result.isFailure)
        assertEquals(0, repo.createdDrafts.size)
        assertTrue(photoStorage.deleted.isEmpty())
    }

    // ── 7. Update with new cover promotes and deletes old ─────

    @Test
    fun updateWithNewCoverPromotesAndDeletesOld() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = baseDraft().copy(id = "recipe-7", coverPhotoPath = "recipe_photos/recipe-7/new_cover.jpg"),
            stagedCover = stagedPhoto,
            replacedCoverPath = "recipe_photos/recipe-7/old_cover.jpg",
        )
        val result = useCase.update("recipe-7", input)

        assertTrue(result.isSuccess)
        assertEquals(1, photoStorage.promoted.size)
        assertTrue(photoStorage.deleted.contains("recipe_photos/recipe-7/old_cover.jpg"))
        assertEquals(1, repo.updateCalls.size)
    }

    // ── 8. Update Room failure preserves old files ─────────────

    @Test
    fun updateRoomFailurePreservesOldFilesAndDeletesPromoted() = runTest {
        val repo = FakeRepo(failUpdate = true)
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = baseDraft().copy(id = "recipe-8"),
            stagedCover = stagedPhoto,
            replacedCoverPath = "recipe_photos/recipe-8/old_cover.jpg",
        )
        val result = useCase.update("recipe-8", input)

        assertTrue(result.isFailure)
        assertFalse(photoStorage.deleted.contains("recipe_photos/recipe-8/old_cover.jpg"))
        assertEquals(1, photoStorage.deleted.size)
        assertTrue(photoStorage.deleted[0].startsWith("recipe_photos/recipe-8/cover_"))
    }

    // ── 9. Update promote failure preserves old files ──────────

    @Test
    fun updatePromoteFailurePreservesOldFiles() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore(failPromote = true)
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = baseDraft().copy(id = "recipe-9"),
            stagedCover = stagedPhoto,
            replacedCoverPath = "recipe_photos/recipe-9/old_cover.jpg",
        )
        val result = useCase.update("recipe-9", input)

        assertTrue(result.isFailure)
        assertFalse(photoStorage.deleted.contains("recipe_photos/recipe-9/old_cover.jpg"))
        assertEquals(0, repo.updateCalls.size)
    }

    // ── 10. Unmodified photo keeps exact path ──────────────────

    @Test
    fun unmodifiedPhotoKeepsExactPath() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = baseDraft().copy(
                id = "recipe-10",
                coverPhotoPath = "recipe_photos/recipe-10/cover_unchanged.jpg",
            ),
        )
        val result = useCase.update("recipe-10", input)

        assertTrue(result.isSuccess)
        assertTrue(photoStorage.promoted.isEmpty())
        assertTrue(photoStorage.deleted.isEmpty())
        val draft = repo.updatedDrafts["recipe-10"]
        assertNotNull(draft)
        assertEquals("recipe_photos/recipe-10/cover_unchanged.jpg", draft!!.coverPhotoPath)
    }

    // ── 11. Removed photo persists null and deletes old ────────

    @Test
    fun removedPhotoPersistsNullAndDeletesOld() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(
            draft = baseDraft().copy(id = "recipe-11", coverPhotoPath = null),
            replacedCoverPath = "recipe_photos/recipe-11/old_cover.jpg",
        )
        val result = useCase.update("recipe-11", input)

        assertTrue(result.isSuccess)
        assertTrue(photoStorage.promoted.isEmpty())
        assertTrue(photoStorage.deleted.contains("recipe_photos/recipe-11/old_cover.jpg"))
        val draft = repo.updatedDrafts["recipe-11"]
        assertNotNull(draft)
        assertEquals(null, draft!!.coverPhotoPath)
    }

    // ── 12. Pre-generated recipe ID used from draft ────────────

    @Test
    fun preGeneratedRecipeIdUsedFromDraft() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(draft = baseDraft().copy(id = "pre-generated-id"))
        val result = useCase.create(input)

        assertTrue(result.isSuccess)
        assertEquals("pre-generated-id", result.getOrThrow())
        assertEquals(1, repo.createdDrafts.size)
        assertEquals("pre-generated-id", repo.createdDrafts[0].id)
    }

    @Test
    fun createWithoutIdReturnsFailure() = runTest {
        val repo = FakeRepo()
        val photoStorage = FakePhotoStore()
        val useCase = SaveRecipeUseCase(repo, photoStorage)

        val input = SaveRecipeInput(draft = baseDraft())
        val result = useCase.create(input)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }

    private class FakeRepo(
        private val failCreate: Boolean = false,
        private val failUpdate: Boolean = false,
    ) : RecipeRepository {
        val createdDrafts = mutableListOf<RecipeDraft>()
        val updatedDrafts = mutableMapOf<String, RecipeDraft>()
        val updateCalls = mutableListOf<String>()

        override fun observeRecipes(): Flow<List<Recipe>> = flow { emit(emptyList()) }
        override fun observeCatalog(filter: RecipeCatalogFilter): Flow<List<RecipeSummary>> = flow { emit(emptyList()) }
        override fun observeCategories(): Flow<List<String>> = flow { emit(emptyList()) }
        override fun observeRecipe(recipeId: String): Flow<Recipe?> = flow { emit(null) }
        override suspend fun getRecipe(recipeId: String): Recipe? = null

        override suspend fun createRecipe(input: RecipeDraft): Result<String> {
            if (failCreate) return Result.failure(IllegalStateException("Room failure"))
            createdDrafts.add(input)
            return Result.success(input.id ?: "generated-id")
        }

        override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = Result.success(Unit)

        override suspend fun updateRecipeFromDraft(recipeId: String, draft: RecipeDraft): Result<Unit> {
            if (failUpdate) return Result.failure(IllegalStateException("Room failure"))
            updateCalls.add(recipeId)
            updatedDrafts[recipeId] = draft
            return Result.success(Unit)
        }

        override suspend fun deleteRecipe(recipeId: String): Result<Unit> = Result.success(Unit)
        override suspend fun setFavorite(recipeId: String, isFavorite: Boolean): Result<Unit> = Result.success(Unit)
    }

    private class FakePhotoStore(
        private val failPromote: Boolean = false,
    ) : RecipePhotoStorage {
        val staged = mutableListOf<StagedPhoto>()
        val promoted = mutableListOf<Pair<StagedPhoto, PhotoDestination>>()
        val deleted = mutableListOf<String>()

        override suspend fun stagePhoto(sourceUriString: String): Result<StagedPhoto> {
            val staged = StagedPhoto(File("fake"), "fake/path.jpg")
            this.staged.add(staged)
            return Result.success(staged)
        }

        override suspend fun promotePhoto(stagedPhoto: StagedPhoto, destination: PhotoDestination): Result<String> {
            if (failPromote) return Result.failure(IllegalStateException("Promote failure"))
            promoted.add(stagedPhoto to destination)
            val path = when (destination) {
                is PhotoDestination.Cover -> "recipe_photos/${destination.recipeId}/cover_new.jpg"
                is PhotoDestination.Step -> "recipe_photos/${destination.recipeId}/steps/step_${destination.stepId}_new.jpg"
            }
            return Result.success(path)
        }

        override suspend fun delete(relativePath: String): Result<Unit> {
            deleted.add(relativePath)
            return Result.success(Unit)
        }

        override suspend fun deleteStaged(stagedPhoto: StagedPhoto): Result<Unit> = Result.success(Unit)
        override suspend fun resolve(relativePath: String): File? = null
        override suspend fun cleanStaging(): Result<Unit> = Result.success(Unit)
        override suspend fun getRecipePhotoPaths(recipeId: String): List<String> = emptyList()
    }
}
