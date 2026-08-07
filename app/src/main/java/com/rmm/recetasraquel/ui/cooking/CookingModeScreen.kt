package com.rmm.recetasraquel.ui.cooking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.ui.components.formatIngredient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    state: CookingUiState,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onShowIngredients: () -> Unit,
    onHideIngredients: () -> Unit,
    onFinish: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    KeepScreenOn()
    val content = state as? CookingUiState.Content

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo cocina") },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Salir del modo cocina" },
                    ) { Text("Salir") }
                },
                actions = {
                    if (content != null) {
                        TextButton(
                            onClick = onShowIngredients,
                            modifier = Modifier
                                .testTag("cooking_ingredients_button")
                                .semantics { contentDescription = "Ver ingredientes" },
                        ) { Text("Ingredientes") }
                    }
                },
            )
        },
        bottomBar = {
            if (content?.currentStep != null) {
                CookingNavigationBar(
                    state = content,
                    onPreviousStep = onPreviousStep,
                    onNextStep = onNextStep,
                    onFinish = onFinish,
                )
            }
        },
    ) { padding ->
        when (state) {
            CookingUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.testTag("cooking_loading"))
            }

            CookingUiState.NotFound -> CookingMessage(
                message = "La receta ya no está disponible.",
                buttonText = "Volver",
                onClick = onNavigateBack,
                modifier = Modifier.padding(padding).testTag("cooking_not_found"),
            )

            is CookingUiState.Error -> CookingMessage(
                message = state.message,
                buttonText = "Volver",
                onClick = onNavigateBack,
                modifier = Modifier.padding(padding).testTag("cooking_error"),
            )

            is CookingUiState.Content -> CookingContent(
                state = state,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (content?.showIngredients == true) {
        IngredientsSheet(
            ingredients = content.recipe.ingredients.sortedBy { it.sortOrder },
            onDismiss = onHideIngredients,
        )
    }
}

@Composable
private fun CookingContent(
    state: CookingUiState.Content,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("cooking_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text(
                text = state.recipe.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        val step = state.currentStep
        if (step == null) {
            item {
                Text(
                    text = "Esta receta todavía no tiene pasos de preparación.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("cooking_no_steps"),
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paso ${state.currentStepIndex + 1} de ${state.totalSteps}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("cooking_step_counter"),
                    )
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().testTag("cooking_progress"),
                    )
                }
            }

            if (step.photoPath != null) {
                item {
                    AsyncImage(
                        model = step.photoPath,
                        contentDescription = "Foto del paso ${state.currentStepIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(MaterialTheme.shapes.large)
                            .testTag("cooking_step_photo"),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            item {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.headlineMedium,
                    lineHeight = MaterialTheme.typography.headlineMedium.lineHeight,
                    modifier = Modifier.testTag("cooking_instruction"),
                )
            }

            step.timerMinutes?.let { minutes ->
                item {
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Tiempo indicado", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "$minutes min",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag("cooking_timer_hint"),
                            )
                            Text(
                                "El temporizador automático se añadirá en una fase posterior.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookingNavigationBar(
    state: CookingUiState.Content,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPreviousStep,
                enabled = state.canGoPrevious,
                modifier = Modifier.weight(1f).testTag("cooking_previous"),
            ) {
                Text("Anterior")
            }
            Button(
                onClick = if (state.isLastStep) onFinish else onNextStep,
                modifier = Modifier
                    .weight(1f)
                    .testTag(if (state.isLastStep) "cooking_finish" else "cooking_next"),
            ) {
                Text(if (state.isLastStep) "Terminar" else "Siguiente")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientsSheet(
    ingredients: List<Ingredient>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("cooking_ingredients_sheet"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Ingredientes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (ingredients.isEmpty()) {
                Text("Esta receta no tiene ingredientes guardados.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ingredients, key = { it.id }) { ingredient ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(formatIngredient(ingredient), style = MaterialTheme.typography.bodyLarge)
                            ingredient.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag("cooking_ingredients_close"),
            ) {
                Text("Cerrar")
            }
        }
    }
}

@Composable
private fun CookingMessage(
    message: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onClick) { Text(buttonText) }
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val previousValue = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previousValue }
    }
}
