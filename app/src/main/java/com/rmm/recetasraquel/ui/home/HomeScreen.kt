package com.rmm.recetasraquel.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.rmm.recetasraquel.domain.model.RecipeSummary
import com.rmm.recetasraquel.ui.components.formatTotalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onToggleFavorites: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: (RecipeSummary) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateRecipe: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recetas de Raquel",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.semantics { contentDescription = "Abrir ajustes" },
                    ) {
                        Text("Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRecipe,
                modifier = Modifier.semantics { contentDescription = "Nueva receta" },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            CatalogIntro(state)

            OutlinedTextField(
                value = state.filter.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("catalog_search"),
                label = { Text("Buscar por receta o ingrediente") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                trailingIcon = if (state.filter.query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.semantics { contentDescription = "Limpiar búsqueda" },
                        ) {
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                } else {
                    null
                },
            )

            CatalogFilters(
                state = state,
                onToggleFavorites = onToggleFavorites,
                onSelectCategory = onSelectCategory,
            )

            CatalogContent(
                state = state,
                onClearFilters = onClearFilters,
                onRetry = onRetry,
                onToggleFavorite = onToggleFavorite,
                onOpenRecipe = onOpenRecipe,
            )
        }
    }
}

@Composable
private fun CatalogIntro(state: CatalogUiState) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Mi recetario",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (state.hasAnyRecipes) {
                "Todo lo que te gusta cocinar, organizado y siempre a mano."
            } else {
                "Tu colección de recetas empieza aquí."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CatalogFilters(
    state: CatalogUiState,
    onToggleFavorites: () -> Unit,
    onSelectCategory: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.filter.favoritesOnly,
            onClick = onToggleFavorites,
            label = { Text("Favoritas") },
            modifier = Modifier.testTag("filter_favorites"),
            shape = MaterialTheme.shapes.large,
            colors = premiumFilterChipColors(),
        )
        FilterChip(
            selected = state.filter.category == null,
            onClick = { onSelectCategory(null) },
            label = { Text("Todas") },
            shape = MaterialTheme.shapes.large,
            colors = premiumFilterChipColors(),
        )
        state.categories.forEach { category ->
            FilterChip(
                selected = state.filter.category == category,
                onClick = { onSelectCategory(category) },
                label = { Text(category) },
                modifier = Modifier.testTag("category_$category"),
                shape = MaterialTheme.shapes.large,
                colors = premiumFilterChipColors(),
            )
        }
    }
}

@Composable
private fun premiumFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun CatalogContent(
    state: CatalogUiState,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: (RecipeSummary) -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    when {
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(Modifier.testTag("catalog_loading"))
        }

        state.errorMessage != null -> CatalogMessage(state.errorMessage) {
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }

        !state.hasAnyRecipes -> CatalogMessage(
            message = "Todavía no hay recetas guardadas.",
            supportingText = "Añade tu primera receta y empieza a construir una colección a tu medida.",
        )

        state.recipes.isEmpty() -> CatalogMessage(
            message = "No se encontraron recetas con estos filtros.",
            supportingText = "Prueba con otra búsqueda o recupera toda la colección.",
        ) {
            Button(
                onClick = onClearFilters,
                modifier = Modifier.semantics { contentDescription = "Limpiar filtros" },
            ) {
                Text("Limpiar filtros")
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("catalog_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Recetas",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "${state.recipes.size} resultados",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        state.actionMessage?.let { message ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                items(
                    items = state.recipes,
                    key = RecipeSummary::id,
                ) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onFavorite = { onToggleFavorite(recipe) },
                        onOpen = { onOpenRecipe(recipe.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogMessage(
    message: String,
    supportingText: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_message")
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "R",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        action?.invoke()
    }
}

@Composable
private fun RecipeCard(
    recipe: RecipeSummary,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_${recipe.id}"),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecipeThumbnail(recipe)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                recipe.category?.takeIf(String::isNotBlank)?.let { category ->
                    Text(
                        text = category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metadata = listOfNotNull(
                    formatTotalTime(recipe.preparationMinutes, recipe.cookingMinutes),
                    recipe.servings?.let { "$it raciones" },
                ).joinToString(" · ")
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = onFavorite,
                modifier = Modifier
                    .semantics {
                        contentDescription = if (recipe.isFavorite) {
                            "Quitar de favoritas"
                        } else {
                            "Marcar como favorita"
                        }
                    }
                    .testTag("favorite_${recipe.id}"),
            ) {
                Text(
                    text = if (recipe.isFavorite) "★" else "☆",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun RecipeThumbnail(recipe: RecipeSummary) {
    if (recipe.coverPhotoPath != null) {
        AsyncImage(
            model = recipe.coverPhotoPath,
            contentDescription = recipe.name,
            modifier = Modifier
                .size(104.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier = Modifier.size(104.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = recipe.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
