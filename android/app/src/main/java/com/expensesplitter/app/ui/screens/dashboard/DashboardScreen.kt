package com.expensesplitter.app.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.ui.components.ActionRow
import com.expensesplitter.app.ui.components.BalanceHeroCard
import com.expensesplitter.app.ui.components.CategoryIcon
import com.expensesplitter.app.ui.theme.Spacing

@Composable
fun DashboardScreen(
    authRepository: AuthRepository,
    expenseRepository: ExpenseRepository,
    onUploadStatement: () -> Unit,
    onScanReceipt: () -> Unit,
    onViewRecurring: () -> Unit,
    onViewActivity: () -> Unit,
    onOpenCategory: (categoryId: String) -> Unit,
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardViewModel(expenseRepository) } },
    )
    val state = viewModel.state
    val sessionUser = authRepository.getSessionUser()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = "Hi, ${sessionUser?.name ?: "there"}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            BalanceHeroCard(sessionUserName = sessionUser?.name, balances = state.balances)

            Column {
                ActionRow(label = "Upload Statement", icon = Icons.Filled.UploadFile, onClick = onUploadStatement)
                Divider()
                ActionRow(label = "Scan a Receipt", icon = Icons.Filled.CameraAlt, onClick = onScanReceipt)
                Divider()
                ActionRow(label = "Recurring Expenses", icon = Icons.Filled.Autorenew, onClick = onViewRecurring)
                Divider()
                ActionRow(label = "View Activity", icon = Icons.Filled.History, onClick = onViewActivity)
            }

            if (state.categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    CategoryGrid(categories = state.categories, onOpenCategory = onOpenCategory)
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<Category>, onOpenCategory: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        categories.chunked(4).forEach { rowCategories ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowCategories.forEach { category ->
                    CategoryCard(category = category, onClick = { onOpenCategory(category.id) }, modifier = Modifier.weight(1f))
                }
                repeat(4 - rowCategories.size) { Row(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        CategoryIcon(categoryName = category.name, icon = category.icon, size = 48.dp)
        Text(
            category.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
