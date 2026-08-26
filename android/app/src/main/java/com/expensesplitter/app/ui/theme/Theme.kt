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
    tertiary = VioletTertiaryLight,
    onTertiary = OnVioletTertiaryLight,
    tertiaryContainer = VioletTertiaryContainerLight,
    onTertiaryContainer = OnVioletTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
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
    tertiary = VioletTertiaryDark,
    onTertiary = OnVioletTertiaryDark,
    tertiaryContainer = VioletTertiaryContainerDark,
    onTertiaryContainer = OnVioletTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = BackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

@Composable
fun ExpenseSplitterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
