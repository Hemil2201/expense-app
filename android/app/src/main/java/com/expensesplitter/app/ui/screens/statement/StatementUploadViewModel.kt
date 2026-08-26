package com.expensesplitter.app.ui.screens.statement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.CurrentUser
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.StatementRepository
import com.expensesplitter.app.data.repository.StatementSummary
import com.expensesplitter.app.data.repository.StatementTransaction
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class StatementFlowState {
    data object PickFile : StatementFlowState()
    data class FileSelected(val file: File, val mimeType: String, val displayName: String) : StatementFlowState()
    data object Uploading : StatementFlowState()
    data class Processing(val statementId: String, val processed: Int = 0, val expectedTotal: Int? = null) : StatementFlowState()
    data class Review(
        val statementId: String,
        val summary: StatementSummary,
        val transactions: List<StatementTransaction>,
        val categories: List<Category>,
        val users: List<CurrentUser>,
    ) : StatementFlowState()
    data class Done(val summary: StatementSummary) : StatementFlowState()
    data class Error(val message: String) : StatementFlowState()
}

class StatementUploadViewModel(
    private val statementRepository: StatementRepository,
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    var state by mutableStateOf<StatementFlowState>(StatementFlowState.PickFile)
        private set

    fun selectFile(file: File, mimeType: String, displayName: String) {
        state = StatementFlowState.FileSelected(file, mimeType, displayName)
    }

    fun reset() {
        state = StatementFlowState.PickFile
    }

    fun upload(bankName: String?, cardLast4: String?) {
        val selected = state as? StatementFlowState.FileSelected ?: return
        state = StatementFlowState.Uploading
        viewModelScope.launch {
            try {
                val upload = statementRepository.uploadStatement(selected.file, selected.mimeType, bankName, cardLast4)
                state = StatementFlowState.Processing(upload.id)
                pollUntilReady(upload.id)
            } catch (e: Exception) {
                state = StatementFlowState.Error(e.message ?: "Upload failed")
            }
        }
    }

    private suspend fun pollUntilReady(statementId: String) {
        while (true) {
            val summary = try {
                statementRepository.getSummary(statementId)
            } catch (e: Exception) {
                state = StatementFlowState.Error(e.message ?: "Failed to check status")
                return
            }
            when (summary.status) {
                "processing" -> {
                    state = StatementFlowState.Processing(statementId, summary.total, summary.expectedTotal)
                    delay(2000)
                }
                "failed" -> {
                    state = StatementFlowState.Error("Couldn't parse this statement — try a different file or format.")
                    return
                }
                else -> {
                    loadReview(statementId, summary)
                    return
                }
            }
        }
    }

    private suspend fun loadReview(statementId: String, summary: StatementSummary) {
        try {
            val transactions = statementRepository.getTransactions(statementId)
            val categories = expenseRepository.getCategories()
            val users = authRepository.getLoginUsers()
            state = if (summary.status == "completed") {
                StatementFlowState.Done(summary)
            } else {
                StatementFlowState.Review(statementId, summary, transactions, categories, users)
            }
        } catch (e: Exception) {
            state = StatementFlowState.Error(e.message ?: "Failed to load review data")
        }
    }

    fun resolve(
        transactionId: String,
        categoryId: String?,
        paidBy: String?,
        isShared: Boolean,
        splitType: String?,
        splitValues: Map<String, String>?,
        clarificationNote: String?,
        confirmDuplicate: Boolean,
        onError: (String) -> Unit,
    ) {
        val current = state as? StatementFlowState.Review ?: return
        viewModelScope.launch {
            val resolveError = try {
                statementRepository.resolveTransaction(
                    transactionId, categoryId, paidBy, isShared, splitType, splitValues, clarificationNote, confirmDuplicate,
                )
                null
            } catch (e: Exception) {
                e.message ?: "Failed to save"
            }

            // Refresh regardless of success/failure — this is a shared
            // statement review (either partner can resolve rows from their
            // own phone), so a failure here may just mean the other person
            // already resolved this exact row. Only surface the error if,
            // after refreshing, this transaction is genuinely still
            // unresolved.
            try {
                val summary = statementRepository.getSummary(current.statementId)
                val transactions = statementRepository.getTransactions(current.statementId)
                loadReview(current.statementId, summary)
                val stillUnresolved = transactions.find { it.id == transactionId }?.resolvedExpenseId == null
                if (resolveError != null && stillUnresolved) onError(resolveError)
            } catch (e: Exception) {
                onError(resolveError ?: e.message ?: "Failed to refresh")
            }
        }
    }
}
