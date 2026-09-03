package com.expensesplitter.app.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.CurrentUser
import com.expensesplitter.app.ui.util.detailMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class LoginUiState {
    data object LoadingUsers : LoginUiState()
    data class PickUser(val users: List<CurrentUser>) : LoginUiState()
    data class EnterPin(val user: CurrentUser, val error: String? = null, val isSubmitting: Boolean = false) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    var uiState by mutableStateOf<LoginUiState>(LoginUiState.LoadingUsers)
        private set

    init {
        loadUsers()
    }

    fun loadUsers() {
        uiState = LoginUiState.LoadingUsers
        viewModelScope.launch {
            uiState = try {
                LoginUiState.PickUser(authRepository.getLoginUsers())
            } catch (e: Exception) {
                LoginUiState.Error(e.message ?: "Could not reach the backend")
            }
        }
    }

    fun selectUser(user: CurrentUser) {
        uiState = LoginUiState.EnterPin(user)
    }

    fun backToUserPicker() {
        loadUsers()
    }

    fun login(pin: String, onLoggedIn: () -> Unit) {
        val current = uiState as? LoginUiState.EnterPin ?: return
        uiState = current.copy(error = null, isSubmitting = true)
        viewModelScope.launch {
            try {
                authRepository.login(current.user.id, pin)
                onLoggedIn()
            } catch (e: HttpException) {
                uiState = current.copy(error = e.detailMessage(), isSubmitting = false)
            } catch (e: Exception) {
                uiState = current.copy(error = e.message ?: "Incorrect PIN", isSubmitting = false)
            }
        }
    }
}
