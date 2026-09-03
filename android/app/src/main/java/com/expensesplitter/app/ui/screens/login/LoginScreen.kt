package com.expensesplitter.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.ui.components.PiggyBankLogo
import com.expensesplitter.app.ui.theme.Spacing
import com.expensesplitter.app.ui.theme.categoryColorFor

@Composable
fun LoginScreen(authRepository: AuthRepository, onLoggedIn: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory { initializer { LoginViewModel(authRepository) } },
    )
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF0F172A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = PiggyBankLogo,
                contentDescription = null,
                modifier = Modifier.width(44.dp).height(36.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            "Solvent",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Money, sorted.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            "Who's using the app?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xl))

        when (state) {
            is LoginUiState.LoadingUsers -> {
                CircularProgressIndicator()
            }
            is LoginUiState.PickUser -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    state.users.forEach { user ->
                        UserPickRow(name = user.name, onClick = { viewModel.selectUser(user) })
                    }
                }
            }
            is LoginUiState.EnterPin -> {
                PinEntry(
                    userName = state.user.name,
                    error = state.error,
                    isSubmitting = state.isSubmitting,
                    onBack = { viewModel.backToUserPicker() },
                    onSubmit = { pin -> viewModel.login(pin, onLoggedIn) },
                )
            }
            is LoginUiState.Error -> {
                Text("Couldn't reach the backend:", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(Spacing.xs))
                Text(state.message, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(Spacing.md))
                Button(onClick = { viewModel.loadUsers() }) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun PinEntry(
    userName: String,
    error: String?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Enter $userName's PIN", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(Spacing.md))
        Button(
            onClick = { onSubmit(pin) },
            enabled = pin.isNotEmpty() && !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Log In")
            }
        }
    }
}

@Composable
private fun UserPickRow(name: String, onClick: () -> Unit) {
    val accent = categoryColorFor(name)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp,
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), fontWeight = FontWeight.SemiBold, color = accent)
        }
        Spacer(Modifier.width(Spacing.md))
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    }
}
