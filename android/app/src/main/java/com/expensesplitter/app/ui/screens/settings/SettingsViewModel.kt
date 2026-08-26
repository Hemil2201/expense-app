package com.expensesplitter.app.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.AuthRepository
import java.io.File
import kotlinx.coroutines.launch

data class SettingsUiState(val avatarUrl: String? = null, val isUploading: Boolean = false, val error: String? = null)

class SettingsViewModel(private val authRepository: AuthRepository) : ViewModel() {
    var state by mutableStateOf(SettingsUiState(avatarUrl = authRepository.getSessionUser()?.avatarUrl))
        private set

    fun uploadAvatar(file: File, mimeType: String) {
        state = state.copy(isUploading = true, error = null)
        viewModelScope.launch {
            state = try {
                val user = authRepository.uploadAvatar(file, mimeType)
                state.copy(isUploading = false, avatarUrl = user.avatarUrl)
            } catch (e: Exception) {
                state.copy(isUploading = false, error = e.message ?: "Failed to upload photo")
            }
        }
    }
}
