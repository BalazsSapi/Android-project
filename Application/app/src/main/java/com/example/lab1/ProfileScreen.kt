package com.example.lab1

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab1.network.AuthRepository
import com.example.lab1.network.HabitRepository
import com.example.lab1.network.ProfileRepository
import com.example.lab1.network.models.HabitResponse
import com.example.lab1.network.models.ProfileDto
import com.example.lab1.utils.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userId: Long = -1,
    val username: String? = null,
    val email: String? = null,
    // GET /profile válasza és a felhasználó habitjei (GET /habit/user/{userId})
    val profile: ProfileDto? = null,
    val habits: List<HabitResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)
    private val profileRepository = ProfileRepository()
    private val habitRepository = HabitRepository()
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(loadSession())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Először a mentett session adatai látszanak, utána frissül a backendről
    fun refresh() {
        _uiState.update {
            it.copy(
                userId = sessionManager.userId,
                username = sessionManager.username,
                email = sessionManager.email,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val userId = sessionManager.userId
            val profileDeferred = async { profileRepository.getProfile() }
            val habitsDeferred = async {
                if (userId != -1L) {
                    habitRepository.getHabitsByUser(userId)
                } else {
                    Result.success(emptyList<HabitResponse>())
                }
            }

            val profileResult = profileDeferred.await()
            val habitsResult = habitsDeferred.await()

            // A session-ben tárolt név/email is frissüljön
            profileResult.getOrNull()?.let { sessionManager.updateUser(it.email, it.username) }

            val error = profileResult.exceptionOrNull()?.message
                ?: habitsResult.exceptionOrNull()?.message

            _uiState.update {
                val profile = profileResult.getOrNull() ?: it.profile
                it.copy(
                    isLoading = false,
                    profile = profile,
                    username = profile?.username ?: it.username,
                    email = profile?.email ?: it.email,
                    habits = habitsResult.getOrDefault(it.habits),
                    error = error
                )
            }
        }
    }

    // POST /auth/local/logout, majd a session törlése (a hiba nem akadályozza a kilépést)
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            sessionManager.clear()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    private fun loadSession() = ProfileUiState(
        userId = sessionManager.userId,
        username = sessionManager.username,
        email = sessionManager.email
    )
}

@Composable
fun ProfileScreen(
    onAddHabit: () -> Unit,
    onEditProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var menuExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLoggedOut()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile Screen", style = MaterialTheme.typography.headlineMedium)
                // Opciók menü: kijelentkezés megerősítéssel
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Log out") },
                            onClick = {
                                menuExpanded = false
                                showLogoutDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        // Profil összegzés
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileAvatar(
                        profile = uiState.profile,
                        fallbackName = uiState.username,
                        size = 72.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Name: ${uiState.username ?: "Guest"}")
                        Text("Email: ${uiState.email ?: "-"}")
                        Text("ID: ${uiState.userId}")
                        OutlinedButton(onClick = onEditProfile) { Text("Edit Profile") }
                    }
                }
            }
        }

        // A felhasználó habitjei
        item {
            Text("My Habits", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.habits.isEmpty()) {
            item { Text("No habits yet") }
        } else {
            items(uiState.habits, key = { it.id }) { habit ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(habit.name, style = MaterialTheme.typography.titleSmall)
                        Text(habit.category.name, style = MaterialTheme.typography.bodySmall)
                        habit.goal.takeIf { it.isNotBlank() }?.let {
                            Text("Goal: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Button(onClick = onAddHabit, modifier = Modifier.fillMaxWidth()) {
                Text("Add New Habit")
            }
        }
    }

    // Kijelentkezés megerősítése
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}
