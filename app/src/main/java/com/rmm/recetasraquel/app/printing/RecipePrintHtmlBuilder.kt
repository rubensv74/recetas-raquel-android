package com.rmm.recetasraquel.app.printing

import com.rmm.recetasraquel.domain.ingredient.RecipeSafetyPresentationState
import com.rmm.recetasraquel.domain.ingredient.RecipeSafetySummary
import com.rmm.recetasraquel.domain.ingredient.RegulatoryEffect
import com.rmm.recetasraquel.domain.ingredient.RegulatoryExemption
import com.rmm.recetasraquel.domain.model.Ingredient
import com.rmm.recetasraquel.domain.model.Recipe

internal object RecipePrintHtmlBuilder {

    fun build(
        recipe: Recipe,
        safetySummary: RecipeSafetySummary?,
        safetyMessage: String?,
        coverPhotoDataUri: String? = null,
    ): String {
        val ingredientSafety = buildIngredientSafetyIndex(safetySummary)
        val ingredientReview = safetySummary
            ?.reviewNotices
            .orEmpty()
            .filter { it.ingredientId != null }
            .groupBy { it.ingredientId!! }

        return buildString {
            append(
                """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <style>
                    @page { size: A4 portrait; margin: 14mm 14mm 17mm; }
                    * { box-sizing: border-box; }
                    html, body { margin: 0; padding: 0; }
                    body {
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
                      color: #211B16;
                      background: #FFFFFF;
                      font-size: 10.5pt;
                      line-height: 1.42;
                      -webkit-print-color-adjust: exact;
                      print-color-adjust: exact;
                    }
                    h1, h2, h3, p { margin-top: 0; }
                    h1 {
                      margin-bottom: 5px;
                      font-family: Georgia, "Times New Roman", serif;
                      font-size: 25pt;
                      line-height: 1.05;
                      font-weight: 700;
                    }
                    h2 {
                      margin: 19px 0 8px;
                      font-family: Georgia, "Times New Roman", serif;
                      font-size: 15pt;
                    }
                    .brand {
                      margin-bottom: 9px;
                      color: #9E621C;
                      font-size: 9pt;
                      font-weight: 800;
                      letter-spacing: 1.8px;
                      text-transform: uppercase;
                    }
                    .category {
                      margin-bottom: 5px;
                      color: #5B5048;
                      font-size: 8.5pt;
                      font-weight: 800;
                      letter-spacing: 1px;
                      text-transform: uppercase;
                    }
                    .description { color: #5B5048; font-size: 11pt; }
                    .cover {
                      width: 100%;
                      max-height: 54mm;
                      margin: 10px 0 12px;
                      object-fit: cover;
                      border-radius: 9px;
                    }
                    .metadata {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 8px;
                      margin: 10px 0 12px;
                    }
                    .metadata-item {
                      min-width: 92px;
                      padding: 7px 10px;
                      border: 1px solid #E8E0D6;
                      border-radius: 7px;
                      background: #FAF7F2;
                    }
                    .metadata-label {
                      display: block;
                      color: #5B5048;
                      font-size: 7.7pt;
                      font-weight: 700;
                      text-transform: uppercase;
                    }
                    .metadata-value { font-size: 10.5pt; font-weight: 800; }
                    .safety-critical {
                      margin: 12px 0 17px;
                      padding: 12px 14px;
                      border: 3px solid #A95010;
                      border-radius: 9px;
                      background: #FFF1E6;
                      break-inside: avoid;
                      page-break-inside: avoid;
                    }
                    .safety-critical h2 {
                      margin: 0 0 4px;
                      color: #7D380A;
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
                      font-size: 14pt;
                      font-weight: 900;
                      letter-spacing: .4px;
                      text-transform: uppercase;
                    }
                    .safety-subtitle {
                      margin: 0 0 8px;
                      color: #5B5048;
                      font-size: 8.8pt;
                      font-weight: 650;
                    }
                    .safety-section { margin-top: 7px; }
                    .safety-label {
                      display: block;
                      margin-bottom: 2px;
                      font-size: 8.5pt;
                      font-weight: 900;
                      letter-spacing: .4px;
                      text-transform: uppercase;
                    }
                    .safety-groups { margin: 0; padding-left: 18px; }
                    .safety-groups li { margin: 1px 0; }
                    .safety-ingredient { color: #5B5048; font-size: 8.8pt; }
                    .safety-warning {
                      margin-top: 8px;
                      padding-top: 7px;
                      border-top: 1px solid #D6A77E;
                      font-weight: 800;
                    }
                    .section { break-inside: auto; page-break-inside: auto; }
                    .ingredient, .step, .note-card, .regulatory-item {
                      break-inside: avoid;
                      page-break-inside: avoid;
                    }
                    .ingredient {
                      padding: 7px 0;
                      border-bottom: 1px solid #E8E0D6;
                    }
                    .ingredient-main { font-size: 11pt; font-weight: 650; }
                    .ingredient-notes { margin-top: 2px; color: #5B5048; font-size: 9pt; }
                    .ingredient-safety {
                      margin-top: 3px;
                      color: #7D380A;
                      font-size: 8.8pt;
                      font-weight: 750;
                    }
                    .ingredient-review {
                      margin-top: 3px;
                      color: #8A3029;
                      font-size: 8.8pt;
                      font-weight: 750;
                    }
                    .step {
                      margin: 0 0 8px;
                      padding: 9px 11px;
                      border-left: 4px solid #C58A2A;
                      background: #FAF7F2;
                    }
                    .step-number { font-weight: 900; }
                    .timer { margin-top: 3px; color: #9E621C; font-size: 9pt; font-weight: 800; }
                    .note-card {
                      padding: 10px 12px;
                      border: 1px solid #E8E0D6;
                      border-radius: 7px;
                      background: #FAF7F2;
                    }
                    .review-panel, .regulatory-panel {
                      margin-top: 15px;
                      padding: 11px 13px;
                      border-radius: 8px;
                      break-inside: avoid;
                      page-break-inside: avoid;
                    }
                    .review-panel { border: 2px solid #B5473C; background: #FFF0EE; }
                    .regulatory-panel { border: 2px solid #33709F; background: #EEF7FD; }
                    .panel-title { margin: 0 0 6px; font-size: 11pt; font-weight: 900; }
                    .regulatory-item {
                      margin: 7px 0;
                      padding-top: 7px;
                      border-top: 1px solid #A8C7DA;
                    }
                    .regulatory-disclaimer { margin-top: 8px; font-size: 8.8pt; font-weight: 800; }
                    .footer-note {
                      margin-top: 19px;
                      padding-top: 8px;
                      border-top: 1px solid #E8E0D6;
                      color: #81766D;
                      font-size: 7.8pt;
                    }
                  </style>
                </head>
                <body>
                """.trimIndent(),
            )

            append("""<div class="brand">Recetoria · Receta imprimible</div>""")
            recipe.category?.takeIf { it.isNotBlank() }?.let {
                append("""<div class="category">${it.htmlEscape()}</div>""")
            }
            append("<h1>${recipe.name.htmlEscape()}</h1>")
            recipe.description?.takeIf { it.isNotBlank() }?.let {
                append("""<p class="description">${it.htmlEscape()}</p>""")
            }

            coverPhotoDataUri?.let {
                append("""<img class="cover" src="$it" alt="${recipe.name.htmlEscape()}">""")
            }

            appendMetadata(recipe)
            appendSafetyBlock(safetySummary, safetyMessage)

            if (recipe.ingredients.isNotEmpty()) {
                append("""<section class="section"><h2>Ingredientes</h2>""")
                recipe.ingredients.sortedBy { it.sortOrder }.forEach { ingredient ->
                    append("""<div class="ingredient">""")
                    append("""<div class="ingredient-main">${formatIngredient(ingredient).htmlEscape()}</div>""")
                    ingredient.notes?.takeIf { it.isNotBlank() }?.let {
                        append("""<div class="ingredient-notes">${it.htmlEscape()}</div>""")
                    }
                    ingredientSafety[ingredient.id]?.takeIf { it.isNotEmpty() }?.let { labels ->
                        append("""<div class="ingredient-safety">Alérgenos/seguridad: ${labels.joinToString(" · ").htmlEscape()}</div>""")
                    }
                    ingredientReview[ingredient.id]?.takeIf { it.isNotEmpty() }?.let { notices ->
                        append(
                            """<div class="ingredient-review">Requiere revisión: ${
                                notices.joinToString(" ") { it.message }.htmlEscape()
                            }</div>""",
                        )
                    }
                    append("</div>")
                }
                append("</section>")
            }

            if (recipe.steps.isNotEmpty()) {
                append("""<section class="section"><h2>Preparación</h2>""")
                recipe.steps.sortedBy { it.sortOrder }.forEachIndexed { index, step ->
                    append("""<div class="step">""")
                    append("""<div><span class="step-number">${index + 1}.</span> ${step.instruction.htmlEscape()}</div>""")
                    step.timerMinutes?.let {
                        append("""<div class="timer">Temporizador: $it min</div>""")
                    }
                    append("</div>")
                }
                append("</section>")
            }

            recipe.notes?.takeIf { it.isNotBlank() }?.let {
                append("""<section class="section"><h2>Notas</h2><div class="note-card">${it.htmlEscape()}</div></section>""")
            }

            appendReviewPanel(safetySummary)
            appendRegulatoryPanel(recipe, safetySummary)

            append(
                """
                <div class="footer-note">
                  Recetoria · Documento generado desde los datos guardados en la aplicación.
                  Si existe una alergia o intolerancia, comprueba el etiquetado actual del producto y la información del fabricante.
                </div>
                </body>
                </html>
                """.trimIndent(),
            )
        }
    }

