package com.expensesplitter.app.ui.screens.insights

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.BudgetRepository
import com.expensesplitter.app.data.repository.ReportRepository
import com.expensesplitter.app.ui.components.BarChart
import com.expensesplitter.app.ui.components.BarSegment
import com.expensesplitter.app.ui.components.BarValue
import com.expensesplitter.app.ui.components.SegmentedBar
import com.expensesplitter.app.ui.components.donutSlicesFor
import com.expensesplitter.app.ui.components.DonutChart
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.Spacing
import java.time.Month
import kotlinx.coroutines.launch

@Composable
fun InsightsScreen(reportRepository: ReportRepository, budgetRepository: BudgetRepository) {
    val viewModel: InsightsViewModel = viewModel(
        factory = viewModelFactory { initializer { InsightsViewModel(reportRepository, budgetRepository) } },
    )
    val state = viewModel.state
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.changeMonth(-1) }) { Text("<") }
            Text(
                "${Month.of(state.month).name.lowercase().replaceFirstChar { it.uppercase() }} ${state.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
            IconButton(onClick = { viewModel.changeMonth(1) }) { Text(">") }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        state.error?.let { Text("Couldn't load insights: $it", color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                coroutineScope.launch {
                    val file = reportRepository.downloadCsv(state.month, state.year, context.cacheDir)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share expenses CSV"))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export CSV") }

        SectionTitle("Category Breakdown")
        if (state.byCategory.isEmpty()) {
            EmptyChartMessage("No expenses this month")
        } else {
            DonutChart(
                slices = donutSlicesFor(state.byCategory.map { it.categoryName to (it.total.toDoubleOrNull() ?: 0.0) }),
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            )
        }
        Divider(modifier = Modifier.padding(vertical = Spacing.lg))

        SectionTitle("Spend Over Time")
        if (state.trend.isEmpty()) {
            EmptyChartMessage("Not enough data yet")
        } else {
            BarChart(
                values = state.trend.map { m ->
                    BarValue(
                        Month.of(m.month).name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                        m.total.toDoubleOrNull() ?: 0.0,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            )
        }
        Divider(modifier = Modifier.padding(vertical = Spacing.lg))

        SectionTitle("Personal vs Shared")
        state.report?.let { report ->
            SegmentedBar(
                segments = listOf(
                    BarSegment("Personal", report.personalSpend.toDoubleOrNull() ?: 0.0, BalanceColors.positiveLight),
                    BarSegment("Shared", report.sharedSpend.toDoubleOrNull() ?: 0.0, BalanceColors.negativeLight),
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            )
        }
        Divider(modifier = Modifier.padding(vertical = Spacing.lg))

        SectionTitle("Budget vs Actual")
        val budgetRows = state.budgets.flatMap { category ->
            val personalRows = category.personal.filter { it.targetAmount != null }
                .map { "${category.categoryName} · ${it.name}" to it }
            val groupRow = if (category.group.targetAmount != null) {
                listOf("${category.categoryName} · Group" to category.group)
            } else {
                emptyList()
            }
            personalRows + groupRow
        }
        if (budgetRows.isEmpty()) {
            EmptyChartMessage("No budget targets set — add one from the Budgets tab")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                budgetRows.forEach { (label, line) ->
                    val target = when (line) {
                        is com.expensesplitter.app.data.repository.PersonalBudgetLine -> line.targetAmount
                        is com.expensesplitter.app.data.repository.GroupBudgetLine -> line.targetAmount
                        else -> null
                    }
                    val actual = when (line) {
                        is com.expensesplitter.app.data.repository.PersonalBudgetLine -> line.actualSpend
                        is com.expensesplitter.app.data.repository.GroupBudgetLine -> line.actualSpend
                        else -> "0"
                    }
                    BudgetComparisonRow(label = label, target = target, actual = actual)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyChartMessage(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BudgetComparisonRow(label: String, target: String?, actual: String) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$actual / $target", style = MaterialTheme.typography.bodySmall)
        }
        val targetValue = target?.toDoubleOrNull()
        val actualValue = actual.toDoubleOrNull() ?: 0.0
        if (targetValue != null && targetValue > 0) {
            val fraction = (actualValue / targetValue).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = if (actualValue > targetValue) BalanceColors.negativeLight else BalanceColors.positiveLight,
            )
        }
    }
}
