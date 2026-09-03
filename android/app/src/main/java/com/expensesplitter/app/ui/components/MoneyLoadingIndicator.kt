package com.expensesplitter.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A loading state themed around the app's subject (money) instead of a
// generic spinner — a breathing "$" mark, built on Compose's
// animate*AsState primitive.
@Composable
fun MoneyLoadingIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val transition = rememberInfiniteTransition(label = "money-loading")
    val scale by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "money-scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "money-alpha",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            "$",
            fontSize = (size.value * 0.6).sp,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = alpha),
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        )
    }
}