    private fun StringBuilder.appendMetadata(recipe: Recipe) {
        val metadata = listOfNotNull(
            recipe.servings?.let { "Raciones" to it.toString() },
            recipe.preparationMinutes?.let { "Preparación" to "$it min" },
            recipe.cookingMinutes?.let { "Cocción" to "$it min" },
            totalMinutes(recipe)?.let { "Total" to "$it min" },
        )
        if (metadata.isEmpty()) return

        append("""<div class="metadata">""")
        metadata.forEach { (label, value) ->
            append(
                """<div class="metadata-item"><span class="metadata-label">${label.htmlEscape()}</span>""" +
                    """<span class="metadata-value">${value.htmlEscape()}</span></div>""",
            )
        }
        append("</div>")
    }

    private fun StringBuilder.appendSafetyBlock(
        summary: RecipeSafetySummary?,
        safetyMessage: String?,
    ) {
        append("""<section class="safety-critical">""")
        append("<h2>Alérgenos y seguridad alimentaria</h2>")
        append(
            """<p class="safety-subtitle">Información prioritaria basada en los ingredientes y datos de seguridad registrados para esta receta.</p>""",
        )

        when {
            safetyMessage != null -> {
                append("""<div class="safety-warning">NO VERIFICADO · ${safetyMessage.htmlEscape()}</div>""")
            }

            summary == null -> {
                append("""<div class="safety-warning">REQUIERE REVISIÓN · No hay un resumen de seguridad disponible para esta receta.</div>""")
            }

            summary.groups.isEmpty() && summary.reviewNotices.isEmpty() -> {
                append(
                    """<div>No se han detectado coincidencias en los datos registrados.</div>""" +
                        """<div class="safety-warning">La información disponible puede ser incompleta. Comprueba las etiquetas y la información del fabricante.</div>""",
                )
            }

            else -> {
                SAFETY_STATE_ORDER.forEach { state ->
                    val groups = summary.groups.filter { it.presentationState == state }
                    if (groups.isNotEmpty()) {
                        append("""<div class="safety-section"><span class="safety-label">${state.printLabel()}</span><ul class="safety-groups">""")
                        groups.forEach { group ->
                            val responsibleIngredients = group.observations
                                .map { it.ingredientName }
                                .distinct()
                                .take(4)
                            append("<li><strong>${group.safetyGroupName.htmlEscape()}</strong>")
                            if (responsibleIngredients.isNotEmpty()) {
                                append(""" <span class="safety-ingredient">(${responsibleIngredients.joinToString(", ").htmlEscape()})</span>""")
                            }
                            append("</li>")
                        }
                        append("</ul></div>")
                    }
                }

                if (summary.reviewNotices.isNotEmpty()) {
                    val affected = summary.reviewNotices
                        .mapNotNull { it.ingredientName }
                        .distinct()
                        .take(5)
                    append("""<div class="safety-warning">REQUIERE REVISIÓN · ${summary.reviewNotices.size} aviso(s) pendiente(s)""")
                    if (affected.isNotEmpty()) {
                        append(" · ${affected.joinToString(", ").htmlEscape()}")
                    }
                    append("</div>")
                }
            }
        }

        append("</section>")
    }

