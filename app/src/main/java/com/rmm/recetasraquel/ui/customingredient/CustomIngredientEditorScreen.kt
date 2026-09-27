package com.rmm.recetasraquel.ui.customingredient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nuevo ingrediente personalizado",
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
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                IntroCard()
            }

            item {
                PremiumSectionCard(
                    eyebrow = "DATOS PRINCIPALES",
                    title = "Identidad del ingrediente",
                    supportingText = "Define cómo aparecerá este ingrediente en tus recetas y en la biblioteca local.",
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = { Text("Nombre *") },
                        singleLine = true,
                        colors = premiumTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_name"),
                    )

                    FieldLabel("Tipo")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(CustomIngredientType.values().toList()) { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { onTypeChange(type) },
                                label = { Text(type.displayName()) },
                                colors = premiumFilterChipColors(),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.testTag("custom_type_${type.name}"),
                            )
                        }
                    }

                    FieldLabel("Categoría")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedCategoryId == null,
                                onClick = { onCategoryChange(null) },
                                label = { Text("Sin categoría") },
                                colors = premiumFilterChipColors(),
                                shape = MaterialTheme.shapes.large,
                            )
                        }
                        items(state.categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = state.selectedCategoryId == category.id,
                                onClick = { onCategoryChange(category.id) },
                                label = { Text(category.name) },
                                colors = premiumFilterChipColors(),
                                shape = MaterialTheme.shapes.large,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.defaultUnit,
                        onValueChange = onDefaultUnitChange,
                        label = { Text("Unidad habitual") },
                        singleLine = true,
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.aliasesText,
                        onValueChange = onAliasesChange,
                        label = { Text("Alias") },
                        supportingText = { Text("Separados por comas") },
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.type == CustomIngredientType.COMMERCIAL_PRODUCT) {
                item {
                    PremiumSectionCard(
                        eyebrow = "PRODUCTO CONCRETO",
                        title = "Datos comerciales",
                        supportingText = "Registra la referencia exacta cuya etiqueta has consultado.",
                    ) {
                        OutlinedTextField(
                            value = state.brand,
                            onValueChange = onBrandChange,
                            label = { Text("Marca") },
                            singleLine = true,
                            colors = premiumTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_brand"),
                        )
                        OutlinedTextField(
                            value = state.tradeName,
                            onValueChange = onTradeNameChange,
                            label = { Text("Nombre comercial") },
                            singleLine = true,
                            colors = premiumTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_trade_name"),
                        )
                        OutlinedTextField(
                            value = state.labelReadAt,
                            onValueChange = onLabelReadAtChange,
                            label = { Text("Fecha de lectura de etiqueta") },
                            supportingText = { Text("Opcional · AAAA-MM-DD") },
                            singleLine = true,
                            colors = premiumTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_label_read_at"),
                        )
                    }
                }
            }

            item {
                PremiumSectionCard(
                    eyebrow = "COMPOSICIÓN",
                    title = "Qué sabemos del producto",
                    supportingText = "Este dato condiciona cómo debe interpretarse la información de seguridad.",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.compositionKnown == true,
                            onClick = { onCompositionKnownChange(true) },
                            label = { Text("Conocida") },
                            colors = premiumFilterChipColors(),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.testTag("composition_known"),
                        )
                        FilterChip(
                            selected = state.compositionKnown == false,
                            onClick = { onCompositionKnownChange(false) },
                            label = { Text("Desconocida") },
                            colors = premiumFilterChipColors(),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.testTag("composition_unknown"),
                        )
                    }

                    InformationNotice(
                        text = "Si la composición es desconocida, la aplicación debe tratar la información disponible como incompleta.",
                        emphasized = state.compositionKnown == false,
                    )

                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = onNotesChange,
                        label = { Text("Notas") },
                        minLines = 2,
                        maxLines = 5,
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                SectionHeading(
                    eyebrow = "RIESGO Y EVIDENCIA",
                    title = "Seguridad alimentaria",
                    supportingText = "Registra únicamente declaraciones que puedas justificar. No declarar un alérgeno no demuestra que esté ausente.",
                )
            }

            item {
                InformationNotice(
                    text = "Registra solo lo que conozcas. La ausencia de declaraciones no significa ausencia de alérgenos o riesgo. Comprueba siempre la etiqueta del producto cuando corresponda.",
                    emphasized = true,
                    modifier = Modifier.testTag("custom_safety_warning"),
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
                OutlinedButton(
                    onClick = onAddSafetyRow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_safety_declaration"),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Añadir declaración de seguridad")
                }
            }

            state.validationMessage?.let { message ->
                item {
                    ErrorNotice(message)
                }
            }

            state.errorMessage?.let { message ->
                item {
                    ErrorNotice(message) {
                        TextButton(onClick = onRetry) {
                            Text("Recargar datos")
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp)
                        .testTag("save_custom_ingredient"),
                    shape = MaterialTheme.shapes.large,
                ) {
                    if (state.isSaving) {
                        Text("Guardando…")
                    } else {
                        Text("Guardar ingrediente")
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "INGREDIENTE PERSONALIZADO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Una identidad local, con contexto",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Crea una identidad local cuando el ingrediente no esté en la biblioteca o necesites registrar un producto concreto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PremiumSectionCard(
    eyebrow: String,
    title: String,
    supportingText: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun SectionHeading(
    eyebrow: String,
    title: String,
    supportingText: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("safety_row_${row.key}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DECLARACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Información de seguridad",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(onClick = onRemove) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            FieldLabel("Grupo")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.safetyGroups, key = { it.id }) { group ->
                    FilterChip(
                        selected = row.safetyGroupId == group.id,
                        onClick = { onGroupChange(group.id) },
                        label = { Text(group.displayName) },
                        colors = premiumFilterChipColors(),
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }

            FieldLabel("Relación registrada")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CustomIngredientSafetyRelationType.values().toList()) { relationType ->
                    FilterChip(
                        selected = row.relationType == relationType,
                        onClick = { onRelationTypeChange(relationType) },
                        label = { Text(relationType.displayName()) },
                        colors = premiumFilterChipColors(),
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }

            FieldLabel("Nivel de evidencia")
            LazyRow(
                modifier = Modifier.testTag("custom_safety_evidence_${row.key}"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(CustomIngredientSafetyEvidence.values().toList()) { evidence ->
                    FilterChip(
                        selected = row.evidenceLevel == evidence,
                        onClick = { onEvidenceChange(evidence) },
                        label = { Text(evidence.displayName()) },
                        colors = premiumFilterChipColors(),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.testTag("custom_safety_evidence_${row.key}_${evidence.name}"),
                    )
                }
            }

            OutlinedTextField(
                value = row.sourceDetails,
                onValueChange = onSourceDetailsChange,
                label = { Text("Origen de la declaración") },
                supportingText = { Text("Ej.: leído en la etiqueta del producto") },
                colors = premiumTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = row.notes,
                onValueChange = onNotesChange,
                label = { Text("Notas de seguridad") },
                minLines = 2,
                maxLines = 4,
                colors = premiumTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InformationNotice(
    text: String,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (emphasized) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ErrorNotice(
    message: String,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            action?.invoke()
        }
    }
}

@Composable
private fun premiumFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
private fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

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
