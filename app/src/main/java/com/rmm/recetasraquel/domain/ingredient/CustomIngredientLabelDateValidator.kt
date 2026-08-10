package com.rmm.recetasraquel.domain.ingredient

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object CustomIngredientLabelDateValidator {
    private val isoDatePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT)

    fun isValid(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.isEmpty()) return true
        if (!isoDatePattern.matches(candidate)) return false
        return runCatching { LocalDate.parse(candidate, formatter) }.isSuccess
    }
}
