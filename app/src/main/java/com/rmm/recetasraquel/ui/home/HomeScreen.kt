package com.rmm.recetasraquel.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
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
        topBar = {
            TopAppBar(
                title = { Text("Recetas de Raquel") },
                actions = {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.semantics { contentDescription = "Abrir ajustes" },
                    ) { Text("Ajustes") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRecipe,
                modifier = Modifier.semantics { contentDescription = "Nueva receta" },
            ) { Text("+") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.filter.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().testTag("catalog_search"),
                label = { Text("Buscar por receta o ingrediente") },
                singleLine = true,
                trailingIcon = if (state.filter.query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.semantics { contentDescription = "Limpiar búsqueda" },
                        ) { Text("×") }
                    }
                } else null,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.favoritesOnly,
                    onClick = onToggleFavorites,
                    label = { Text("Favoritas") },
                    modifier = Modifier.testTag("filter_favorites"),
                )
                FilterChip(
                    selected = state.filter.category == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("Todas") },
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.filter.category == category,
                        onClick = { onSelectCategory(category) },
                        label = { Text(category) },
                        modifier = Modifier.testTag("category_$category"),
                    )
                }
            }
            CatalogContent(state, onClearFilters, onRetry, onToggleFavorite, onOpenRecipe)
        }
    }
}

@Composable
private fun CatalogContent(
    state: CatalogUiState,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: (RecipeSummary) -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.testTag("catalog_loading"))
        }
        state.errorMessage != null -> CatalogMessage(state.errorMessage) {
            Button(onClick = onRetry) { Text("Reintentar") }
        }
        !state.hasAnyRecipes -> CatalogMessage("Todavía no hay recetas guardadas.")
        state.recipes.isEmpty() -> CatalogMessage("No se encontraron recetas con estos filtros.") {
            Button(
                onClick = onClearFilters,
                modifier = Modifier.semantics { contentDescription = "Limpiar filtros" },
            ) { Text("Limpiar filtros") }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                Text("${state.recipes.size} resultados", style = MaterialTheme.typography.labelLarge)
                state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("catalog_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    items(state.recipes, key = RecipeSummary::id) { recipe ->
                        RecipeCard(recipe, { onToggleFavorite(recipe) }, { onOpenRecipe(recipe.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogMessage(message: String, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().testTag("catalog_message"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(message)
        action?.invoke()
    }
}

@Composable
private fun RecipeCard(recipe: RecipeSummary, onFavorite: () -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("recipe_${recipe.id}")) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Sin foto", style = MaterialTheme.typography.labelSmall) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                recipe.category?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                val metadata = listOfNotNull(
                    formatTotalTime(recipe.preparationMinutes, recipe.cookingMinutes),
                    recipe.servings?.let { "$it raciones" },
                ).joinToString(" · ")
                if (metadata.isNotEmpty()) Text(metadata, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.semantics {
                    contentDescription = if (recipe.isFavorite) "Quitar de favoritas" else "Marcar como favorita"
                }.testTag("favorite_${recipe.id}"),
            ) { Text(if (recipe.isFavorite) "★" else "☆") }
        }
    }
}
