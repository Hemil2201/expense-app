package com.expensesplitter.app.data.repository

import com.expensesplitter.app.data.remote.ApiService
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

data class ReceiptDraft(
    val date: String?,
    val description: String?,
    val amount: String?,
    val categoryId: String?,
    val receiptPhotoUrl: String,
)

/** Holds the most recent receipt scan result so AddExpenseScreen can pick it
 * up and prefill the form. A simple in-memory holder is enough for a 2-user
 * app — no need for SavedStateHandle plumbing across a multi-field object. */
class PendingReceiptDraftHolder {
    var draft: ReceiptDraft? = null

    fun consume(): ReceiptDraft? = draft.also { draft = null }
}

class ReceiptRepository(private val apiService: ApiService) {
    suspend fun scan(file: File, mimeType: String): ReceiptDraft {
        val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
        val dto = apiService.scanReceipt(part)
        return ReceiptDraft(
            date = dto.date,
            description = dto.description,
            amount = dto.amount,
            categoryId = dto.category_id,
            receiptPhotoUrl = dto.receipt_photo_url,
        )
    }
}
