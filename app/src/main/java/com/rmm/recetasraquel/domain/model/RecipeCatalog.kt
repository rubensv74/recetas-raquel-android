package com.rmm.recetasraquel.domain.model

data class RecipeSummary(
    val id: String,
    val name: String,
    val category: String?,
    val servings: Int?,
    val preparationMinutes: Int?,
    val cookingMinutes: Int?,
    val isFavorite: Boolean,
    val coverPhotoPath: String?,
    val updatedAt: Long,
)

data class RecipeCatalogFilter(
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val category: String? = null,
) {
    fun normalized() = copy(
        query = query.trim(),
        category = category?.trim()?.takeIf(String::isNotEmpty),
    )

    val isActive: Boolean
        get() = query.isNotBlank() || favoritesOnly || category != null
}
