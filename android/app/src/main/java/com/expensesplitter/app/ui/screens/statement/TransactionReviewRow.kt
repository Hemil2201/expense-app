package com.expensesplitter.app.ui.screens.statement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.CurrentUser
import com.expensesplitter.app.data.repository.StatementTransaction
import com.expensesplitter.app.ui.components.CategoryIcon
import com.expensesplitter.app.ui.screens.expense.SPLIT_TYPES
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionReviewRow(
    txn: StatementTransaction,
    categories: List<Category>,
    users: List<CurrentUser>,
    viewModel: StatementUploadViewModel,
) {
    if (txn.resolvedExpenseId != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BalanceColors.positiveContainerLight.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BalanceColors.positiveLight)
                    Column {
                        Text(
                            txn.rawDescription ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${txn.rawDate} · resolved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "${txn.rawAmount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
        return
    }

    var selectedCategoryId by remember { mutableStateOf(txn.matchedCategoryId) }
    var isShared by remember { mutableStateOf(false) }
    var splitType by remember { mutableStateOf("equal") }
    var splitValues by remember { mutableStateOf(mapOf<String, String>()) }
    var clarificationNote by remember { mutableStateOf(txn.userClarificationNote ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        txn.rawDescription ?: "(no description)",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${txn.rawDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${txn.rawAmount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }

            if (txn.isDuplicateOf != null) {
                Text(
                    "Possible duplicate of an existing expense",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(onClick = {
                        viewModel.resolve(txn.id, null, null, false, null, null, null, true) { error = it }
                    }) { Text("It's a duplicate — skip") }
                    OutlinedButton(onClick = {
                        // User says it's not actually a duplicate — fall through to normal resolve below.
                        viewModel.resolve(
                            txn.id, selectedCategoryId, null, isShared,
                            if (isShared) splitType else null,
                            if (isShared && splitType != "equal") splitValues else null,
                            clarificationNote.ifBlank { null }, false,
                        ) { error = it }
                    }) { Text("Not a duplicate") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                return@Column
            }

            if (txn.needsClarification) {
                Text(
                    "Needs clarification — what was this charge for?",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = clarificationNote,
                    onValueChange = { clarificationNote = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val selectedCategory = categories.find { it.id == selectedCategoryId }
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedCategory?.name ?: "Select category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = {
                        CategoryIcon(categoryName = selectedCategory?.name ?: "?", icon = selectedCategory?.icon, size = 28.dp)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            leadingIcon = { CategoryIcon(categoryName = category.name, icon = category.icon, size = 28.dp) },
                            text = { Text(category.name) },
                            onClick = { selectedCategoryId = category.id; categoryMenuExpanded = false },
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Shared expense")
                Switch(checked = isShared, onCheckedChange = { isShared = it })
            }

            if (isShared) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SPLIT_TYPES.forEach { type ->
                        FilterChip(
                            selected = splitType == type,
                            onClick = { splitType = type; splitValues = emptyMap() },
                            label = { Text(type.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                if (splitType != "equal") {
                    users.forEach { user ->
                        OutlinedTextField(
                            value = splitValues[user.id] ?: "",
                            onValueChange = { splitValues = splitValues + (user.id to it) },
                            label = { Text(user.name) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    if (selectedCategoryId == null) {
                        error = "Pick a category first"
                        return@Button
                    }
                    if (txn.needsClarification && clarificationNote.isBlank()) {
                        error = "Add a note before confirming"
                        return@Button
                    }
                    viewModel.resolve(
                        txn.id, selectedCategoryId, null, isShared,
                        if (isShared) splitType else null,
                        if (isShared && splitType != "equal") splitValues else null,
                        clarificationNote.ifBlank { null }, false,
                    ) { error = it }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Confirm") }
        }
    }
}
