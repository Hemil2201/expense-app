package com.expensesplitter.app.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.ui.theme.Spacing
import com.expensesplitter.app.ui.theme.categoryColorFor

data class DonutSlice(val label: String, val value: Double, val color: Color)

// Hand-rolled donut (not a Vico chart) — Vico's pie/donut API surface wasn't
// something that could be verified with confidence without risking a broken
// build, and this matches the app's flat visual style exactly for free.
@Composable
fun DonutChart(slices: List<DonutSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                if (total <= 0.0) return@Canvas
                var startAngle = -90f
                val strokeWidth = size.minDimension * 0.28f
                slices.forEach { slice ->
                    val sweep = (slice.value / total * 360.0).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                    )
                    startAngle += sweep
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(slice.color, CircleShape))
                    Text(
                        "  ${slice.label}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text("$" + "%.2f".format(slice.value), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

fun donutSlicesFor(items: List<Pair<String, Double>>): List<DonutSlice> =
    items.map { (name, value) -> DonutSlice(name, value, categoryColorFor(name)) }
