package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogEntry
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogInformationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientLibraryScreen(
    state: IngredientLibraryUiState,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onSelectIngredient: (IngredientCatalogEntry) -> Unit,
    onShowIngredientInfo: (IngredientCatalogEntry) -> Unit,
    onDismissIngredientInfo: () -> Unit,
    onRetryIngredientInfo: () -> Unit,
    onAddManualIngredient: () -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Biblioteca de ingredientes",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Encuentra el ingrediente exacto",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Busca en la biblioteca verificada antes de crear un ingrediente manual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Buscar ingrediente") },
                supportingText = { Text("Nombre o alias. Sin coincidencias aproximadas.") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .testTag("ingredient_library_search"),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("Todas") },
                    shape = MaterialTheme.shapes.large,
                    colors = libraryFilterChipColors(),
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onSelectCategory(category.id) },
                        label = { Text(category.name) },
                        modifier = Modifier.testTag("ingredient_category_${category.id}"),
                        shape = MaterialTheme.shapes.large,
                        colors = libraryFilterChipColors(),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onAddManualIngredient,
                    modifier = Modifier.testTag("ingredient_library_manual"),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Crear ingrediente nuevo")
                }
                if (state.hasActiveSearch) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.testTag("ingredient_library_clear"),
                    ) {
                        Text("Limpiar")
                    }
                }
            }

            if (state.isSearching) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }

            when {
                state.isLoading -> LibraryCenteredState {
                    CircularProgressIndicator()
                }

                state.errorMessage != null -> LibraryCenteredState {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }

                !state.hasActiveSearch && state.frequentIngredients.isNotEmpty() -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ingredient_library_frequent"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
                ) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            Text(
                                text = "Frecuentes",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Ingredientes presentes en varias de tus recetas guardadas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(state.frequentIngredients, key = { it.ingredient.id }) { frequent ->
                        IngredientCatalogRow(
                            ingredient = frequent.ingredient,
                            usageText = if (frequent.recipeCount == 1) {
                                "Usado en 1 receta"
                            } else {
                                "Usado en ${frequent.recipeCount} recetas"
                            },
                            onClick = { onSelectIngredient(frequent.ingredient) },
                            onShowInfo = { onShowIngredientInfo(frequent.ingredient) },
                        )
                    }
                }

                !state.hasActiveSearch -> LibraryCenteredState {
                    LibraryStateBadge("B")
                    Text(
                        text = "Busca un ingrediente o elige una categoría.",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "La biblioteca utiliza identidades verificadas y no asigna ingredientes por similitud aproximada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                !state.isSearching && state.results.isEmpty() -> LibraryCenteredState {
                    LibraryStateBadge("?")
                    Text(
                        text = "No se han encontrado ingredientes con esos criterios.",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Puedes cambiar la búsqueda o crear un ingrediente nuevo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ingredient_library_results"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
                ) {
                    items(state.results, key = { it.id }) { ingredient ->
                        IngredientCatalogRow(
                            ingredient = ingredient,
                            onClick = { onSelectIngredient(ingredient) },
                            onShowInfo = { onShowIngredientInfo(ingredient) },
                        )
                    }
                }
            }
        }
    }

    state.ingredientInfo?.let { infoState ->
        IngredientLibraryInfoDialog(
            state = infoState,
            onDismiss = onDismissIngredientInfo,
            onRetry = onRetryIngredientInfo,
        )
    }
}

@Composable
private fun libraryFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun LibraryCenteredState(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun LibraryStateBadge(text: String) {
    Surface(
        modifier = Modifier.size(54.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun IngredientCatalogRow(
    ingredient: IngredientCatalogEntry,
    usageText: String? = null,
    onClick: () -> Unit,
    onShowInfo: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("ingredient_result_${ingredient.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = ingredient.canonicalName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = ingredient.categoryName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            usageText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ingredient.defaultUnit?.takeIf { it.isNotBlank() }?.let { unit ->
                Text(
                    text = "Unidad habitual: $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IngredientInformationStatus(ingredient)
            TextButton(
                onClick = onShowInfo,
                modifier = Modifier.testTag("ingredient_info_${ingredient.id}"),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text("Ver información")
            }
        }
    }
}

@Composable
private fun IngredientInformationStatus(ingredient: IngredientCatalogEntry) {
    val status = ingredient.informationStatus
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("ingredient_status_${ingredient.id}"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = status.statusSymbol(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics {
                    contentDescription = status.symbolAccessibilityLabel()
                },
            )
            Text(
                text = status.displayText(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun IngredientCatalogInformationStatus.statusSymbol(): String = when (this) {
    IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED -> "ⓘ"
    IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED -> "ⓘ"
    IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED -> "⚠"
}

private fun IngredientCatalogInformationStatus.symbolAccessibilityLabel(): String = when (this) {
    IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED ->
        "Información de seguridad disponible"
    IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED ->
        "Información regulatoria disponible"
    IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED ->
        "Aviso: información posiblemente incompleta"
}

private fun IngredientCatalogInformationStatus.displayText(): String = when (this) {
    IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED ->
        "Información de seguridad registrada"
    IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED ->
        "Información regulatoria específica"
    IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED ->
        "La información disponible puede ser incompleta"
}
