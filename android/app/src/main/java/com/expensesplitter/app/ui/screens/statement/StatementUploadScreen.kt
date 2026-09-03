package com.expensesplitter.app.ui.screens.statement

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.StatementRepository
import com.expensesplitter.app.ui.components.MoneyLoadingIndicator
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.Spacing
import java.io.File

@Composable
fun StatementUploadScreen(
    statementRepository: StatementRepository,
    expenseRepository: ExpenseRepository,
    authRepository: AuthRepository,
    onDone: () -> Unit,
) {
    val viewModel: StatementUploadViewModel = viewModel(
        factory = viewModelFactory {
            initializer { StatementUploadViewModel(statementRepository, expenseRepository, authRepository) }
        },
    )
    val state = viewModel.state
    val context = LocalContext.current

    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        var displayName = "statement"
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) displayName = cursor.getString(nameIndex)
        }
        val mimeType = resolver.getType(uri) ?: if (displayName.endsWith(".pdf")) "application/pdf" else "text/csv"
        val tempFile = File(context.cacheDir, displayName)
        resolver.openInputStream(uri)?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        viewModel.selectFile(tempFile, mimeType, displayName)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is StatementFlowState.PickFile -> PickFileContent { pickFileLauncher.launch(arrayOf("*/*")) }
            is StatementFlowState.FileSelected -> FileSelectedContent(s.displayName, onUpload = viewModel::upload)
            is StatementFlowState.Uploading -> LoadingContent("Uploading…")
            is StatementFlowState.Processing -> ProcessingContent(processed = s.processed, expectedTotal = s.expectedTotal)
            is StatementFlowState.Review -> ReviewContent(s, viewModel)
            is StatementFlowState.Done -> DoneContent(s.summary.total, onDone)
            is StatementFlowState.Error -> ErrorContent(s.message, viewModel::reset)
        }
    }
}

@Composable
private fun PickFileContent(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.UploadFile,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Pick a CSV or PDF export from any of your cards. We'll parse it, suggest categories, " +
                "and flag anything that needs a closer look before it becomes real expenses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) { Text("Choose File") }
    }
}

@Composable
private fun FileSelectedContent(fileName: String, onUpload: (bankName: String?, cardLast4: String?) -> Unit) {
    var bankName by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text("Selected: $fileName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Bank name (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = cardLast4,
            onValueChange = { cardLast4 = it },
            label = { Text("Card last 4 digits (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onUpload(bankName.ifBlank { null }, cardLast4.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Upload") }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoneyLoadingIndicator()
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProcessingContent(processed: Int, expectedTotal: Int?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (expectedTotal == null || expectedTotal == 0) {
            // Before parsing has determined how many transactions there
            // are yet (e.g. still extracting text from a PDF) — no count to
            // show progress against.
            MoneyLoadingIndicator()
            Text(
                "Reading statement…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                progress = { processed.toFloat() / expectedTotal },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Categorizing $processed of $expectedTotal transactions…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Running rules-based matching, with an AI fallback for anything unclear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DoneContent(total: Int, onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        ) {
            Box(
                modifier = Modifier.size(88.dp).background(BalanceColors.positiveLight.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = BalanceColors.positiveLight,
                )
            }
        }
        Text("All done!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("$total transaction(s) from this statement are now in your expenses.")
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to Dashboard") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
    }
}

@Composable
private fun ReviewContent(review: StatementFlowState.Review, viewModel: StatementUploadViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text("Review Transactions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val statusParts = buildList {
                add("${review.summary.resolved}/${review.summary.total} resolved")
                if (review.summary.needsClarification > 0) add("${review.summary.needsClarification} need clarification")
                if (review.summary.possibleDuplicates > 0) add("${review.summary.possibleDuplicates} possible duplicates")
            }
            Text(
                statusParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(review.transactions, key = { it.id }) { txn ->
                TransactionReviewRow(txn, review.categories, review.users, viewModel)
            }
        }
    }
}
