package com.expensesplitter.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expensesplitter.app.R

// Space Grotesk — geometric, confident, slightly technical. Used by
// Copilot Money / Cash App / Revolut, Uniswap, and most 2026-era
// crypto/fintech products for exactly this reason: it reads as "modern
// finance" without tipping into the loud/uppercase brutalism of its sibling
// pairings. One variable file, several weights via variation settings.
@OptIn(ExperimentalTextApi::class)
private val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.space_grotesk_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.space_grotesk_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.space_grotesk_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMedium = baseline.displayMedium.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displaySmall = baseline.displaySmall.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold),
    titleLarge = baseline.titleLarge.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = SpaceGroteskFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = SpaceGroteskFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = SpaceGroteskFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = baseline.labelMedium.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    labelSmall = baseline.labelSmall.copy(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Medium),
)

// Money is the hero of a finance app and M3's 15-slot type scale tops out at
// displayLarge (57sp) — too small and too light for a screen's single most
// important number. This is a dedicated scale for amounts, used via
// MoneyText (see MoneyText.kt), not part of MaterialTheme.typography.
object MoneyType {
    val hero = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 56.sp, letterSpacing = (-1).sp)
    val large = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-0.5).sp)
    val medium = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.25).sp)
    val small = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
}
