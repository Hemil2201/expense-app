package com.expensesplitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensesplitter.app.ui.theme.categoryColorFor

// Name -> vector icon for the known category set. Raw emoji from the backend
// (category.icon) render inconsistently — some glyphs are plain pictographs,
// others ("button" emoji like the recycle/refresh symbol) bake in their own
// colored-square art in most emoji fonts, so mixing them reads as visually
// broken rather than intentional. A curated vector set keeps every category
// badge the same weight, size, and style. Unrecognized category names (this
// app isn't scoped to a fixed list — more may be added later) fall back to
// their initial letter rather than guessing an icon.
private val CATEGORY_ICONS: Map<String, ImageVector> = mapOf(
    "eating out" to Icons.Filled.Restaurant,
    "dining" to Icons.Filled.Restaurant,
    "restaurants" to Icons.Filled.Restaurant,
    "entertainment" to Icons.Filled.Movie,
    "groceries" to Icons.Filled.ShoppingCart,
    "health" to Icons.Filled.LocalHospital,
    "healthcare" to Icons.Filled.LocalHospital,
    "other" to Icons.Filled.Category,
    "rent/utilities" to Icons.Filled.Home,
    "rent" to Icons.Filled.Home,
    "utilities" to Icons.Filled.Home,
    "housing" to Icons.Filled.Home,
    "shopping" to Icons.Filled.ShoppingBag,
    "subscriptions" to Icons.Filled.Autorenew,
    "transport" to Icons.Filled.DirectionsCar,
    "transportation" to Icons.Filled.DirectionsCar,
    "travel" to Icons.Filled.Flight,
    "education" to Icons.Filled.School,
)

private fun iconFor(categoryName: String): ImageVector? = CATEGORY_ICONS[categoryName.trim().lowercase()]

// Colored circular badge for a category — a curated vector icon when the
// category name is recognized, otherwise its initial letter.
@Composable
fun CategoryIcon(
    categoryName: String,
    icon: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val color = categoryColorFor(categoryName)
    val vectorIcon = iconFor(categoryName)
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (vectorIcon != null) {
            Icon(
                vectorIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.55f),
            )
        } else {
            Text(
                text = categoryName.take(1).uppercase(),
                fontSize = (size.value * 0.42).sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}
