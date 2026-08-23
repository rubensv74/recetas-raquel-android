package com.rmm.recetasraquel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    inversePrimary = BronzeLight,
    primary = ToastedBronze,
    onPrimary = SoftIvory,
    primaryContainer = BronzeContainer,
    onPrimaryContainer = EspressoInk,
    secondary = AntiqueGold,
    onSecondary = EspressoInk,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = EspressoInk,
    tertiary = MayContain,
    onTertiary = SoftIvory,
    tertiaryContainer = BlueContainer,
    onTertiaryContainer = EspressoInk,
    error = CriticalRisk,
    onError = SoftIvory,
    errorContainer = RedContainer,
    onErrorContainer = EspressoInk,
    background = SoftIvory,
    onBackground = EspressoInk,
    surface = IvorySurface,
    onSurface = EspressoInk,
    surfaceVariant = WarmStone,
    onSurfaceVariant = SecondaryInk,
    outline = WarmOutline,
    outlineVariant = WarmStone,
    surfaceTint = ToastedBronze,
    inverseSurface = EspressoInk,
    inverseOnSurface = SoftIvory,
    scrim = EspressoInk,
    surfaceBright = IvorySurface,
    surfaceDim = WarmStone,
    surfaceContainerLowest = IvorySurface,
    surfaceContainerLow = SoftIvory,
    surfaceContainer = WarmStone,
    surfaceContainerHigh = WarmStone,
    surfaceContainerHighest = WarmStone,
)

private val DarkColors = darkColorScheme(
    inversePrimary = ToastedBronze,
    primary = BronzeLight,
    onPrimary = NightBackground,
    primaryContainer = BronzeDarkContainer,
    onPrimaryContainer = NightText,
    secondary = GoldLight,
    onSecondary = NightBackground,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = NightText,
    tertiary = BlueLight,
    onTertiary = NightBackground,
    tertiaryContainer = BlueDarkContainer,
    onTertiaryContainer = NightText,
    error = RedLight,
    onError = NightBackground,
    errorContainer = RedDarkContainer,
    onErrorContainer = NightText,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceRaised,
    onSurfaceVariant = NightTextSecondary,
    outline = NightOutline,
    outlineVariant = NightSurfaceHighest,
    surfaceTint = BronzeLight,
    inverseSurface = SoftIvory,
    inverseOnSurface = EspressoInk,
    scrim = EspressoInk,
    surfaceBright = NightSurfaceHighest,
    surfaceDim = NightBackground,
    surfaceContainerLowest = NightBackground,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurfaceRaised,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
)

@Immutable
data class RecetoriaSafetyPalette(
    val confirmed: Color,
    val confirmedContainer: Color,
    val mayContain: Color,
    val mayContainContainer: Color,
    val critical: Color,
    val criticalContainer: Color,
)

private val LightSafetyPalette = RecetoriaSafetyPalette(
    confirmed = ConfirmedPresence,
    confirmedContainer = AmberContainer,
    mayContain = MayContain,
    mayContainContainer = BlueContainer,
    critical = CriticalRisk,
    criticalContainer = RedContainer,
)

private val DarkSafetyPalette = RecetoriaSafetyPalette(
    confirmed = AmberLight,
    confirmedContainer = BronzeDarkContainer,
    mayContain = BlueLight,
    mayContainContainer = BlueDarkContainer,
    critical = RedLight,
    criticalContainer = RedDarkContainer,
)

private val LocalSafetyPalette = staticCompositionLocalOf { LightSafetyPalette }

object RecetoriaTheme {
    val safety: RecetoriaSafetyPalette
        @Composable get() = LocalSafetyPalette.current
}

@Composable
fun RecetasRaquelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSafetyPalette provides if (darkTheme) DarkSafetyPalette else LightSafetyPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = RecetasTypography,
            shapes = RecetasShapes,
            content = content,
        )
    }
}
