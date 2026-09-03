package com.expensesplitter.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.expensesplitter.app.ui.theme.HeroShapes
import com.expensesplitter.app.ui.theme.Spacing

// Bento-grid style card — modular, soft-shadowed, bigger radius than a
// default Material card, and an optional soft tint so each analytics
// section reads as its own distinct tile rather than uniform white blocks.
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = HeroShapes.heroCard,
        color = tint,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            Column(modifier = Modifier.padding(top = Spacing.md)) {
                content()
            }
        }
    }
}
