package com.rmm.recetasraquel.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyGroupSummary
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyObservation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.ui.components.formatIngredient
import com.rmm.recetasraquel.ui.components.formatTotalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    state: DetailUiState,
    onNavigateBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditRecipe: (String) -> Unit = {},
    onStartCooking: (String) -> Unit = {},
) {
    val recipe = (state as? DetailUiState.Content)?.recipe
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "Receta") },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Atrás" },
                    ) { Text("Volver") }
                },
                actions = {
                    recipe?.let {
                        TextButton(
                            onClick = { onEditRecipe(it.id) },
                            modifier = Modifier.semantics { contentDescription = "Editar receta" },
                        ) { Text("Editar") }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.semantics {
                                contentDescription = if (it.isFavorite) "Quitar de favoritas" else "Marcar como favorita"
                            }.testTag("detail_favorite"),
                        ) { Text(if (it.isFavorite) "★" else "☆") }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            DetailUiState.Loading -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(Modifier.testTag("detail_loading")) }
            DetailUiState.NotFound -> DetailMessage(
                "La receta ya no está disponible.", "Volver al catálogo", onNavigateBack,
                Modifier.padding(padding).testTag("detail_not_found"),
            )
            is DetailUiState.Error -> DetailMessage(
                state.message, "Volver al catálogo", onNavigateBack, Modifier.padding(padding),
            )
            is DetailUiState.Content -> RecipeContent(
                recipe = state.recipe,
                safetySummary = state.safetySummary,
                safetyMessage = state.safetyMessage,
                actionMessage = state.actionMessage,
                onStartCooking = onStartCooking,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun RecipeContent(
    recipe: Recipe,
    safetySummary: RecipeSafetySummary?,
    safetyMessage: String?,
    actionMessage: String?,
    onStartCooking: (String) -> Unit,
    modifier: Modifier,
) {
    val regulatoryExemptions = safetySummary?.regulatoryExemptions.orEmpty()
    val ingredientNamesByCatalogId = recipe.ingredients.mapNotNull { ingredient ->
        ingredient.catalogIngredientId?.let { catalogIngredientId -> catalogIngredientId to ingredient.name }
    }.toMap()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("recipe_detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        if (recipe.coverPhotoPath != null) {
            item {
                AsyncImage(
                    model = recipe.coverPhotoPath,
                    contentDescription = recipe.name,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium).testTag("detail_cover_photo"),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recipe.category?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                recipe.description?.let { Text(it) }
                val metadata = listOfNotNull(
                    recipe.servings?.let { "$it raciones" },
                    recipe.preparationMinutes?.let { "Preparación: $it min" },
                    recipe.cookingMinutes?.let { "Cocción: $it min" },
                    formatTotalTime(recipe.preparationMinutes, recipe.cookingMinutes)?.let { "Total: $it" },
                )
                metadata.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (recipe.steps.isNotEmpty()) {
            item {
                Button(
                    onClick = { onStartCooking(recipe.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_start_cooking")
                        .semantics { contentDescription = "Empezar modo cocina" },
                ) {
                    Text("Cocinar")
                }
            }
        }
        if (safetySummary != null || safetyMessage != null) {
            item {
                RecipeSafetyPanel(
                    summary = safetySummary,
                    loadMessage = safetyMessage,
                )
            }
        }
        if (regulatoryExemptions.isNotEmpty()) {
            item {
                RecipeRegulatoryPanel(
                    exemptions = regulatoryExemptions,
                    ingredientNamesByCatalogId = ingredientNamesByCatalogId,
                )
            }
        }
        if (recipe.ingredients.isNotEmpty()) {
            item { SectionTitle("Ingredientes") }
            items(recipe.ingredients.sortedBy { it.sortOrder }, key = { it.id }) { ingredient ->
                Column(Modifier.fillMaxWidth()) {
                    Text(formatIngredient(ingredient))
                    ingredient.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (recipe.steps.isNotEmpty()) {
            item { SectionTitle("Pasos") }
            items(recipe.steps.sortedBy { it.sortOrder }, key = { it.id }) { step ->
                val number = recipe.steps.sortedBy { it.sortOrder }.indexOfFirst { it.id == step.id } + 1
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$number. ${step.instruction}")
                    step.timerMinutes?.let { Text("$it min", style = MaterialTheme.typography.bodySmall) }
                    if (step.photoPath != null) {
                        AsyncImage(
                            model = step.photoPath,
                            contentDescription = "Foto del paso $number",
                            modifier = Modifier.fillMaxWidth().height(140.dp).clip(MaterialTheme.shapes.medium).testTag("step_photo_${step.id}"),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
        recipe.notes?.let {
            item {
                SectionTitle("Notas")
                Text(it)
            }
        }
    }
}

@Composable
private fun RecipeSafetyPanel(
    summary: RecipeSafetySummary?,
    loadMessage: String?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_safety_panel")
            .semantics { contentDescription = "Información sobre seguridad alimentaria" },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "⚠ Información sobre seguridad alimentaria",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (loadMessage != null) {
                Text(loadMessage, style = MaterialTheme.typography.bodyMedium)
            } else if (summary != null) {
                if (summary.groups.isEmpty() && summary.reviewNotices.isEmpty()) {
                    Text(
                        "No se han detectado coincidencias en los datos registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                summary.groups.forEach { group ->
                    SafetyGroupBlock(group)
                }

                if (summary.reviewNotices.isNotEmpty()) {
                    Column(
                        modifier = Modifier.testTag("recipe_safety_review"),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Requiere revisión",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        summary.reviewNotices.forEach { notice ->
                            val ingredientPrefix = notice.ingredientName?.let { "$it: " }.orEmpty()
                            Text(
                                "• $ingredientPrefix${notice.message}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Text(
                "La información disponible puede ser incompleta. Comprueba las etiquetas y la información del fabricante cuando corresponda.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SafetyGroupBlock(group: RecipeSafetyGroupSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_safety_group_${group.safetyGroupId}"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            group.safetyGroupName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            group.presentationState.presentationLabel(),
            style = MaterialTheme.typography.bodyMedium,
        )
        group.observations.forEach { observation ->
            SafetyObservationLine(observation)
        }
    }
}

@Composable
private fun SafetyObservationLine(observation: RecipeSafetyObservation) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "• ${observation.ingredientName}: ${observation.relationType.relationLabel()} · ${observation.evidenceLevel.evidenceLabel()}",
            style = MaterialTheme.typography.bodySmall,
        )
        observation.sourceDetails?.takeIf(String::isNotBlank)?.let { source ->
            Text("Fuente: $source", style = MaterialTheme.typography.bodySmall)
        }
        observation.reviewedAt?.takeIf(String::isNotBlank)?.let { reviewedAt ->
            Text("Revisado: $reviewedAt", style = MaterialTheme.typography.bodySmall)
        }
        observation.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun RecipeSafetyPresentationState.presentationLabel(): String = when (this) {
    RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA -> "Presencia identificada"
    RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO -> "Derivado identificado"
    RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO -> "Puede contener declarado"
    RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA -> "Posible reactividad cruzada"
    RecipeSafetyPresentationState.REQUIERE_REVISION -> "Requiere revisión"
}

private fun RecipeSafetyRelationType.relationLabel(): String = when (this) {
    RecipeSafetyRelationType.INHERENT_SOURCE -> "fuente inherente"
    RecipeSafetyRelationType.CONTAINS -> "contiene"
    RecipeSafetyRelationType.DERIVED_FROM -> "derivado de"
    RecipeSafetyRelationType.REGULATED_COMPONENT -> "componente regulado"
    RecipeSafetyRelationType.DECLARED_MAY_CONTAIN -> "puede contener declarado"
    RecipeSafetyRelationType.POSSIBLE_CROSS_REACTIVITY -> "posible reactividad cruzada"
    RecipeSafetyRelationType.UNKNOWN -> "información no determinada"
}

private fun String.evidenceLabel(): String = when (this) {
    "EU_LEGAL" -> "evidencia normativa UE"
    "OFFICIAL_SCIENTIFIC" -> "evidencia científica oficial"
    "OFFICIAL_HEALTH_AUTHORITY" -> "autoridad sanitaria oficial"
    "MANUFACTURER_LABEL" -> "etiqueta del fabricante"
    "USER_DECLARED" -> "declarado por el usuario"
    "UNVERIFIED" -> "sin verificar"
    else -> "evidencia: $this"
}

@Composable
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailMessage(message: String, button: String, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(message)
        Button(onClick = onClick) { Text(button) }
    }
}