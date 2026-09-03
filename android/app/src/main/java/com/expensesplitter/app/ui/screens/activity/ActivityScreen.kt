package com.expensesplitter.app.ui.screens.activity

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.ui.components.ActivityRow
import com.expensesplitter.app.ui.components.EmptyState
import com.expensesplitter.app.ui.components.SkeletonList
import com.expensesplitter.app.ui.theme.Spacing

@Composable
fun ActivityScreen(expenseRepository: ExpenseRepository) {
    val viewModel: ActivityViewModel = viewModel(
        factory = viewModelFactory { initializer { ActivityViewModel(expenseRepository) } },
    )
    val state = viewModel.state

    Crossfade(targetState = state.isLoading, animationSpec = tween(250), label = "activity-loading") { loading ->
        when {
            loading -> SkeletonList(rows = 8)
            state.error != null -> Box(Modifier.fillMaxSize()) {
                Text("Couldn't load activity: ${state.error}", modifier = Modifier.padding(24.dp))
            }
            state.items.isEmpty() -> EmptyState(
                icon = Icons.Filled.History,
                title = "Nothing yet",
                message = "Shared activity — added, edited, or deleted expenses — will show up here.",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(state.items) { item ->
                    ActivityRow(item)
                }
            }
        }
    }
}
