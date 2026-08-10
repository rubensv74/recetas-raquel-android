package com.rmm.recetasraquel.ui.customingredient

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyEvidence
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientSafetyRelationType
import com.rmm.recetasraquel.domain.ingredient.CustomIngredientType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIngredientEditorScreen(
    state: CustomIngredientEditorUiState,
    onNameChange: (String) -> Unit,
    onTypeChange: (CustomIngredientType) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDefaultUnitChange: (String) -> Unit,
    onAliasesChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onTradeNameChange: (String) -> Unit,
    onCompositionKnownChange: (Boolean) -> Unit,
    onLabelReadAtChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddSafetyRow: () -> Unit,
    onRemoveSafetyRow: (String) -> Unit,
    onSafetyGroupChange: (String, String) -> Unit,
    onSafetyRelationTypeChange: (String, CustomIngredientSafetyRelationType) -> Unit,
    onSafetyEvidenceChange: (String, CustomIngredientSafetyEvidence) -> Unit,
    onSafetySourceDetailsChange: (String, String) -> Unit,
    onSafetyNotesChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo ingrediente personalizado") },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("Volver") } },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("custom_ingredient_form"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Crea una identidad local cuando el ingrediente no esté en la biblioteca o necesites registrar un producto concreto.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("custom_name"),
                )
            }

            item {
                SectionTitle("Tipo")
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(CustomIngredientType.values().toList()) { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { onTypeChange(type) },
                            label = { Text(type.displayName()) },
                            modifier = Modifier.testTag("custom_type_${type.name}"),
                        )
                    }
                }
            }

            item {
                SectionTitle("Categoría")
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { onCategoryChange(null) },
                            label = { Text("Sin categoría") },
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { onCategoryChange(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.defaultUnit,
                        onValueChange = onDefaultUnitChange,
                        label = { Text("Unidad habitual") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.aliasesText,
                        onValueChange = onAliasesChange,
                        label = { Text("Alias") },
                        supportingText = { Text("Separados por comas") },
                        modifier = Modifier.weight(2f),
                    )
                }
            }

            if (state.type == CustomIngredientType.COMMERCIAL_PRODUCT) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = state.brand,
                            onValueChange = onBrandChange,
                            label = { Text("Marca") },
                            modifier = Modifier.weight(1f).testTag("custom_brand"),
                        )
                        OutlinedTextField(
                            value = state.tradeName,
                            onValueChange = onTradeNameChange,
                            label = { Text("Nombre comercial") },
                            modifier = Modifier.weight(1f).testTag("custom_trade_name"),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.labelReadAt,
                        onValueChange = onLabelReadAtChange,
                        label = { Text("Fecha de lectura de etiqueta") },
                        supportingText = { Text("Opcional · AAAA-MM-DD") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("custom_label_read_at"),
                    )
                }
            }

            item {
                SectionTitle("Composición *")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.compositionKnown == true,
                        onClick = { onCompositionKnownChange(true) },
                        label = { Text("Conocida") },
                        modifier = Modifier.testTag("composition_known"),
                    )
                    FilterChip(
                        selected = state.compositionKnown == false,
                        onClick = { onCompositionKnownChange(false) },
                        label = { Text("Desconocida") },
                        modifier = Modifier.testTag("composition_unknown"),
                    )
                }
                Text(
                    text = "Si la composición es desconocida, la aplicación debe tratar la información disponible como incompleta.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notas") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            item {
                SectionTitle("Información de seguridad alimentaria")
                Text(
                    text = "Registra solo lo que conozcas. La ausencia de declaraciones no significa ausencia de alérgenos o riesgo. Comprueba siempre la etiqueta del producto cuando corresponda.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            items(state.safetyRows, key = { it.key }) { row ->
                SafetyDeclarationCard(
                    row = row,
                    state = state,
                    onRemove = { onRemoveSafetyRow(row.key) },
                    onGroupChange = { onSafetyGroupChange(row.key, it) },
                    onRelationTypeChange = { onSafetyRelationTypeChange(row.key, it) },
                    onEvidenceChange = { onSafetyEvidenceChange(row.key, it) },
                    onSourceDetailsChange = { onSafetySourceDetailsChange(row.key, it) },
                    onNotesChange = { onSafetyNotesChange(row.key, it) },
                )
            }

            item {
                TextButton(
                    onClick = onAddSafetyRow,
                    modifier = Modifier.padding(horizontal = 16.dp).testTag("add_safety_declaration"),
                ) {
                    Text("Añadir declaración de seguridad")
                }
            }

            state.validationMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            state.errorMessage?.let { message ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Recargar datos") }
                    }
                }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("save_custom_ingredient"),
                ) {
                    if (state.isSaving) Text("Guardando…") else Text("Guardar ingrediente")
                }
            }
        }
    }
}

