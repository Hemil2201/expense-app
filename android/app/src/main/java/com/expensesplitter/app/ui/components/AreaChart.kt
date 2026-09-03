package com.expensesplitter.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TrendPoint(val label: String, val value: Double)

// Smooth-curve area chart with a soft gradient fill under the line — the
// standard treatment for "trend over time" data, replacing the previous
// hand-rolled flat bar chart. Points are connected with cubic bezier
// segments (not straight lines) for a smooth-curve look.
@Composable
fun AreaChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val maxValue = (points.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(0.01)
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "area-chart-progress",
    )

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            if (points.size < 2) return@Canvas
            val stepX = size.width / (points.size - 1)
            val coords = points.mapIndexed { i, p ->
                Offset(i * stepX, size.height - (p.value / maxValue).toFloat() * size.height * 0.92f)
            }

            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val current = coords[i]
                    val next = coords[i + 1]
                    val midX = (current.x + next.x) / 2
                    cubicTo(midX, current.y, midX, next.y, next.x, next.y)
                }
            }

            // alpha-fades in on first draw rather than clip-revealing —
            // simpler and avoids needing a path-measure-based trim.
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, size.height)
                lineTo(coords.first().x, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f * progress), lineColor.copy(alpha = 0.02f * progress)),
                    startY = 0f,
                    endY = size.height,
                ),
            )
            drawPath(path = linePath, color = lineColor.copy(alpha = progress), style = Stroke(width = 7f, cap = StrokeCap.Round))
            coords.forEachIndexed { i, c ->
                if (i == coords.size - 1) {
                    drawCircle(color = lineColor.copy(alpha = progress), radius = 10f, center = c)
                    drawCircle(color = Color.White.copy(alpha = progress), radius = 4f, center = c)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { p ->
                Text(p.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

