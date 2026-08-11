package com.rmm.recetasraquel.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyGroupSummary
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyObservation
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.ui.components.SafetyGroupPictogram
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recipe?.name ?: "Receta",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Atrás" },
                    ) {
                        Text("Volver")
                    }
                },
                actions = {
                    recipe?.let {
                        TextButton(
                            onClick = { onEditRecipe(it.id) },
                            modifier = Modifier.semantics { contentDescription = "Editar receta" },
                        ) {
                            Text("Editar")
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .semantics {
                                    contentDescription = if (it.isFavorite) {
                                        "Quitar de favoritas"
                                    } else {
                                        "Marcar como favorita"
                                    }
                                }
                                .testTag("detail_favorite"),
                        ) {
                            Text(
                                text = if (it.isFavorite) "★" else "☆",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when (state) {
            DetailUiState.Loading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.testTag("detail_loading"))
            }

            DetailUiState.NotFound -> DetailMessage(
                message = "La receta ya no está disponible.",
                button = "Volver al catálogo",
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(padding)
                    .testTag("detail_not_found"),
            )

            is DetailUiState.Error -> DetailMessage(
                message = state.message,
                button = "Volver al catálogo",
                onClick = onNavigateBack,
                modifier = Modifier.padding(padding),
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
        ingredient.catalogIngredientId?.let { catalogIngredientId ->
            catalogIngredientId to ingredient.name
        }
    }.toMap()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recipe_detail"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 36.dp,
        ),
    ) {
        item {
            RecipeHero(recipe)
        }

        item {
            RecipeEditorialHeader(recipe)
        }

        item {
            RecipeMetadataStrip(recipe)
        }

        actionMessage?.let { message ->
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (recipe.steps.isNotEmpty()) {
            item {
                Button(
                    onClick = { onStartCooking(recipe.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("detail_start_cooking")
                        .semantics { contentDescription = "Empezar modo cocina" },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = "Cocinar",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (!safetySummary?.groups.isNullOrEmpty()) {
            item {
                SafetyGroupSummaryRow(safetySummary!!.groups)
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
            item {
                SectionTitle("Ingredientes")
            }
            items(
                items = recipe.ingredients.sortedBy { it.sortOrder },
                key = { it.id },
            ) { ingredient ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatIngredient(ingredient),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    ingredient.notes?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    )
                }
            }
        }

        if (recipe.steps.isNotEmpty()) {
            item {
                SectionTitle("Pasos")
            }
            items(
                items = recipe.steps.sortedBy { it.sortOrder },
                key = { it.id },
            ) { step ->
                val sortedSteps = recipe.steps.sortedBy { it.sortOrder }
                val number = sortedSteps.indexOfFirst { it.id == step.id } + 1
                RecipeStepBlock(number = number, step = step)
            }
        }

        recipe.notes?.let { notes ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Notas")
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = notes,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeHero(recipe: Recipe) {
    if (recipe.coverPhotoPath != null) {
        AsyncImage(
            model = recipe.coverPhotoPath,
            contentDescription = recipe.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .testTag("detail_cover_photo"),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = "Recetas Raquel",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Cocina hecha para disfrutar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun RecipeEditorialHeader(recipe: Recipe) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        recipe.category?.takeIf(String::isNotBlank)?.let { category ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = category.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Text(
            text = recipe.name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        recipe.description?.takeIf(String::isNotBlank)?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecipeMetadataStrip(recipe: Recipe) {
    val metadata = listOfNotNull(
        recipe.servings?.let { "Raciones" to it.toString() },
        recipe.preparationMinutes?.let { "Preparación" to "$it min" },
        recipe.cookingMinutes?.let { "Cocción" to "$it min" },
        formatTotalTime(recipe.preparationMinutes, recipe.cookingMinutes)?.let { "Total" to it },
    )

    if (metadata.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            metadata.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.second,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = item.first,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }

                if (index < metadata.lastIndex) {
                    Surface(
                        modifier = Modifier.size(width = 1.dp, height = 28.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun SafetyGroupSummaryRow(groups: List<RecipeSafetyGroupSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Alérgenos presentes",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(
                items = groups,
                key = { it.safetyGroupId },
            ) { group ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SafetyGroupPictogram(
                            safetyGroupId = group.safetyGroupId,
                            safetyGroupName = group.safetyGroupName,
                        )
                        Text(
                            text = group.safetyGroupName,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
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
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Información sobre seguridad alimentaria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Riesgo, evidencia y recomendaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (loadMessage != null) {
                Text(
                    text = loadMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (summary != null) {
                if (summary.groups.isEmpty() && summary.reviewNotices.isEmpty()) {
                    Text(
                        text = "No se han detectado coincidencias en los datos registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                summary.groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        )
                    }
                    SafetyGroupBlock(group)
                }

                if (summary.reviewNotices.isNotEmpty()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    )
                    Column(
                        modifier = Modifier.testTag("recipe_safety_review"),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = "Requiere revisión",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        summary.reviewNotices.forEach { notice ->
                            val ingredientPrefix = notice.ingredientName?.let { "$it: " }.orEmpty()
                            Text(
                                text = "• $ingredientPrefix${notice.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            )
            Text(
                text = "La información disponible puede ser incompleta. Comprueba las etiquetas y la información del fabricante cuando corresponda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SafetyGroupBlock(group: RecipeSafetyGroupSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_safety_group_${group.safetyGroupId}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SafetyGroupPictogram(
            safetyGroupId = group.safetyGroupId,
            safetyGroupName = group.safetyGroupName,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = group.safetyGroupName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = group.presentationState.presentationLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = presentationStateColor(group.presentationState),
                fontWeight = FontWeight.SemiBold,
            )
            group.observations.forEach { observation ->
                SafetyObservationLine(observation)
            }
        }
    }
}

@Composable
private fun SafetyObservationLine(observation: RecipeSafetyObservation) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "${observation.ingredientName}: ${observation.relationType.relationLabel()} · ${observation.evidenceLevel.evidenceLabel()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        observation.sourceDetails?.takeIf(String::isNotBlank)?.let { source ->
            Text(
                text = "Fuente: $source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        observation.reviewedAt?.takeIf(String::isNotBlank)?.let { reviewedAt ->
            Text(
                text = "Revisado: $reviewedAt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        observation.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun presentationStateColor(state: RecipeSafetyPresentationState) = when (state) {
    RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA -> MaterialTheme.colorScheme.primary
    RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO -> MaterialTheme.colorScheme.tertiary
    RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO -> MaterialTheme.colorScheme.secondary
    RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA -> MaterialTheme.colorScheme.secondary
    RecipeSafetyPresentationState.REQUIERE_REVISION -> MaterialTheme.colorScheme.error
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
private fun RecipeStepBlock(
    number: Int,
    step: com.rmm.recetasraquel.domain.model.RecipeStep,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            step.timerMinutes?.let { minutes ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "$minutes min",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (step.photoPath != null) {
                AsyncImage(
                    model = step.photoPath,
                    contentDescription = "Foto del paso $number",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .clip(MaterialTheme.shapes.large)
                        .testTag("step_photo_${step.id}"),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun DetailMessage(
    message: String,
    button: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onClick) {
            Text(button)
        }
    }
}
