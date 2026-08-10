package com.rmm.recetasraquel.ui.ingredientlibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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

@Composable
internal fun IngredientLibraryInfoDialog(
    state: IngredientLibraryInfoUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Información del ingrediente")
                Text(
                    text = state.ingredient.canonicalName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
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
                        Text("Cargando información registrada…")
                    }

                    state.errorMessage != null -> {
                        Text(state.errorMessage)
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("ingredient_library_info_retry"),
                        ) {
                            Text("Reintentar")
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
    Text(
        text = state.ingredient.categoryName,
        style = MaterialTheme.typography.bodyMedium,
    )

    state.catalogDetail.description?.takeIf(String::isNotBlank)?.let { description ->
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.catalogDetail.aliases.joinToString(separator = " · "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (state.catalogDetail.relatedPresentations.isNotEmpty()) {
        HorizontalDivider()
        Text(
            text = "Presentaciones relacionadas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        state.catalogDetail.relatedPresentations.forEach { related ->
            Text(
                text = "${related.relationshipLabel()}: ${related.canonicalName}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("ingredient_library_related_${related.ingredientId}"),
            )
        }
        Text(
            text = "Estas relaciones describen identidad culinaria. No heredan ni generan información de seguridad alimentaria.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("ingredient_library_lineage_disclaimer"),
        )
    }

    HorizontalDivider()
    Text(
        text = "Información de seguridad alimentaria",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )

    if (state.safetyRelations.isEmpty()) {
        Text(
            text = "No hay una relación directa de seguridad registrada para este ingrediente. Esto no demuestra ausencia de alérgenos ni ausencia de riesgo.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("ingredient_library_no_direct_safety"),
        )
    } else {
        state.safetyRelations.forEach { record ->
            SafetyRecordBlock(record)
        }
    }

    if (state.regulatoryExemptions.isNotEmpty()) {
        HorizontalDivider()
        Text(
            text = "Información regulatoria de etiquetado",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Estas reglas describen obligaciones legales bajo condiciones concretas; no sustituyen la información de seguridad alimentaria.",
            style = MaterialTheme.typography.bodySmall,
        )
        state.regulatoryExemptions.forEach { exemption ->
            RegulatoryRecordBlock(exemption)
        }
        Text(
            text = "Una excepción regulatoria no significa ausencia del alérgeno, ausencia de riesgo ni aptitud para una persona alérgica o intolerante.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("ingredient_library_regulatory_disclaimer"),
        )
    }

    HorizontalDivider()
    Text(
        text = "La información mostrada procede del catálogo documentado de la aplicación. Ante una alergia o intolerancia, revisa el etiquetado actual del producto y sigue las indicaciones sanitarias aplicables a la persona.",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SafetyRecordBlock(record: CatalogIngredientSafetyRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("ingredient_library_safety_${record.safetyGroupId}"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = record.safetyGroupName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text("Relación: ${record.relationType.relationTypeLabel()}", style = MaterialTheme.typography.bodySmall)
        Text("Evidencia: ${record.evidenceLevel.evidenceLabel()}", style = MaterialTheme.typography.bodySmall)
        Text("Ámbito: ${record.jurisdiction.jurisdictionLabel()}", style = MaterialTheme.typography.bodySmall)
        Text(
            "Fuente: ${record.sourceDetails ?: record.sourceId}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Revisado: ${record.reviewedAt}", style = MaterialTheme.typography.bodySmall)
        record.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RegulatoryRecordBlock(exemption: RegulatoryExemption) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("ingredient_library_regulatory_${exemption.id}"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = exemption.effect.effectLabel(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text("Condiciones: ${exemption.conditions}", style = MaterialTheme.typography.bodySmall)
        Text("Ámbito: ${exemption.jurisdiction.jurisdictionLabel()}", style = MaterialTheme.typography.bodySmall)
        Text("Fuente: ${exemption.sourceId.regulatorySourceLabel()}", style = MaterialTheme.typography.bodySmall)
        if (exemption.effectiveFrom != null || exemption.effectiveTo != null) {
            Text(exemption.validityLabel(), style = MaterialTheme.typography.bodySmall)
        }
        Text("Revisado: ${exemption.reviewedAt}", style = MaterialTheme.typography.bodySmall)
        exemption.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodySmall)
        }
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
