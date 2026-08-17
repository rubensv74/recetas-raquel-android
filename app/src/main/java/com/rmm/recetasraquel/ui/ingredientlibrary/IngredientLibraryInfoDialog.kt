package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmm.recetasraquel.domain.ingredient.CatalogIngredientSafetyRecord
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogRelatedPresentation
import com.rmm.recetasraquel.domain.ingredient.IngredientCatalogRelationDirection
import com.rmm.recetasraquel.domain.ingredient.IngredientLineageType
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.ui.theme.RecetoriaTheme

@Composable
internal fun IngredientLibraryInfoDialog(
    state: IngredientLibraryInfoUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "INFORMACIÓN DEL INGREDIENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.ingredient.canonicalName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.ingredient.categoryName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("ingredient_library_info_dialog")
                    .semantics {
                        contentDescription = "Información de identidad, seguridad y regulación del ingrediente"
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Text(
                            text = "Cargando información registrada…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.errorMessage != null -> {
                        InfoSectionCard(
                            title = "No se pudo cargar la información",
                            titleColor = MaterialTheme.colorScheme.error,
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(
                                onClick = onRetry,
                                modifier = Modifier.testTag("ingredient_library_info_retry"),
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }

                    else -> IngredientInformationContent(state)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("ingredient_library_info_close"),
            ) {
                Text("Cerrar")
            }
        },
    )
}

@Composable
private fun IngredientInformationContent(state: IngredientLibraryInfoUiState) {
    if (
        !state.catalogDetail.description.isNullOrBlank() ||
        state.catalogDetail.aliases.isNotEmpty()
    ) {
        InfoSectionCard(
            title = "Identidad culinaria",
            eyebrow = "CATÁLOGO",
        ) {
            state.catalogDetail.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("ingredient_library_description"),
                )
            }

            if (state.catalogDetail.aliases.isNotEmpty()) {
                Column(
                    modifier = Modifier.testTag("ingredient_library_aliases"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "También puede aparecer como",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.catalogDetail.aliases.joinToString(separator = " · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (state.catalogDetail.relatedPresentations.isNotEmpty()) {
        InfoSectionCard(
            title = "Presentaciones relacionadas",
            eyebrow = "RELACIÓN CULINARIA",
        ) {
            state.catalogDetail.relatedPresentations.forEach { related ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ingredient_library_related_${related.ingredientId}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = related.relationshipLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.42f),
                    )
                    Text(
                        text = related.canonicalName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.58f),
                    )
                }
            }
            ContextNotice(
                text = "Estas relaciones describen identidad culinaria. No heredan ni generan información de seguridad alimentaria.",
                modifier = Modifier.testTag("ingredient_library_lineage_disclaimer"),
            )
        }
    }

    SafetySection(state)

    if (state.regulatoryExemptions.isNotEmpty()) {
        RegulatorySection(state)
    }

    ContextNotice(
        title = "Antes de decidir",
        text = "La información mostrada procede del catálogo documentado de la aplicación. Ante una alergia o intolerancia, revisa el etiquetado actual del producto y sigue las indicaciones sanitarias aplicables a la persona.",
        emphasized = true,
    )
}

@Composable
private fun SafetySection(state: IngredientLibraryInfoUiState) {
    InfoSectionCard(
        title = "Seguridad alimentaria",
        eyebrow = "RIESGO Y EVIDENCIA",
        borderColor = when {
            state.safetyRelations.isEmpty() -> RecetoriaTheme.safety.mayContain
            state.safetyRelations.any { it.relationType == "UNKNOWN" } -> RecetoriaTheme.safety.critical
            state.safetyRelations.any {
                it.relationType == "DECLARED_MAY_CONTAIN" ||
                    it.relationType == "POSSIBLE_CROSS_REACTIVITY"
            } -> RecetoriaTheme.safety.mayContain
            else -> RecetoriaTheme.safety.confirmed
        },
    ) {
        if (state.safetyRelations.isEmpty()) {
            ContextNotice(
                title = "Información directa no registrada",
                text = "No hay una relación directa de seguridad registrada para este ingrediente. Esto no demuestra ausencia de alérgenos ni ausencia de riesgo.",
                modifier = Modifier.testTag("ingredient_library_no_direct_safety"),
                emphasized = true,
            )
        } else {
            state.safetyRelations.forEachIndexed { index, record ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                SafetyRecordBlock(record)
            }
        }
    }
}

@Composable
private fun RegulatorySection(state: IngredientLibraryInfoUiState) {
    InfoSectionCard(
        title = "Información regulatoria de etiquetado",
        eyebrow = "OBLIGACIÓN LEGAL",
        borderColor = MaterialTheme.colorScheme.secondary,
    ) {
        Text(
            text = "Estas reglas describen obligaciones legales bajo condiciones concretas; no sustituyen la información de seguridad alimentaria.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.regulatoryExemptions.forEachIndexed { index, exemption ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            RegulatoryRecordBlock(exemption)
        }
        ContextNotice(
            title = "Importante",
            text = "Una excepción regulatoria no significa ausencia del alérgeno, ausencia de riesgo ni aptitud para una persona alérgica o intolerante.",
            modifier = Modifier.testTag("ingredient_library_regulatory_disclaimer"),
            emphasized = true,
        )
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    eyebrow: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    borderColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            eyebrow?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
            )
            content()
        }
    }
}