    private fun StringBuilder.appendReviewPanel(summary: RecipeSafetySummary?) {
        val globalNotices = summary?.reviewNotices.orEmpty().filter { it.ingredientId == null }
        if (globalNotices.isEmpty()) return

        append("""<section class="review-panel"><div class="panel-title">Revisión pendiente</div><ul>""")
        globalNotices.forEach { notice ->
            append("<li>${notice.message.htmlEscape()}</li>")
        }
        append("</ul></section>")
    }

    private fun StringBuilder.appendRegulatoryPanel(
        recipe: Recipe,
        summary: RecipeSafetySummary?,
    ) {
        val exemptions = summary?.regulatoryExemptions.orEmpty()
        if (exemptions.isEmpty()) return

        val ingredientNames = recipe.ingredients
            .mapNotNull { ingredient ->
                ingredient.catalogIngredientId?.let { it to ingredient.name }
            }
            .toMap()

        append("""<section class="regulatory-panel"><div class="panel-title">Información regulatoria de etiquetado</div>""")
        append("""<div>Excepciones legales aplicables bajo condiciones concretas. Este bloque es independiente de la información de seguridad alimentaria.</div>""")
        exemptions.forEach { exemption ->
            append("""<div class="regulatory-item">""")
            append("""<strong>${(ingredientNames[exemption.ingredientId] ?: "Ingrediente regulado").htmlEscape()}</strong><br>""")
            append("${exemption.effect.printLabel().htmlEscape()}<br>")
            append("Condiciones: ${exemption.conditions.htmlEscape()}<br>")
            append("Ámbito: ${exemption.jurisdiction.jurisdictionLabel().htmlEscape()}<br>")
            append("Fuente: ${exemption.sourceId.regulatorySourceLabel().htmlEscape()}<br>")
            exemption.validityLabel()?.let { append("${it.htmlEscape()}<br>") }
            append("Revisado: ${exemption.reviewedAt.htmlEscape()}")
            exemption.notes?.takeIf { it.isNotBlank() }?.let {
                append("<br>${it.htmlEscape()}")
            }
            append("</div>")
        }
        append("""<div class="regulatory-disclaimer">Esta información se refiere a obligaciones de etiquetado. No significa que el alérgeno esté ausente, que no exista riesgo ni que el alimento sea apto para una persona alérgica o intolerante.</div>""")
        append("</section>")
    }

