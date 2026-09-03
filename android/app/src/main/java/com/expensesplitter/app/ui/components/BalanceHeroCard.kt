package com.expensesplitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.data.repository.UserBalance
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.HeroShapes
import com.expensesplitter.app.ui.theme.Ink
import com.expensesplitter.app.ui.theme.InkElevated
import com.expensesplitter.app.ui.theme.MoneyType
import com.expensesplitter.app.ui.theme.OnInk
import com.expensesplitter.app.ui.theme.Spacing
import java.math.BigDecimal

// The Cash App move: the single most important number on the Home screen
// sits on a bold near-black card instead of blending into the same white
// surface as everything else — dark navy (#0F172A), scoped to just this
// hero card so the rest of the app stays light. The amount itself counts
// up/down via AnimatedMoneyText rather than snapping when it changes.
@Composable
fun BalanceHeroCard(
    sessionUserName: String?,
    balances: List<UserBalance>,
    modifier: Modifier = Modifier,
) {
    val mine = balances.find { it.name == sessionUserName }
    val other = balances.find { it.name != sessionUserName }
    val amount = mine?.netBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isPositive = amount.signum() > 0
    val isSettled = amount.signum() == 0

    val (headline, accentColor, icon) = when {
        isSettled -> Triple("You're all settled up", OnInk, Icons.Filled.CheckCircle)
        isPositive -> Triple("${other?.name ?: "They"} owes you", BalanceColors.positiveOnInk, Icons.Filled.SouthWest)
        else -> Triple("You owe ${other?.name ?: "them"}", BalanceColors.negativeOnInk, Icons.Filled.NorthEast)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = HeroShapes.heroCard,
        color = Ink,
    ) {
        Row(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(InkElevated, Ink)),
                )
                .padding(Spacing.lg + Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SHARED BALANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnInk.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnInk,
                )
                Spacer(Modifier.height(Spacing.xs))
                if (!isSettled) {
                    AnimatedMoneyText(
                        value = amount.abs().toDouble(),
                        style = MoneyType.large,
                        color = accentColor,
                    )
                } else {
                    Text("$0.00", style = MoneyType.large, color = accentColor)
                }
            }
            Box(
                modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
            }
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = try {
    BigDecimal(this)
} catch (e: NumberFormatException) {
    null
}