@Composable
private fun ContextNotice(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    emphasized: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (emphasized) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SafetyRecordBlock(record: CatalogIngredientSafetyRecord) {
    val semanticColor = safetyRecordColor(record.relationType)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("ingredient_library_safety_${record.safetyGroupId}"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = record.safetyGroupName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = semanticColor,
        )
        RecordDetail("Relación", record.relationType.relationTypeLabel())
        RecordDetail("Evidencia", record.evidenceLevel.evidenceLabel())
        RecordDetail("Ámbito", record.jurisdiction.jurisdictionLabel())
        RecordDetail("Fuente", record.sourceDetails ?: record.sourceId)
        RecordDetail("Revisado", record.reviewedAt)
        record.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun safetyRecordColor(relationType: String) = when (relationType) {
    "DECLARED_MAY_CONTAIN", "POSSIBLE_CROSS_REACTIVITY" -> RecetoriaTheme.safety.mayContain
    "UNKNOWN" -> RecetoriaTheme.safety.critical
    else -> RecetoriaTheme.safety.confirmed
}

@Composable
private fun RegulatoryRecordBlock(exemption: RegulatoryExemption) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("ingredient_library_regulatory_${exemption.id}"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = exemption.effect.effectLabel(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        RecordDetail("Condiciones", exemption.conditions)
        RecordDetail("Ámbito", exemption.jurisdiction.jurisdictionLabel())
        RecordDetail("Fuente", exemption.sourceId.regulatorySourceLabel())
        if (exemption.effectiveFrom != null || exemption.effectiveTo != null) {
            RecordDetail("Vigencia", exemption.validityLabel().removePrefix("Vigencia: "))
        }
        RecordDetail("Revisado", exemption.reviewedAt)
        exemption.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.30f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.70f),
        )
    }
}

private fun IngredientCatalogRelatedPresentation.relationshipLabel(): String = when (direction) {
    IngredientCatalogRelationDirection.PARENT -> when (relationType) {
        IngredientLineageType.DERIVED_FROM -> "Derivado de"
        IngredientLineageType.VARIANT_OF -> "Variante de"
        IngredientLineageType.CUT_OF -> "Corte de"
        IngredientLineageType.FORM_OF -> "Forma de"
    }
    IngredientCatalogRelationDirection.CHILD -> when (relationType) {
        IngredientLineageType.DERIVED_FROM -> "Derivado relacionado"
        IngredientLineageType.VARIANT_OF -> "Variante relacionada"
        IngredientLineageType.CUT_OF -> "Corte relacionado"
        IngredientLineageType.FORM_OF -> "Forma relacionada"
    }
}

private fun String.relationTypeLabel(): String = when (this) {
    "INHERENT_SOURCE" -> "Fuente inherente del grupo"
    "CONTAINS" -> "Contiene"
    "REGULATED_COMPONENT" -> "Componente regulado"
    "DERIVED_FROM" -> "Derivado de"
    "DECLARED_MAY_CONTAIN" -> "Puede contener (declarado)"
    "POSSIBLE_CROSS_REACTIVITY" -> "Posible reactividad cruzada"
    "UNKNOWN" -> "Requiere revisión"
    else -> this.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun String.evidenceLabel(): String = when (this) {
    "EU_LEGAL" -> "Fuente jurídica de la Unión Europea"
    "OFFICIAL" -> "Fuente oficial"
    "REVIEWED" -> "Revisado"
    "USER_DECLARED" -> "Declarado por el usuario"
    "UNVERIFIED" -> "Sin verificar"
    else -> this.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

private fun String.jurisdictionLabel(): String = when (this) {
    "EU-ES" -> "Unión Europea / España"
    else -> this
}

private fun RegulatoryEffect.effectLabel(): String = when (this) {
    RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION ->
        "Excepción de la declaración obligatoria de alérgenos"
}

private fun String.regulatorySourceLabel(): String = when (this) {
    "EU_FIC_1169_2011" -> "Unión Europea · Reglamento (UE) n.º 1169/2011 · Anexo II"
    "EU_MUSTARD_2024_2512" -> "Comisión Europea · Reglamento Delegado (UE) 2024/2512"
    else -> this
}

private fun RegulatoryExemption.validityLabel(): String = when {
    effectiveFrom != null && effectiveTo != null -> "Vigencia: $effectiveFrom a $effectiveTo"
    effectiveFrom != null -> "Vigente desde: $effectiveFrom"
    effectiveTo != null -> "Vigente hasta: $effectiveTo"
    else -> ""
}
