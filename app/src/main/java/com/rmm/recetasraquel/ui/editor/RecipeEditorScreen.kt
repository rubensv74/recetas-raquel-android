package com.rmm.recetasraquel.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    state: RecipeEditorUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onPreparationMinutesChange: (String) -> Unit,
    onCookingMinutesChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddIngredient: () -> Unit,
    onIngredientQuantityChange: (String, String) -> Unit,
    onIngredientUnitChange: (String, String) -> Unit,
    onIngredientNameChange: (String, String) -> Unit,
    onIngredientNotesChange: (String, String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onMoveIngredientUp: (String) -> Unit,
    onMoveIngredientDown: (String) -> Unit,
    onAddStep: () -> Unit,
    onStepInstructionChange: (String, String) -> Unit,
    onStepTimerChange: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onMoveStepUp: (String) -> Unit,
    onMoveStepDown: (String) -> Unit,
    onCoverPhotoSelected: (Uri) -> Unit,
    onRemoveCoverPhoto: () -> Unit,
    onStepPhotoSelected: (String, Uri) -> Unit,
    onRemoveStepPhoto: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onDismissSaveError: () -> Unit,
    onDismissPhotoError: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val title = when (state.mode) {
        is EditorMode.Create -> "Nueva receta"
        is EditorMode.Edit -> "Editar receta"
    }

    var pendingStepKey by remember { mutableStateOf<String?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { onCoverPhotoSelected(it) }
    }

    val stepPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val key = pendingStepKey
        uri?.let { if (key != null) onStepPhotoSelected(key, it) }
        pendingStepKey = null
    }

    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            snackbarHostState.showSnackbar(it)
            onDismissSaveError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Cancelar" },
                    ) { Text("Cancelar") }
                },
                actions = {
                    if (state.mode is EditorMode.Edit) {
                        TextButton(
                            onClick = onDelete,
                            enabled = !state.isSaving && !state.isDeleting,
                            modifier = Modifier.semantics { contentDescription = "Eliminar receta" },
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = onSave,
                        enabled = !state.isSaving && !state.isDeleting,
                        modifier = Modifier.testTag("editor_save").semantics { contentDescription = "Guardar" },
                    ) { Text("Guardar") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator(Modifier.testTag("editor_loading")) }

            state.loadError != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text(state.loadError)
                Button(onClick = onNavigateBack) { Text("Volver") }
            }

            else -> EditorContent(
                state = state,
                onNameChange = onNameChange,
                onCategoryChange = onCategoryChange,
                onDescriptionChange = onDescriptionChange,
                onServingsChange = onServingsChange,
                onPreparationMinutesChange = onPreparationMinutesChange,
                onCookingMinutesChange = onCookingMinutesChange,
                onNotesChange = onNotesChange,
                onAddIngredient = onAddIngredient,
                onIngredientQuantityChange = onIngredientQuantityChange,
                onIngredientUnitChange = onIngredientUnitChange,
                onIngredientNameChange = onIngredientNameChange,
                onIngredientNotesChange = onIngredientNotesChange,
                onRemoveIngredient = onRemoveIngredient,
                onMoveIngredientUp = onMoveIngredientUp,
                onMoveIngredientDown = onMoveIngredientDown,
                onAddStep = onAddStep,
                onStepInstructionChange = onStepInstructionChange,
                onStepTimerChange = onStepTimerChange,
                onRemoveStep = onRemoveStep,
                onMoveStepUp = onMoveStepUp,
                onMoveStepDown = onMoveStepDown,
                onCoverPhotoSelected = { coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemoveCoverPhoto = onRemoveCoverPhoto,
                onStepPhotoSelected = { stepKey ->
                    pendingStepKey = stepKey
                    stepPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveStepPhoto = onRemoveStepPhoto,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Eliminar receta") },
            text = { Text("Esta acción eliminará la receta, sus ingredientes y sus pasos. No se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    enabled = !state.isDeleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.semantics { contentDescription = "Confirmar eliminación" },
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) { Text("Cancelar") }
            },
        )
    }

    if (state.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text("Cambios sin guardar") },
            text = { Text("Tienes cambios sin guardar. ¿Deseas descartarlos?") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDiscard,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDiscard) { Text("Seguir editando") }
            },
        )
    }
}

