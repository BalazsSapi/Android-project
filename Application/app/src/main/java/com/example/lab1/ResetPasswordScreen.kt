package com.example.lab1

import android.app.Application
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab1.network.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSent: Boolean = false
)

class ResetPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun resetPassword() {
        val state = _uiState.value
        val email = state.email.trim()

        val validationError = when {
            email.isEmpty() -> "Az email megadása kötelező"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Adj meg érvényes email címet"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        // Az új jelszót a backend generálja és emailben küldi el
        viewModelScope.launch {
            repository.resetPassword(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSent = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "A jelszó visszaállítása nem sikerült. Próbáld újra."
                        )
                    }
                }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Jelszó visszaállítása",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.isSent) {
            // A kérés sikeres volt: az új jelszó az emailben van
            Text(
                text = "Az új jelszót elküldtük a megadott email címre. " +
                    "Jelentkezz be vele, és minél előbb változtasd meg.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Vissza a belépéshez")
            }
        } else {
            Text(
                text = "Add meg a regisztrált email címedet, és új jelszót küldünk rá.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isLoading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::resetPassword,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Új jelszó kérése")
                }
            }

            TextButton(
                onClick = onBackToLogin,
                enabled = !uiState.isLoading
            ) {
                Text("Vissza a belépéshez")
            }
        }
    }
}
