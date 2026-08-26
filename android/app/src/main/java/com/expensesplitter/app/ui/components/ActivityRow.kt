package com.expensesplitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.data.repository.ActivityItem
import com.expensesplitter.app.ui.theme.Spacing
import com.expensesplitter.app.ui.theme.categoryColorFor
import com.expensesplitter.app.ui.util.formatActivityTimestamp

@Composable
fun ActivityRow(item: ActivityItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(categoryColorFor(item.userName).copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.userName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = categoryColorFor(item.userName))
        }
        Column {
            Text("${item.userName} ${item.message}", style = MaterialTheme.typography.bodyMedium)
            Text(
                formatActivityTimestamp(item.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
