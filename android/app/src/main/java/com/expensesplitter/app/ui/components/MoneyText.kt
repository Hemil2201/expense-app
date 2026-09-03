package com.expensesplitter.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

// Counts up/down to a new balance/amount instead of snapping — the kind of
// small motion real fintech apps (Cash App, Revolut) use so a refreshed
// number feels alive rather than a jarring re-render. Pure UI-layer
// animateFloatAsState — no ViewModel changes needed.
@Composable
fun AnimatedMoneyText(
    value: Double,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    prefix: String = "$",
    textAlign: TextAlign? = null,
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "animated-money",
    )
    androidx.compose.material3.Text(
        text = prefix + "%,.2f".format(animated),
        style = style,
        color = color,
        modifier = modifier,
        textAlign = textAlign,
    )
}
