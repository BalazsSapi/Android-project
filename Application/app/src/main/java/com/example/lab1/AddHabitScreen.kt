package com.example.lab1

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lab1.network.HabitRepository
import com.example.lab1.network.models.HabitCategory
import com.example.lab1.utils.ImageUrls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddHabitUiState(
    val name: String = "",
    val description: String = "",
    val goal: String = "",
    val categories: List<HabitCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoadingCategories: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class AddHabitViewModel(application: Application) : AndroidViewModel(application) {

    private val habitRepository = HabitRepository()

    private val _uiState = MutableStateFlow(AddHabitUiState())
    val uiState: StateFlow<AddHabitUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _uiState.update { it.copy(isLoadingCategories = true, error = null) }
        viewModelScope.launch {
            habitRepository.getCategories()
                .onSuccess { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategoryId = state.selectedCategoryId
                                ?: categories.firstOrNull()?.id,
                            isLoadingCategories = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            error = throwable.message ?: "Failed to load categories"
                        )
                    }
                }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, error = null) }
    }

    fun onGoalChange(value: String) {
        _uiState.update { it.copy(goal = value, error = null) }
    }

    fun onCategorySelected(id: Long) {
        _uiState.update { it.copy(selectedCategoryId = id, error = null) }
    }

    fun createHabit() {
        val state = _uiState.value
        if (state.isSaving) return

        val validationError = when {
            state.name.isBlank() -> "Habit name is required"
            state.selectedCategoryId == null -> "Choose a category"
            state.goal.isBlank() -> "Goal is required"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            habitRepository.createHabit(
                name = state.name.trim(),
                description = state.description.trim(),
                categoryId = state.selectedCategoryId!!,
                goal = state.goal.trim()
            )
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Failed to create habit"
                        )
                    }
                }
        }
    }
}

@Composable
fun AddHabitScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToCreateSchedule: () -> Unit,
    viewModel: AddHabitViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, "Habit created", Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Column {
                Text("Add habit", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Create a new habit to start tracking.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.goal,
            onValueChange = viewModel::onGoalChange,
            label = { Text("Goal") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Category", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.categories, key = { it.id }) { category ->
                // A kategória ikonját a backend iconUrl mezőjéből töltjük be (ha van)
                val iconUrl = ImageUrls.resolve(category.iconUrl)
                FilterChip(
                    selected = uiState.selectedCategoryId == category.id,
                    onClick = { viewModel.onCategorySelected(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = if (iconUrl != null) {
                        {
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }

        Button(
            onClick = viewModel::createHabit,
            enabled = !uiState.isSaving && !uiState.isLoadingCategories,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create habit")
        }

        TextButton(
            onClick = onNavigateToCreateSchedule,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Schedule an existing habit")
        }
    }
}
