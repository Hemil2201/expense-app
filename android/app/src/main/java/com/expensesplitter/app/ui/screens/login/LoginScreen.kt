package com.expensesplitter.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.AuthRepository

@Composable
fun LoginScreen(authRepository: AuthRepository, onLoggedIn: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory { initializer { LoginViewModel(authRepository) } },
    )
    val state = viewModel.uiState

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Expense Splitter",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Who's using the app?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (state) {
                is LoginUiState.LoadingUsers, is LoginUiState.LoggingIn -> {
                    CircularProgressIndicator()
                }
                is LoginUiState.PickUser -> {
                    state.users.forEach { user ->
                        Button(
                            onClick = { viewModel.login(user.id, onLoggedIn) },
                            modifier = Modifier.fillMaxWidth(0.7f),
                        ) { Text(user.name, style = MaterialTheme.typography.titleMedium) }
                    }
                }
                is LoginUiState.Error -> {
                    Text("Couldn't reach the backend:", color = MaterialTheme.colorScheme.error)
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { viewModel.loadUsers() }) { Text("Retry") }
                }
            }
        }
    }
}