    private fun buildIngredientSafetyIndex(summary: RecipeSafetySummary?): Map<String, List<String>> {
        if (summary == null) return emptyMap()

        return summary.groups
            .flatMap { group ->
                group.observations.map { observation ->
                    observation.ingredientId to "${group.presentationState.shortLabel()}: ${group.safetyGroupName}"
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, labels) -> labels.distinct() }
    }

    private fun formatIngredient(ingredient: Ingredient): String {
        val amount = listOfNotNull(ingredient.quantity, ingredient.unit)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return if (amount.isBlank()) ingredient.name else "$amount · ${ingredient.name}"
    }

    private fun totalMinutes(recipe: Recipe): Int? = when {
        recipe.preparationMinutes == null && recipe.cookingMinutes == null -> null
        else -> (recipe.preparationMinutes ?: 0) + (recipe.cookingMinutes ?: 0)
    }

    private fun RecipeSafetyPresentationState.printLabel(): String = when (this) {
        RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA -> "CONTIENE / PRESENCIA IDENTIFICADA"
        RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO -> "DERIVADO IDENTIFICADO"
        RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO -> "PUEDE CONTENER · DECLARADO"
        RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA -> "POSIBLE REACTIVIDAD CRUZADA"
        RecipeSafetyPresentationState.REQUIERE_REVISION -> "REQUIERE REVISIÓN"
    }

    private fun RecipeSafetyPresentationState.shortLabel(): String = when (this) {
        RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA -> "Contiene"
        RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO -> "Derivado"
        RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO -> "Puede contener"
        RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA -> "Posible reactividad cruzada"
        RecipeSafetyPresentationState.REQUIERE_REVISION -> "Requiere revisión"
    }

    private fun RegulatoryEffect.printLabel(): String = when (this) {
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

    private fun RegulatoryExemption.validityLabel(): String? = when {
        effectiveFrom != null && effectiveTo != null -> "Vigencia: $effectiveFrom a $effectiveTo"
        effectiveFrom != null -> "Vigente desde: $effectiveFrom"
        effectiveTo != null -> "Vigente hasta: $effectiveTo"
        else -> null
    }

    private fun String.htmlEscape(): String = buildString(length) {
        this@htmlEscape.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private val SAFETY_STATE_ORDER = listOf(
        RecipeSafetyPresentationState.PRESENCIA_IDENTIFICADA,
        RecipeSafetyPresentationState.DERIVADO_IDENTIFICADO,
        RecipeSafetyPresentationState.PUEDE_CONTENER_DECLARADO,
        RecipeSafetyPresentationState.POSIBLE_REACTIVIDAD_CRUZADA,
        RecipeSafetyPresentationState.REQUIERE_REVISION,
    )
}
