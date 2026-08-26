package com.expensesplitter.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.data.repository.UserBalance
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.Spacing
import java.math.BigDecimal
import kotlin.math.abs

// Splitwise-style balance summary: flat white surface, bordered, with the
// amount itself carrying the color (green = owed to you, red = you owe) —
// not a filled/gradient block.
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

    val (headline, accentColor) = when {
        isSettled -> "You're all settled up" to MaterialTheme.colorScheme.onSurface
        isPositive -> "${other?.name ?: "They"} owes you" to BalanceColors.positiveLight
        else -> "You owe ${other?.name ?: "them"}" to BalanceColors.negativeLight
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(Spacing.lg),
    ) {
        Text(
            text = "SHARED BALANCE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
        )
        if (!isSettled) {
            Text(
                text = "$" + String.format("%.2f", abs(amount.toDouble())),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = try {
    BigDecimal(this)
} catch (e: NumberFormatException) {
    null
}
