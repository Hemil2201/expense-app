package com.expensesplitter.app.ui.screens.receipt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.PendingReceiptDraftHolder
import com.expensesplitter.app.data.repository.ReceiptRepository
import java.io.File
import kotlinx.coroutines.launch

data class ReceiptScanUiState(val isScanning: Boolean = false, val error: String? = null)

class ReceiptScanViewModel(
    private val receiptRepository: ReceiptRepository,
    private val pendingReceiptDraftHolder: PendingReceiptDraftHolder,
) : ViewModel() {
    var state by mutableStateOf(ReceiptScanUiState())
        private set

    fun scan(file: File, mimeType: String, onScanned: () -> Unit) {
        state = state.copy(isScanning = true, error = null)
        viewModelScope.launch {
            try {
                val draft = receiptRepository.scan(file, mimeType)
                pendingReceiptDraftHolder.draft = draft
                state = state.copy(isScanning = false)
                onScanned()
            } catch (e: Exception) {
                state = state.copy(isScanning = false, error = e.message ?: "Failed to scan receipt")
            }
        }
    }
}
