package com.expensesplitter.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// The exact mark used for the launcher icon (see
// res/drawable/ic_launcher_foreground.xml) — kept as a real ImageVector
// rather than a tinted Icon() so every in-app use renders the identical
// multi-color logo, not a flat single-tone approximation of it.
val PiggyBankLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "PiggyBankLogo",
        defaultWidth = 70.dp,
        defaultHeight = 58.dp,
        viewportWidth = 70f,
        viewportHeight = 58f,
    ).apply {
        // body
        path(fill = SolidColor(Color(0xFF059669))) {
            moveTo(6f, 34f)
            arcTo(26f, 20f, 0f, isMoreThanHalf = true, isPositiveArc = false, 58f, 34f)
            arcTo(26f, 20f, 0f, isMoreThanHalf = true, isPositiveArc = false, 6f, 34f)
            close()
        }
        // snout
        path(fill = SolidColor(Color(0xFF059669))) {
            moveTo(56f, 28f)
            curveTo(60f, 26f, 64f, 27f, 64f, 31f)
            curveTo(64f, 35f, 60f, 35f, 57f, 33f)
            close()
        }
        // eye
        path(fill = SolidColor(Color(0xFF0F172A))) {
            moveTo(13f, 32f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 19f, 32f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = false, 13f, 32f)
            close()
        }
        // coin slot
        path(fill = SolidColor(Color(0xFF0C7A5A))) {
            moveTo(27f, 12f); lineTo(37f, 12f); lineTo(37f, 16f); lineTo(27f, 16f); close()
        }
        // legs
        path(fill = SolidColor(Color(0xFF0C7A5A))) {
            moveTo(12f, 52f); lineTo(18f, 52f); lineTo(18f, 58f); lineTo(12f, 58f); close()
        }
        path(fill = SolidColor(Color(0xFF0C7A5A))) {
            moveTo(46f, 52f); lineTo(52f, 52f); lineTo(52f, 58f); lineTo(46f, 58f); close()
        }
    }.build()
}
