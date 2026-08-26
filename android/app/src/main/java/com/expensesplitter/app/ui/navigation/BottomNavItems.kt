package com.expensesplitter.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

// The five always-visible top-level destinations. Everything else (Add
// Expense, Statement Upload, Receipt Scan, Expense Detail, Activity,
// Recurring, Deleted Expenses) is reached by pushing from one of these, not
// a tab of its own — matches Splitwise's pattern of a few tabs with detail
// flows nested underneath.
val BottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Home", Icons.Filled.Home),
    BottomNavItem(Screen.ExpenseList.route, "Expenses", Icons.Filled.Receipt),
    BottomNavItem(Screen.Budgets.route, "Budgets", Icons.Filled.PieChart),
    BottomNavItem(Screen.Insights.route, "Dashboard", Icons.Filled.Insights),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings),
)
