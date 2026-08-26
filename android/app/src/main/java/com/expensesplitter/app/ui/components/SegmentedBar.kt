package com.expensesplitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.ui.theme.Spacing

data class BarSegment(val label: String, val value: Double, val color: Color)

// A single flat proportional bar split into colored segments, with a legend
// row underneath. Used for two-value comparisons (e.g. personal vs shared)
// where a full axis chart would be overkill.
@Composable
fun SegmentedBar(segments: List<BarSegment>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.value }.coerceAtLeast(0.01)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp)),
        ) {
            segments.forEach { segment ->
                val weight = (segment.value / total).toFloat().coerceAtLeast(0.001f)
                Row(modifier = Modifier.weight(weight).fillMaxWidth().background(segment.color)) {}
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            segments.forEach { segment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(segment.color, CircleShape))
                    Column(modifier = Modifier.padding(start = Spacing.xs)) {
                        Text(segment.label, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$" + "%.2f".format(segment.value),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
