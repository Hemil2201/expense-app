package com.expensesplitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensesplitter.app.ui.theme.categoryColorFor

// Colored circular badge for a category — uses the category's own emoji icon
// (from the backend) when present, otherwise falls back to its initial.
@Composable
fun CategoryIcon(
    categoryName: String,
    icon: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    val color = categoryColorFor(categoryName)
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon?.takeIf { it.isNotBlank() } ?: categoryName.take(1).uppercase(),
            fontSize = (size.value * 0.5).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