@Composable
private fun EditorContent(
    state: RecipeEditorUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onPreparationMinutesChange: (String) -> Unit,
    onCookingMinutesChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddIngredient: () -> Unit,
    onIngredientQuantityChange: (String, String) -> Unit,
    onIngredientUnitChange: (String, String) -> Unit,
    onIngredientNameChange: (String, String) -> Unit,
    onIngredientNotesChange: (String, String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onMoveIngredientUp: (String) -> Unit,
    onMoveIngredientDown: (String) -> Unit,
    onAddStep: () -> Unit,
    onStepInstructionChange: (String, String) -> Unit,
    onStepTimerChange: (String, String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onMoveStepUp: (String) -> Unit,
    onMoveStepDown: (String) -> Unit,
    onCoverPhotoSelected: () -> Unit,
    onRemoveCoverPhoto: () -> Unit,
    onStepPhotoSelected: (String) -> Unit,
    onRemoveStepPhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("editor_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item { SectionTitle("Fotografía de la receta") }

        item {
            CoverPhotoSection(
                photoState = state.coverPhotoState,
                onSelect = onCoverPhotoSelected,
                onRemove = onRemoveCoverPhoto,
            )
        }

        item { SectionTitle("Información general") }

        item {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth().testTag("editor_name"),
                label = { Text("Nombre *") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { error -> { Text(error) } },
                singleLine = true,
            )
        }

        item {
            OutlinedTextField(
                value = state.category,
                onValueChange = onCategoryChange,
                modifier = Modifier.fillMaxWidth().testTag("editor_category"),
                label = { Text("Categoría") },
                singleLine = true,
            )
        }

        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth().testTag("editor_description"),
                label = { Text("Descripción") },
                minLines = 2,
                maxLines = 5,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.servings,
                    onValueChange = onServingsChange,
                    modifier = Modifier.weight(1f).testTag("editor_servings"),
                    label = { Text("Raciones") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.preparationMinutes,
                    onValueChange = onPreparationMinutesChange,
                    modifier = Modifier.weight(1f).testTag("editor_prep_time"),
                    label = { Text("Preparación (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.cookingMinutes,
                    onValueChange = onCookingMinutesChange,
                    modifier = Modifier.weight(1f).testTag("editor_cook_time"),
                    label = { Text("Cocción (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth().testTag("editor_notes"),
                label = { Text("Notas") },
                minLines = 2,
                maxLines = 5,
            )
        }

        item { SectionTitle("Ingredientes") }

        items(state.ingredients, key = { it.key }) { item ->
            IngredientEditorRow(
                item = item,
                error = state.ingredientErrors[item.key],
                isFirst = state.ingredients.firstOrNull()?.key == item.key,
                isLast = state.ingredients.lastOrNull()?.key == item.key,
                onQuantityChange = { onIngredientQuantityChange(item.key, it) },
                onUnitChange = { onIngredientUnitChange(item.key, it) },
                onNameChange = { onIngredientNameChange(item.key, it) },
                onNotesChange = { onIngredientNotesChange(item.key, it) },
                onRemove = { onRemoveIngredient(item.key) },
                onMoveUp = { onMoveIngredientUp(item.key) },
                onMoveDown = { onMoveIngredientDown(item.key) },
                modifier = Modifier.testTag("editor_ingredient_${item.key}"),
            )
        }

        item {
            AddItemButton(
                text = "Añadir ingrediente",
                onClick = onAddIngredient,
                modifier = Modifier.testTag("add_ingredient"),
            )
        }

        item { SectionTitle("Preparación") }

        items(state.steps, key = { it.key }) { item ->
            val stepPhoto = state.stepPhotoStates[item.key] ?: EditorPhotoState.None
            StepEditorRow(
                item = item,
                index = state.steps.indexOf(item),
                photoState = stepPhoto,
                error = state.stepErrors[item.key],
                isFirst = state.steps.firstOrNull()?.key == item.key,
                isLast = state.steps.lastOrNull()?.key == item.key,
                onInstructionChange = { onStepInstructionChange(item.key, it) },
                onTimerChange = { onStepTimerChange(item.key, it) },
                onRemove = { onRemoveStep(item.key) },
                onMoveUp = { onMoveStepUp(item.key) },
                onMoveDown = { onMoveStepDown(item.key) },
                onSelectPhoto = { onStepPhotoSelected(item.key) },
                onRemovePhoto = { onRemoveStepPhoto(item.key) },
                modifier = Modifier.testTag("editor_step_${item.key}"),
            )
        }

        item {
            AddItemButton(
                text = "Añadir paso",
                onClick = onAddStep,
                modifier = Modifier.testTag("add_step"),
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun CoverPhotoSection(
    photoState: EditorPhotoState,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (photoState) {
                is EditorPhotoState.None, is EditorPhotoState.Removed -> {
                    OutlinedButton(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth().testTag("select_cover_photo"),
                    ) { Text("Seleccionar foto") }
                }
                is EditorPhotoState.Processing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Procesando...")
                    }
                }
                is EditorPhotoState.Staged, is EditorPhotoState.Persisted -> {
                    val model = when (photoState) {
                        is EditorPhotoState.Staged -> photoState.stagedPhoto.stagedFile
                        is EditorPhotoState.Persisted -> photoState.relativePath
                        else -> null
                    }
                    AsyncImage(
                        model = model,
                        contentDescription = "Fotografía de la receta",
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).testTag("cover_photo_preview"),
                        contentScale = ContentScale.Crop,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSelect, modifier = Modifier.testTag("change_cover_photo")) { Text("Cambiar") }
                        OutlinedButton(onClick = onRemove, modifier = Modifier.testTag("remove_cover_photo")) { Text("Quitar") }
                    }
                }
                is EditorPhotoState.Error -> {
                    Text(photoState.message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onSelect) { Text("Reintentar") }
                }
            }
        }
    }
}

@Composable
private fun IngredientEditorRow(
    item: EditorIngredientItem,
    error: String?,
    isFirst: Boolean,
    isLast: Boolean,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val originLabel = when {
        item.catalogIngredientId != null -> "Ingrediente de biblioteca"
        item.customIngredientId != null -> "Ingrediente personalizado"
        else -> null
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(0.4f),
                    label = { Text("Cantidad") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = item.unit,
                    onValueChange = onUnitChange,
                    modifier = Modifier.weight(0.3f),
                    label = { Text("Unidad") },
                    singleLine = true,
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.semantics { contentDescription = "Eliminar ingrediente" },
                ) { Text("×", style = MaterialTheme.typography.titleMedium) }
            }
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth().testTag("ingredient_name_${item.key}"),
                label = { Text("Ingrediente *") },
                readOnly = originLabel != null,
                isError = error != null,
                supportingText = when {
                    error != null -> { { Text(error) } }
                    originLabel != null -> { { Text(originLabel) } }
                    else -> null
                },
                singleLine = true,
            )
            OutlinedTextField(
                value = item.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observaciones") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.semantics { contentDescription = "Subir ingrediente" },
                ) { Text("↑") }
                TextButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.semantics { contentDescription = "Bajar ingrediente" },
                ) { Text("↓") }
            }
        }
    }
}

