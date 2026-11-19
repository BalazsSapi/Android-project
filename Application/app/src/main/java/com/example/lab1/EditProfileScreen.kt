package com.example.lab1

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.lab1.network.ProfileRepository
import com.example.lab1.network.models.ProfileDto
import com.example.lab1.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EditProfileUiState(
    val profile: ProfileDto? = null,
    val username: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isSaved: Boolean = false
)

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository()
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    // GET /profile
    private fun loadProfile() {
        viewModelScope.launch {
            repository.getProfile()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            username = profile.username.orEmpty()
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load profile"
                        )
                    }
                }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, error = null) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // Új felhasználónév mentése: PATCH /profile
    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val username = state.username.trim()
        if (username.isEmpty()) {
            _uiState.update { it.copy(error = "Username is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            repository.updateProfile(username)
                .onSuccess { profile ->
                    sessionManager.updateUser(profile.email, profile.username ?: username)
                    _uiState.update { it.copy(isSaving = false, profile = profile, isSaved = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Failed to update profile"
                        )
                    }
                }
        }
    }

    // Profilkép feltöltése: POST /profile/upload-profile-image (multipart)
    fun uploadImage(uri: Uri) {
        if (_uiState.value.isUploading) return
        _uiState.update { it.copy(isUploading = true, error = null) }

        viewModelScope.launch {
            val context = getApplication<Application>()
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            // A kiválasztott képet a cache-be másoljuk, hogy fájlként fel lehessen tölteni
            val copied = withContext(Dispatchers.IO) {
                runCatching {
                    val target = File(context.cacheDir, "profile_upload.$extension")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Cannot read the selected image")
                    target
                }
            }
            val file = copied.getOrNull()
            if (file == null) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        error = copied.exceptionOrNull()?.message ?: "Cannot read the selected image"
                    )
                }
                return@launch
            }

            repository.uploadProfileImage(file, mimeType)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(isUploading = false, profile = profile, message = "Profile photo updated")
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            error = throwable.message ?: "Failed to upload photo"
                        )
                    }
                }
        }
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Képválasztó a galériából (külön engedély nem kell hozzá)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.uploadImage(uri)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Edit Profile", style = MaterialTheme.typography.headlineSmall)
        }

        ProfileAvatar(
            profile = uiState.profile,
            fallbackName = uiState.username,
            size = 96.dp
        )

        OutlinedButton(
            onClick = { imagePicker.launch("image/*") },
            enabled = !uiState.isUploading
        ) {
            if (uiState.isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Change photo")
            }
        }

        // Az email csak megjelenik, nem szerkeszthető
        Text(
            "Email: ${uiState.profile?.email ?: "-"}",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            enabled = !uiState.isLoading && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
