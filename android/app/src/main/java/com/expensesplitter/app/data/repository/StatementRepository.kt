package com.expensesplitter.app.data.repository

import com.expensesplitter.app.data.remote.ApiService
import com.expensesplitter.app.data.remote.dto.ResolveTransactionDto
import com.expensesplitter.app.data.remote.dto.SplitInputDto
import com.expensesplitter.app.data.remote.dto.StatementTransactionDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class StatementUpload(val id: String, val status: String)

data class StatementSummary(
    val id: String,
    val status: String,
    val total: Int,
    val resolved: Int,
    val needsClarification: Int,
    val possibleDuplicates: Int,
    val expectedTotal: Int?,
)

data class StatementTransaction(
    val id: String,
    val rawDate: String?,
    val rawDescription: String?,
    val rawAmount: String?,
    val matchedCategoryId: String?,
    val needsClarification: Boolean,
    val userClarificationNote: String?,
    val isDuplicateOf: String?,
    val resolvedExpenseId: String?,
)

class StatementRepository(private val apiService: ApiService) {

    suspend fun uploadStatement(file: File, mimeType: String, bankName: String?, cardLast4: String?): StatementUpload {
        val filePart = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody(mimeType.toMediaTypeOrNull()),
        )
        val bankNamePart = bankName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val cardLast4Part = cardLast4?.toRequestBody("text/plain".toMediaTypeOrNull())
        val dto = apiService.uploadStatement(filePart, bankNamePart, cardLast4Part)
        return StatementUpload(dto.id, dto.status)
    }

    suspend fun getSummary(id: String): StatementSummary {
        val dto = apiService.getStatement(id)
        return StatementSummary(
            dto.id, dto.status, dto.total, dto.resolved, dto.needs_clarification, dto.possible_duplicates, dto.expected_total,
        )
    }

    suspend fun getTransactions(id: String): List<StatementTransaction> =
        apiService.getStatementTransactions(id).map { it.toDomain() }

    // Returns the updated transaction straight from the resolve response
    // instead of discarding it — the caller can apply it to local state
    // immediately without a follow-up fetch just to see what changed.
    suspend fun resolveTransaction(
        transactionId: String,
        categoryId: String?,
        paidBy: String?,
        isShared: Boolean,
        splitType: String?,
        splitValues: Map<String, String>?,
        clarificationNote: String?,
        confirmDuplicate: Boolean,
    ): StatementTransaction {
        val splits = splitValues?.map { (userId, value) -> SplitInputDto(userId, value) }
        val dto = apiService.resolveTransaction(
            transactionId,
            ResolveTransactionDto(
                category_id = categoryId,
                paid_by = paidBy,
                is_shared = isShared,
                split_type = splitType,
                splits = splits,
                user_clarification_note = clarificationNote,
                confirm_duplicate = confirmDuplicate,
            ),
        )
        return dto.toDomain()
    }

    private fun StatementTransactionDto.toDomain() = StatementTransaction(
        id = id,
        rawDate = raw_date,
        rawDescription = raw_description,
        rawAmount = raw_amount,
        matchedCategoryId = matched_category_id,
        needsClarification = needs_clarification,
        userClarificationNote = user_clarification_note,
        isDuplicateOf = is_duplicate_of,
        resolvedExpenseId = resolved_expense_id,
    )
}
