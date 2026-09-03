package com.expensesplitter.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = OnGreenPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = OnGreenPrimaryContainerLight,
    // secondary intentionally mirrors primary (green), not the red balance
    // color — components like FilterChip default to `secondary` for their
    // selected state, and selection should read as affirmative, not a
    // warning. Red is reserved for BalanceColors, used directly where it
    // means "you owe."
    secondary = GreenPrimaryLight,
    onSecondary = OnGreenPrimaryLight,
    secondaryContainer = GreenPrimaryContainerLight,
    onSecondaryContainer = OnGreenPrimaryContainerLight,
    tertiary = IndigoTertiaryLight,
    onTertiary = OnIndigoTertiaryLight,
    tertiaryContainer = IndigoTertiaryContainerLight,
    onTertiaryContainer = OnIndigoTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    // These are unused by any Text/Icon color directly, but M3 components
    // (NavigationBar, Menu, Dialog, elevated Card) read them for their
    // container fill. Leaving them unset falls back to Material's baseline
    // purple seed — that's the bug that made the bottom nav bar lavender
    // instead of the app's neutral/green palette.
    surfaceTint = GreenPrimaryLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = GreenPrimaryDark,
    scrim = ScrimLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = OnGreenPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = OnGreenPrimaryContainerDark,
    secondary = GreenPrimaryDark,
    onSecondary = OnGreenPrimaryDark,
    secondaryContainer = GreenPrimaryContainerDark,
    onSecondaryContainer = OnGreenPrimaryContainerDark,
    tertiary = IndigoTertiaryDark,
    onTertiary = OnIndigoTertiaryDark,
    tertiaryContainer = IndigoTertiaryContainerDark,
    onTertiaryContainer = OnIndigoTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surfaceTint = GreenPrimaryDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = GreenPrimaryLight,
    scrim = ScrimDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

@Composable
fun ExpenseSplitterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
