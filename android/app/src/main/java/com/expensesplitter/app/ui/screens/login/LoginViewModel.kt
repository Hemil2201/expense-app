package com.expensesplitter.app.ui.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.CurrentUser
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object LoadingUsers : LoginUiState()
    data class PickUser(val users: List<CurrentUser>) : LoginUiState()
    data object LoggingIn : LoginUiState()
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

    fun login(userId: String, onLoggedIn: () -> Unit) {
        uiState = LoginUiState.LoggingIn
        viewModelScope.launch {
            try {
                authRepository.login(userId)
                onLoggedIn()
            } catch (e: Exception) {
                uiState = LoginUiState.Error(e.message ?: "Login failed")
            }
        }
    }
}
