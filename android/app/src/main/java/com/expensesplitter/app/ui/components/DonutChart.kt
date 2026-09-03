package com.expensesplitter.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.ui.theme.CategoryPalette
import com.expensesplitter.app.ui.theme.Spacing
import com.expensesplitter.app.ui.theme.categoryColorFor

data class DonutSlice(val label: String, val value: Double, val color: Color)

// Thicker ring, rounded stroke caps, and a total readout centered in the
// hole — the modern "total at a glance" donut pattern used across Copilot
// Money / Apple Wallet-style spend breakdowns, instead of a thin flat ring
// with no center content. Slices sweep in with an animated progress value.
@Composable
fun DonutChart(slices: List<DonutSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut-progress",
    )
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(124.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(124.dp)) {
                if (total <= 0.0) return@Canvas
                var startAngle = -90f
                val strokeWidth = size.minDimension * 0.22f
                slices.forEach { slice ->
                    val sweep = (slice.value / total * 360.0).toFloat() * progress
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    startAngle += (slice.value / total * 360.0).toFloat()
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedMoneyText(
                    value = total,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    prefix = "$",
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            slices.take(5).forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(slice.color, CircleShape))
                    Text(
                        "  ${slice.label}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "$" + "%.0f".format(slice.value),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// categoryColorFor hashes a name to one of 8 palette colors, so two
// categories in the same chart can collide (e.g. two different names both
// landing on "emerald"). Stable per-name color is still nice-to-have
// elsewhere, but within a single chart two adjacent slices must never share
// a color — so any repeat here is bumped to the next unused palette entry.
fun donutSlicesFor(items: List<Pair<String, Double>>): List<DonutSlice> {
    val used = mutableSetOf<Color>()
    return items.map { (name, value) ->
        var color = categoryColorFor(name)
        if (color in used) {
            color = CategoryPalette.firstOrNull { it !in used } ?: color
        }
        used += color
        DonutSlice(name, value, color)
    }
}