@Composable
private fun StepEditorRow(
    item: EditorStepItem,
    index: Int,
    photoState: EditorPhotoState,
    error: String?,
    isFirst: Boolean,
    isLast: Boolean,
    onInstructionChange: (String) -> Unit,
    onTimerChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSelectPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${index + 1}.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.semantics { contentDescription = "Eliminar paso" },
                ) { Text("×", style = MaterialTheme.typography.titleMedium) }
            }
            OutlinedTextField(
                value = item.instruction,
                onValueChange = onInstructionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Instrucción *") },
                isError = error != null,
                supportingText = error?.let { e -> { Text(e) } },
                minLines = 2,
                maxLines = 8,
            )
            OutlinedTextField(
                value = item.timerMinutes,
                onValueChange = onTimerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tiempo (min)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            StepPhotoSection(
                photoState = photoState,
                onSelect = onSelectPhoto,
                onRemove = onRemovePhoto,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.semantics { contentDescription = "Subir paso" },
                ) { Text("↑") }
                TextButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.semantics { contentDescription = "Bajar paso" },
                ) { Text("↓") }
            }
        }
    }
}

@Composable
private fun StepPhotoSection(
    photoState: EditorPhotoState,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    when (photoState) {
        is EditorPhotoState.None, is EditorPhotoState.Removed -> {
            OutlinedButton(onClick = onSelect, modifier = Modifier.testTag("add_step_photo")) {
                Text("Añadir foto")
            }
        }
        is EditorPhotoState.Processing -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Procesando...", style = MaterialTheme.typography.bodySmall)
            }
        }
        is EditorPhotoState.Staged, is EditorPhotoState.Persisted -> {
            val model = when (photoState) {
                is EditorPhotoState.Staged -> photoState.stagedPhoto.stagedFile
                is EditorPhotoState.Persisted -> photoState.relativePath
                else -> null
            }
            AsyncImage(
                model = model,
                contentDescription = "Foto del paso",
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.medium).testTag("step_photo_preview"),
                contentScale = ContentScale.Crop,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSelect, modifier = Modifier.testTag("change_step_photo")) { Text("Cambiar") }
                OutlinedButton(onClick = onRemove, modifier = Modifier.testTag("remove_step_photo")) { Text("Quitar") }
            }
        }
        is EditorPhotoState.Error -> {
            Text(photoState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onSelect) { Text("Reintentar") }
        }
    }
}

@Composable
private fun AddItemButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) { Text(text) }
}

@Composable
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
