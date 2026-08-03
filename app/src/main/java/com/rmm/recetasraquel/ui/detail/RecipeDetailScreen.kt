package com.rmm.recetasraquel.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmm.recetasraquel.domain.model.Recipe
import com.rmm.recetasraquel.ui.components.formatIngredient
import com.rmm.recetasraquel.ui.components.formatTotalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    state: DetailUiState,
    onNavigateBack: () -> Unit,
    onToggleFavorite: () -> Unit,
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
            is DetailUiState.Content -> RecipeContent(state.recipe, state.actionMessage, Modifier.padding(padding))
        }
    }
}

@Composable
private fun RecipeContent(recipe: Recipe, actionMessage: String?, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("recipe_detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
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
                Column(Modifier.fillMaxWidth()) {
                    Text("$number. ${step.instruction}")
                    step.timerMinutes?.let { Text("$it min", style = MaterialTheme.typography.bodySmall) }
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
