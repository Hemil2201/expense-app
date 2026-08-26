package com.expensesplitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BarValue(val label: String, val value: Double)

// A simple hand-rolled vertical bar chart (not a charting library — see
// InsightsScreen for why) for month-over-month trend data. Bars are drawn
// with proportional height relative to the largest value in the series.
@Composable
fun BarChart(values: List<BarValue>, modifier: Modifier = Modifier, barColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    val maxValue = values.maxOfOrNull { it.value }?.coerceAtLeast(0.01) ?: 0.01
    Row(
        modifier = modifier.height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { bar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val fraction = (bar.value / maxValue).toFloat().coerceIn(0.02f, 1f)
                Text(
                    "%.0f".format(bar.value),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((120 * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor),
                ) {}
                Text(bar.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