@Composable
private fun SafetyDeclarationCard(
    row: CustomIngredientSafetyRow,
    state: CustomIngredientEditorUiState,
    onRemove: () -> Unit,
    onGroupChange: (String) -> Unit,
    onRelationTypeChange: (CustomIngredientSafetyRelationType) -> Unit,
    onEvidenceChange: (CustomIngredientSafetyEvidence) -> Unit,
    onSourceDetailsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Declaración", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onRemove) { Text("Eliminar") }
            }

            Text("Grupo", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.safetyGroups, key = { it.id }) { group ->
                    FilterChip(
                        selected = row.safetyGroupId == group.id,
                        onClick = { onGroupChange(group.id) },
                        label = { Text(group.displayName) },
                    )
                }
            }

            Text("Relación registrada", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CustomIngredientSafetyRelationType.values().toList()) { relationType ->
                    FilterChip(
                        selected = row.relationType == relationType,
                        onClick = { onRelationTypeChange(relationType) },
                        label = { Text(relationType.displayName()) },
                    )
                }
            }

            Text("Nivel de evidencia", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CustomIngredientSafetyEvidence.values().forEach { evidence ->
                    FilterChip(
                        selected = row.evidenceLevel == evidence,
                        onClick = { onEvidenceChange(evidence) },
                        label = { Text(evidence.displayName()) },
                    )
                }
            }

            OutlinedTextField(
                value = row.sourceDetails,
                onValueChange = onSafetySourceDetailsChange,
                label = { Text("Origen de la declaración") },
                supportingText = { Text("Ej.: leído en la etiqueta del producto") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = row.notes,
                onValueChange = onNotesChange,
                label = { Text("Notas de seguridad") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private fun CustomIngredientType.displayName(): String = when (this) {
    CustomIngredientType.SIMPLE -> "Simple"
    CustomIngredientType.COMPOUND -> "Compuesto"
    CustomIngredientType.COMMERCIAL_PRODUCT -> "Producto comercial"
}

private fun CustomIngredientSafetyRelationType.displayName(): String = when (this) {
    CustomIngredientSafetyRelationType.INHERENT_SOURCE -> "Fuente inherente"
    CustomIngredientSafetyRelationType.CONTAINS -> "Contiene"
    CustomIngredientSafetyRelationType.DERIVED_FROM -> "Derivado de"
    CustomIngredientSafetyRelationType.REGULATED_COMPONENT -> "Componente regulado"
    CustomIngredientSafetyRelationType.DECLARED_MAY_CONTAIN -> "Puede contener (declarado)"
    CustomIngredientSafetyRelationType.POSSIBLE_CROSS_REACTIVITY -> "Posible reactividad cruzada"
    CustomIngredientSafetyRelationType.UNKNOWN -> "Sin determinar"
}

private fun CustomIngredientSafetyEvidence.displayName(): String = when (this) {
    CustomIngredientSafetyEvidence.USER_DECLARED -> "Declaración del usuario"
    CustomIngredientSafetyEvidence.UNVERIFIED -> "No verificado"
}
