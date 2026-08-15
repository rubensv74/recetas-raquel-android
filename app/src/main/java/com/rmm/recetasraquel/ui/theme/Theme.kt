package com.rmm.recetasraquel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Copper,
    onPrimary = Porcelain,
    primaryContainer = CopperSoft,
    onPrimaryContainer = CopperDeep,
    secondary = Olive,
    onSecondary = Porcelain,
    secondaryContainer = OliveSoft,
    onSecondaryContainer = Espresso,
    tertiary = CopperDeep,
    onTertiary = Porcelain,
    background = Ivory,
    onBackground = Espresso,
    surface = Porcelain,
    onSurface = Espresso,
    surfaceVariant = Linen,
    onSurfaceVariant = SoftInk,
    outline = OutlineWarm,
)

private val DarkColors = darkColorScheme(
    primary = CopperLight,
    onPrimary = NightEspresso,
    primaryContainer = CopperDeep,
    onPrimaryContainer = NightIvory,
    secondary = OliveLight,
    onSecondary = NightEspresso,
    secondaryContainer = Olive,
    onSecondaryContainer = NightIvory,
    tertiary = Sand,
    onTertiary = NightEspresso,
    background = NightEspresso,
    onBackground = NightIvory,
    surface = NightSurface,
    onSurface = NightIvory,
    surfaceVariant = NightSurfaceRaised,
    onSurfaceVariant = NightSoftInk,
    outline = SoftInk,
)

@Composable
fun RecetasRaquelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RecetasTypography,
        shapes = RecetasShapes,
        content = content,
    )
}
