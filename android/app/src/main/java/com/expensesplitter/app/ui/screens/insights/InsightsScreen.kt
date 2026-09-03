package com.expensesplitter.app.ui.screens.insights

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.expensesplitter.app.ui.components.AreaChart
import com.expensesplitter.app.ui.components.BarSegment
import com.expensesplitter.app.ui.components.MonthPager
import com.expensesplitter.app.ui.components.SectionCard
import com.expensesplitter.app.ui.components.SegmentedBar
import com.expensesplitter.app.ui.components.SkeletonBlock
import com.expensesplitter.app.ui.components.TrendPoint
import com.expensesplitter.app.ui.components.donutSlicesFor
import com.expensesplitter.app.ui.components.DonutChart
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.IndigoTertiaryContainerLight
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
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
            }) { Text("Export CSV") }
        }

        MonthPager(month = state.month, year = state.year, onChange = viewModel::changeMonth)

        Crossfade(targetState = state.isLoading, animationSpec = tween(250), label = "insights-loading") { loading ->
            if (loading) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    repeat(3) {
                        SkeletonBlock(Modifier.fillMaxWidth().height(180.dp), shape = RoundedCornerShape(32.dp))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    state.error?.let { Text("Couldn't load insights: $it", color = MaterialTheme.colorScheme.error) }

                    SectionCard("Category Breakdown", tint = MaterialTheme.colorScheme.surfaceContainerLow) {
                        if (state.byCategory.isEmpty()) {
                            EmptyChartMessage("No expenses this month")
                        } else {
                            DonutChart(
                                slices = donutSlicesFor(state.byCategory.map { it.categoryName to (it.total.toDoubleOrNull() ?: 0.0) }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    SectionCard("Spend Over Time", tint = IndigoTertiaryContainerLight.copy(alpha = 0.35f)) {
                        if (state.trend.size < 2) {
                            EmptyChartMessage("Not enough data yet")
                        } else {
                            AreaChart(
                                points = state.trend.map { m ->
                                    TrendPoint(
                                        Month.of(m.month).name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                        m.total.toDoubleOrNull() ?: 0.0,
                                    )
                                },
                                lineColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    SectionCard("Personal vs Shared", tint = MaterialTheme.colorScheme.surfaceContainerLow) {
                        state.report?.let { report ->
                            SegmentedBar(
                                segments = listOf(
                                    BarSegment("Personal", report.personalSpend.toDoubleOrNull() ?: 0.0, BalanceColors.positiveLight),
                                    BarSegment("Shared", report.sharedSpend.toDoubleOrNull() ?: 0.0, BalanceColors.negativeLight),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    SectionCard("Budget vs Actual", tint = MaterialTheme.colorScheme.surfaceContainerLow) {
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

                    Spacer(Modifier.height(Spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun EmptyChartMessage(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BudgetComparisonRow(label: String, target: String?, actual: String) {
    val targetValue = target?.toDoubleOrNull()
    val actualValue = actual.toDoubleOrNull() ?: 0.0
    val isOverBudget = targetValue != null && targetValue > 0 && actualValue > targetValue

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$actual / $target",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOverBudget) BalanceColors.negativeLight else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (targetValue != null && targetValue > 0) {
            val fraction = (actualValue / targetValue).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = if (isOverBudget) BalanceColors.negativeLight else BalanceColors.positiveLight,
            )
        }
    }
}
