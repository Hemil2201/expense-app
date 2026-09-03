package com.expensesplitter.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Bento-grid style — rounded-xl to rounded-2xl (16-24px) cards, bumped up
// from the previous 6/10/16/20/28 scale so cards read as soft modular tiles
// rather than boxy Material default rectangles.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

// A few extra sizes beyond M3's 5-slot Shapes for the bigger hero cards.
object HeroShapes {
    val heroCard = RoundedCornerShape(32.dp)
    val pill = RoundedCornerShape(50)
}
