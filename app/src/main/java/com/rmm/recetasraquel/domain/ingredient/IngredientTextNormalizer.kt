package com.rmm.recetasraquel.domain.ingredient

import java.text.Normalizer
import java.util.Locale

object IngredientTextNormalizer {
    private val combiningMarks = Regex("\\p{M}+")
    private val spaces = Regex("\\s+")

    fun normalize(value: String): String = Normalizer
        .normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .replace(spaces, " ")
}
