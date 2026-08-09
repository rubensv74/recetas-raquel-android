package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
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
    onAddManualIngredient: () -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca de ingredientes") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("Volver") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Buscar ingrediente") },
                supportingText = { Text("Nombre o alias. Sin coincidencias aproximadas.") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .testTag("ingredient_library_search"),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("Todas") },
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        onClick = { onSelectCategory(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onAddManualIngredient) {
                    Text("Introducir manualmente")
                }
                if (state.hasActiveSearch) {
                    TextButton(onClick = onClearFilters) {
                        Text("Limpiar")
                    }
                }
            }

            if (state.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                state.isLoading -> LibraryCenteredState {
                    CircularProgressIndicator()
                }

                state.errorMessage != null -> LibraryCenteredState {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = onRetry) { Text("Reintentar") }
                }

                !state.hasActiveSearch -> LibraryCenteredState {
                    Text(
                        text = "Busca un ingrediente o elige una categoría.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "La biblioteca utiliza identidades verificadas y no asigna ingredientes por similitud aproximada.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                !state.isSearching && state.results.isEmpty() -> LibraryCenteredState {
                    Text(
                        text = "No se han encontrado ingredientes con esos criterios.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Puedes cambiar la búsqueda o introducir el ingrediente manualmente.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ingredient_library_results"),
                ) {
                    items(state.results, key = { it.id }) { ingredient ->
                        IngredientCatalogRow(
                            ingredient = ingredient,
                            onClick = { onSelectIngredient(ingredient) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryCenteredState(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun IngredientCatalogRow(
    ingredient: IngredientCatalogEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("ingredient_result_${ingredient.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = ingredient.canonicalName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = ingredient.categoryName,
                style = MaterialTheme.typography.bodyMedium,
            )
            ingredient.defaultUnit?.takeIf { it.isNotBlank() }?.let { unit ->
                Text(
                    text = "Unidad habitual: $unit",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = ingredient.informationStatus.displayText(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun IngredientCatalogInformationStatus.displayText(): String = when (this) {
    IngredientCatalogInformationStatus.SAFETY_RELATIONS_RECORDED ->
        "Información de seguridad registrada"
    IngredientCatalogInformationStatus.REGULATORY_EXEMPTION_RECORDED ->
        "Información regulatoria específica"
    IngredientCatalogInformationStatus.NO_DIRECT_SAFETY_RELATION_RECORDED ->
        "La información disponible puede ser incompleta"
}
