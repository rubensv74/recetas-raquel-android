package com.rmm.recetasraquel.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption

@Composable
internal fun RecipeRegulatoryPanel(
    exemptions: List<RegulatoryExemption>,
    ingredientNamesByCatalogId: Map<String, String>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_regulatory_panel")
            .semantics { contentDescription = "Información regulatoria de etiquetado" },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Información regulatoria de etiquetado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Se ha identificado información legal aplicable a uno o más ingredientes bajo condiciones concretas.",
                style = MaterialTheme.typography.bodyMedium,
            )

            exemptions.forEach { exemption ->
                RegulatoryExemptionBlock(
                    exemption = exemption,
                    ingredientName = ingredientNamesByCatalogId[exemption.ingredientId],
                )
            }

            Text(
                "Esta información se refiere a obligaciones de etiquetado. No significa que el alérgeno esté ausente, que no exista riesgo ni que el alimento sea apto para una persona alérgica o intolerante.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Si existe una alergia o intolerancia, comprueba el etiquetado actual del producto y sigue las indicaciones sanitarias aplicables a esa persona.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RegulatoryExemptionBlock(
    exemption: RegulatoryExemption,
    ingredientName: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_regulatory_exemption_${exemption.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            ingredientName ?: "Ingrediente regulado",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(exemption.effect.effectLabel(), style = MaterialTheme.typography.bodyMedium)
        Text("Condiciones: ${exemption.conditions}", style = MaterialTheme.typography.bodySmall)
        Text("Ámbito: ${exemption.jurisdiction.jurisdictionLabel()}", style = MaterialTheme.typography.bodySmall)
        Text("Fuente: ${exemption.sourceId.regulatorySourceLabel()}", style = MaterialTheme.typography.bodySmall)
        if (exemption.effectiveFrom != null || exemption.effectiveTo != null) {
            Text(
                exemption.validityLabel(),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("Revisado: ${exemption.reviewedAt}", style = MaterialTheme.typography.bodySmall)
        exemption.notes?.takeIf(String::isNotBlank)?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun RegulatoryEffect.effectLabel(): String = when (this) {
    RegulatoryEffect.EXEMPT_FROM_MANDATORY_ALLERGEN_DECLARATION ->
        "Excepción de la declaración obligatoria de alérgenos bajo las condiciones indicadas."
}

private fun String.jurisdictionLabel(): String = when (this) {
    "EU-ES" -> "Unión Europea / España"
    else -> this
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
