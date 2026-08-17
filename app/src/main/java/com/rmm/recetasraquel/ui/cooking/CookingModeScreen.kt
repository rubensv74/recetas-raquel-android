package com.rmm.recetasraquel.ui.cooking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "MODO COCINA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = content?.recipe?.name ?: "Cocinando",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Salir del modo cocina" },
                    ) {
                        Text("Salir")
                    }
                },
                actions = {
                    if (content != null) {
                        TextButton(
                            onClick = onShowIngredients,
                            modifier = Modifier
                                .testTag("cooking_ingredients_button")
                                .semantics { contentDescription = "Ver ingredientes" },
                        ) {
                            Text("Ingredientes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(Modifier.testTag("cooking_loading"))
            }

            CookingUiState.NotFound -> CookingMessage(
                eyebrow = "RECETA NO DISPONIBLE",
                message = "La receta ya no está disponible.",
                supportingText = "Vuelve al recetario para continuar.",
                buttonText = "Volver",
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(padding)
                    .testTag("cooking_not_found"),
            )

            is CookingUiState.Error -> CookingMessage(
                eyebrow = "NO SE PUDO ABRIR",
                message = state.message,
                supportingText = "Sal del modo cocina y vuelve a intentarlo desde la receta.",
                buttonText = "Volver",
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(padding)
                    .testTag("cooking_error"),
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
            .testTag("cooking_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
    ) {
        item {
            CookingRecipeHeader(state)
        }

        val step = state.currentStep
        if (step == null) {
            item {
                EmptyCookingState()
            }
        } else {
            item {
                StepProgress(
                    currentStepIndex = state.currentStepIndex,
                    totalSteps = state.totalSteps,
                    progress = state.progress,
                )
            }

            item {
                CurrentStepCard(
                    stepNumber = state.currentStepIndex + 1,
                    instruction = step.instruction,
                    photoPath = step.photoPath,
                )
            }

            step.timerMinutes?.let { minutes ->
                item {
                    StepTimeCard(minutes = minutes)
                }
            }
        }
    }
}

@Composable
private fun CookingRecipeHeader(state: CookingUiState.Content) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.recipe.category?.takeIf(String::isNotBlank)?.let { category ->
            Text(
                text = category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = state.recipe.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Concéntrate en el paso actual. La pantalla permanecerá encendida mientras cocinas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepProgress(
    currentStepIndex: Int,
    totalSteps: Int,
    progress: Float,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Paso ${currentStepIndex + 1} de $totalSteps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("cooking_step_counter"),
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = "${((progress * 100).toInt()).coerceIn(0, 100)}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .testTag("cooking_progress"),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun CurrentStepCard(
    stepNumber: Int,
    instruction: String,
    photoPath: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (photoPath != null) {
                AsyncImage(
                    model = photoPath,
                    contentDescription = "Foto del paso $stepNumber",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .testTag("cooking_step_photo"),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "AHORA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight,
                    modifier = Modifier.testTag("cooking_instruction"),
                )
            }
        }
    }
}

@Composable
private fun StepTimeCard(minutes: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⏱",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "TIEMPO DEL PASO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$minutes min",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag("cooking_timer_hint"),
                )
                Text(
                    text = "Referencia indicada en la receta.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyCookingState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "PREPARACIÓN PENDIENTE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Esta receta todavía no tiene pasos de preparación.",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("cooking_no_steps"),
            )
            Text(
                text = "Añade los pasos desde la edición de la receta para utilizar el modo cocina.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPreviousStep,
                    enabled = state.canGoPrevious,
                    modifier = Modifier
                        .weight(0.9f)
                        .heightIn(min = 58.dp)
                        .testTag("cooking_previous"),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = "Anterior",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Button(
                    onClick = if (state.isLastStep) onFinish else onNextStep,
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 58.dp)
                        .testTag(if (state.isLastStep) "cooking_finish" else "cooking_next"),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = if (state.isLastStep) "Terminar" else "Siguiente",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "INGREDIENTES DE LA RECETA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Ingredientes",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Consulta la lista sin perder el paso actual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (ingredients.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = "Esta receta no tiene ingredientes guardados.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(ingredients, key = { it.id }) { ingredient ->
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = formatIngredient(ingredient),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                            ingredient.notes?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("cooking_ingredients_close"),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Cerrar")
            }
        }
    }
}

@Composable
private fun CookingMessage(
    eyebrow: String,
    message: String,
    supportingText: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(buttonText)
                }
            }
        }
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
