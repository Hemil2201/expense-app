package com.expensesplitter.app.ui.theme

import androidx.compose.ui.graphics.Color

// Splitwise-style palette: signature green for brand/positive-balance,
// warm red-orange for negative balance, near-white flat backgrounds (not
// tinted container fills) — matches Splitwise's list-heavy, low-chrome UI.

val GreenPrimaryLight = Color(0xFF1BA37B)
val OnGreenPrimaryLight = Color(0xFFFFFFFF)
val GreenPrimaryContainerLight = Color(0xFFCFF2E4)
val OnGreenPrimaryContainerLight = Color(0xFF00201A)

val RedSecondaryLight = Color(0xFFE8604A)
val OnRedSecondaryLight = Color(0xFFFFFFFF)
val RedSecondaryContainerLight = Color(0xFFFFDBD1)
val OnRedSecondaryContainerLight = Color(0xFF3A0900)

val VioletTertiaryLight = Color(0xFF6B5CA5)
val OnVioletTertiaryLight = Color(0xFFFFFFFF)
val VioletTertiaryContainerLight = Color(0xFFE9DDFF)
val OnVioletTertiaryContainerLight = Color(0xFF22005D)

val BackgroundLight = Color(0xFFFFFFFF)
val OnBackgroundLight = Color(0xFF1B1F1D)
val SurfaceVariantLight = Color(0xFFF0F2F1)
val OnSurfaceVariantLight = Color(0xFF5B635F)
val OutlineLight = Color(0xFFDDE2E0)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val GreenPrimaryDark = Color(0xFF6FDBB4)
val OnGreenPrimaryDark = Color(0xFF00382C)
val GreenPrimaryContainerDark = Color(0xFF00513F)
val OnGreenPrimaryContainerDark = Color(0xFFCFF2E4)

val RedSecondaryDark = Color(0xFFFFB4A0)
val OnRedSecondaryDark = Color(0xFF5F1600)
val RedSecondaryContainerDark = Color(0xFF7D2C10)
val OnRedSecondaryContainerDark = Color(0xFFFFDBD1)

val VioletTertiaryDark = Color(0xFFD2BFFF)
val OnVioletTertiaryDark = Color(0xFF392C6F)
val VioletTertiaryContainerDark = Color(0xFF514287)
val OnVioletTertiaryContainerDark = Color(0xFFE9DDFF)

val BackgroundDark = Color(0xFF161917)
val OnBackgroundDark = Color(0xFFE1E3E0)
val SurfaceVariantDark = Color(0xFF2A2E2C)
val OnSurfaceVariantDark = Color(0xFFB8C0BB)
val OutlineDark = Color(0xFF3A3F3D)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Balance semantics: green = they owe you / you're owed (positive), red = you
// owe them (negative) — the classic Splitwise convention.
object BalanceColors {
    val positiveLight = GreenPrimaryLight
    val positiveContainerLight = GreenPrimaryContainerLight
    val negativeLight = RedSecondaryLight
    val negativeContainerLight = RedSecondaryContainerLight

    val positiveDark = GreenPrimaryDark
    val positiveContainerDark = GreenPrimaryContainerDark
    val negativeDark = RedSecondaryDark
    val negativeContainerDark = RedSecondaryContainerDark
}

// Deterministic color assignment for category icon backgrounds — cycles
// through a curated set so any category (default or user-added later) gets a
// distinct, pleasant color without needing a maintained name->color map.
val CategoryPalette = listOf(
    Color(0xFF1BA37B), // green
    Color(0xFFE8604A), // red-orange
    Color(0xFF6B5CA5), // violet
    Color(0xFFC97B2E), // amber
    Color(0xFF3B6FB6), // blue
    Color(0xFF9C4B7A), // magenta
    Color(0xFF57883C), // olive green
    Color(0xFFB05252), // brick red
)

fun categoryColorFor(categoryName: String): Color {
    val index = categoryName.fold(0) { acc, c -> acc + c.code }
    return CategoryPalette[index % CategoryPalette.size]
}
