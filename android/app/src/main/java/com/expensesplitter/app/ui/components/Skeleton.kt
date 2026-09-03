package com.expensesplitter.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.ui.theme.Spacing

// Shimmering placeholder block, built on the same animate*AsState approach
// used elsewhere in this theme. Shaped like the content it stands in for,
// so screens don't jump when real data arrives.
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmer-translate",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(translate, 0f),
                    end = Offset(translate + 300f, 300f),
                ),
            ),
    ) {}
}

// Mirrors BalanceHeroCard's layout so the loading state doesn't jump.
@Composable
fun SkeletonBalanceCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.lg),
    ) {
        SkeletonBlock(Modifier.width(110.dp).height(12.dp))
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBlock(Modifier.width(160.dp).height(16.dp))
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBlock(Modifier.width(180.dp).height(36.dp))
    }
}

// Mirrors an ActionRow/ExpenseRow: leading circle + two lines.
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        SkeletonBlock(Modifier.size(40.dp), shape = CircleShape)
        Spacer(Modifier.width(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SkeletonBlock(Modifier.width(160.dp).height(14.dp))
            SkeletonBlock(Modifier.width(100.dp).height(11.dp))
        }
    }
}

@Composable
fun SkeletonList(rows: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {
        repeat(rows) { SkeletonRow() }
    }
}
