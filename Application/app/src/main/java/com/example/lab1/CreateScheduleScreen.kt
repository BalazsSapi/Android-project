package com.example.lab1

import android.app.Application
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.lab1.network.HabitRepository
import com.example.lab1.network.ScheduleRepository
import com.example.lab1.network.models.HabitResponse
import com.example.lab1.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CreateScheduleUiState(
    val habits: List<HabitResponse> = emptyList(),
    val selectedHabitId: Long? = null,
    val date: String = DateTimeUtils.todayDate(),
    val startTime: String = DateTimeUtils.currentTime(),
    val durationMinutes: String = "30",
    val repeatPattern: String = REPEAT_DAILY,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
) {
    companion object {
        const val REPEAT_NONE = "none"
        const val REPEAT_DAILY = "daily"
        const val REPEAT_WEEKDAYS = "weekdays"
        const val REPEAT_WEEKENDS = "weekends"
    }
}

class CreateScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val habitRepository = HabitRepository()
    private val scheduleRepository = ScheduleRepository()

    private val _uiState = MutableStateFlow(CreateScheduleUiState())
    val uiState: StateFlow<CreateScheduleUiState> = _uiState.asStateFlow()

    fun loadHabits() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            habitRepository.getHabits()
                .onSuccess { habits ->
                    _uiState.update { state ->
                        state.copy(
                            habits = habits,
                            selectedHabitId = state.selectedHabitId
                                ?: habits.firstOrNull()?.id,
                            isLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load habits"
                        )
                    }
                }
        }
    }

    fun onHabitSelected(id: Long) {
        _uiState.update { it.copy(selectedHabitId = id, error = null) }
    }

    fun onDateChange(value: String) {
        _uiState.update { it.copy(date = value, error = null) }
    }

    fun onStartTimeChange(value: String) {
        _uiState.update { it.copy(startTime = value, error = null) }
    }

    fun onDurationChange(value: String) {
        if (value.all { it.isDigit() }) {
            _uiState.update { it.copy(durationMinutes = value, error = null) }
        }
    }

    fun onRepeatPatternChange(value: String) {
        _uiState.update { it.copy(repeatPattern = value, error = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun createSchedule() {
        val state = _uiState.value
        if (state.isSaving) return

        val duration = state.durationMinutes.toIntOrNull()
        val validationError = when {
            state.selectedHabitId == null -> "Choose a habit"
            state.date.isBlank() -> "Choose a date"
            state.startTime.isBlank() -> "Choose a start time"
            duration == null || duration <= 0 -> "Duration must be at least 1 minute"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        val startIso = DateTimeUtils.buildIsoDateTime(state.date, state.startTime)
        val endIso = DateTimeUtils.addMinutesToIso(startIso, duration!!)
        val notes = state.notes.trim().ifBlank { null }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val result = if (state.repeatPattern == CreateScheduleUiState.REPEAT_NONE) {
                scheduleRepository.createCustomSchedule(
                    habitId = state.selectedHabitId!!,
                    date = state.date,
                    startTime = startIso,
                    endTime = endIso,
                    durationMinutes = duration,
                    notes = notes
                ).map { listOf(it) }
            } else {
                scheduleRepository.createRecurringSchedule(
                    habitId = state.selectedHabitId!!,
                    startTime = startIso,
                    endTime = endIso,
                    durationMinutes = duration,
                    repeatPattern = state.repeatPattern,
                    repeatDays = 30,
                    notes = notes
                )
            }

            result
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Failed to create schedule"
                        )
                    }
                }
        }
    }
}

private fun showDatePicker(
    context: Context,
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(currentDate)
    }.getOrNull()?.let { calendar.time = it }

    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            onDateSelected(
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.time)
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: Context,
    currentTime: String,
    onTimeSelected: (String) -> Unit
) {
    val parts = currentTime.split(":").mapNotNull { it.toIntOrNull() }
    val hour = parts.getOrNull(0) ?: 9
    val minute = parts.getOrNull(1) ?: 0

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(
                String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
            )
        },
        hour,
        minute,
        true
    ).show()
}

// Value sent to the backend paired with the label shown in the UI
private val repeatOptions = listOf(
    CreateScheduleUiState.REPEAT_NONE to "Once",
    CreateScheduleUiState.REPEAT_DAILY to "Daily",
    CreateScheduleUiState.REPEAT_WEEKDAYS to "Weekdays",
    CreateScheduleUiState.REPEAT_WEEKENDS to "Weekends"
)

@Composable
fun CreateScheduleScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToAddHabit: () -> Unit,
    viewModel: CreateScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadHabits() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, "Schedule created", Toast.LENGTH_SHORT).show()
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
                Text("New schedule", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Choose a habit, time, and repeat pattern.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text("Habit", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.habits, key = { it.id }) { habit ->
                FilterChip(
                    selected = uiState.selectedHabitId == habit.id,
                    onClick = { viewModel.onHabitSelected(habit.id) },
                    label = { Text(habit.name) }
                )
            }
        }

        Text("When", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    showDatePicker(context, uiState.date, viewModel::onDateChange)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(DateTimeUtils.formatDisplayDate(uiState.date))
            }
            OutlinedButton(
                onClick = {
                    showTimePicker(context, uiState.startTime, viewModel::onStartTimeChange)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(uiState.startTime)
            }
        }

        OutlinedTextField(
            value = uiState.durationMinutes,
            onValueChange = viewModel::onDurationChange,
            label = { Text("Duration (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Repeat", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(repeatOptions) { (pattern, label) ->
                FilterChip(
                    selected = uiState.repeatPattern == pattern,
                    onClick = { viewModel.onRepeatPatternChange(pattern) },
                    label = { Text(label) }
                )
            }
        }

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::createSchedule,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create schedule")
        }

        TextButton(
            onClick = onNavigateToAddHabit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add another habit")
        }
    }
}
