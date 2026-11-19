package com.example.lab1

import android.app.Application
import android.widget.Toast
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
import com.example.lab1.network.ScheduleRepository
import com.example.lab1.network.models.ParticipantDto
import com.example.lab1.network.models.UpdateScheduleRequest
import com.example.lab1.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditScheduleUiState(
    val isLoading: Boolean = true,
    val habitName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: String = "",
    val status: String = STATUS_PLANNED,
    val notes: String = "",
    val participants: List<ParticipantDto> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
) {
    companion object {
        const val STATUS_PLANNED = "Planned"
        const val STATUS_SKIPPED = "Skipped"
        const val STATUS_COMPLETED = "Completed"
    }
}

class EditScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository()

    private val _uiState = MutableStateFlow(EditScheduleUiState())
    val uiState: StateFlow<EditScheduleUiState> = _uiState.asStateFlow()

    private var scheduleId: Long = -1L
    private var loaded = false
    private var originalParticipantIds: List<Long> = emptyList()

    // Az adatok csak egyszer töltődnek be, hogy pl. képernyőforgatás ne írja felül a szerkesztést
    fun load(id: Long) {
        if (loaded) return
        scheduleId = id
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            scheduleRepository.getScheduleById(id)
                .onSuccess { schedule ->
                    loaded = true
                    originalParticipantIds = schedule.participants.map { it.id }

                    val startIso = schedule.startTime
                    val duration = schedule.durationMinutes
                    // Ha nincs end_time, a kezdésből és az időtartamból számoljuk
                    val endText = when {
                        !schedule.endTime.isNullOrBlank() -> DateTimeUtils.formatTime(schedule.endTime)
                        startIso != null && duration != null ->
                            DateTimeUtils.addMinutesToIso(startIso, duration)
                                ?.let { DateTimeUtils.formatTime(it) }
                                .orEmpty()
                        else -> ""
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            habitName = schedule.habit?.name.orEmpty(),
                            date = DateTimeUtils.formatDate(startIso) ?: DateTimeUtils.todayDate(),
                            startTime = DateTimeUtils.formatTime(startIso),
                            endTime = endText,
                            durationMinutes = duration?.toString().orEmpty(),
                            status = schedule.status ?: EditScheduleUiState.STATUS_PLANNED,
                            notes = schedule.notes.orEmpty(),
                            participants = schedule.participants
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load schedule"
                        )
                    }
                }
        }
    }

    // "HH:mm" -> percek
    private fun minutesOf(time: String): Int? {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    // Ha a kezdés és a vég is érvényes, az időtartam automatikusan követi őket
    private fun syncDuration(state: EditScheduleUiState): EditScheduleUiState {
        val start = minutesOf(state.startTime)
        val end = minutesOf(state.endTime)
        return if (start != null && end != null && end > start) {
            state.copy(durationMinutes = (end - start).toString())
        } else {
            state
        }
    }

    fun onStartTimeChange(value: String) {
        _uiState.update { syncDuration(it.copy(startTime = value, error = null)) }
    }

    fun onEndTimeChange(value: String) {
        _uiState.update { syncDuration(it.copy(endTime = value, error = null)) }
    }

    fun onDurationChange(value: String) {
        if (value.all { it.isDigit() }) {
            _uiState.update { it.copy(durationMinutes = value, error = null) }
        }
    }

    fun onStatusChange(value: String) {
        _uiState.update { it.copy(status = value, error = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun onRemoveParticipant(id: Long) {
        _uiState.update { state ->
            state.copy(participants = state.participants.filter { it.id != id }, error = null)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val duration = state.durationMinutes.toIntOrNull()
        val start = minutesOf(state.startTime)
        val end = minutesOf(state.endTime)
        val validationError = when {
            state.startTime.isBlank() -> "Choose a start time"
            duration == null || duration <= 0 -> "Duration must be at least 1 minute"
            state.endTime.isNotBlank() && start != null && end != null && end <= start ->
                "End time must be after the start time"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        val startIso = DateTimeUtils.buildIsoDateTime(state.date, state.startTime)
        val endIso = state.endTime.takeIf { it.isNotBlank() }
            ?.let { DateTimeUtils.buildIsoDateTime(state.date, it) }
        // A résztvevők listáját csak akkor küldjük, ha változott
        val participantIds = state.participants.map { it.id }.takeIf { it != originalParticipantIds }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            scheduleRepository.updateSchedule(
                scheduleId,
                UpdateScheduleRequest(
                    startTime = startIso,
                    endTime = endIso,
                    durationMinutes = duration,
                    status = state.status,
                    notes = state.notes.trim(),
                    participantIds = participantIds
                )
            )
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Failed to update schedule"
                        )
                    }
                }
        }
    }
}

private val statusOptions = listOf(
    EditScheduleUiState.STATUS_PLANNED,
    EditScheduleUiState.STATUS_SKIPPED,
    EditScheduleUiState.STATUS_COMPLETED
)

@Composable
fun EditScheduleScreen(
    scheduleId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(scheduleId) { viewModel.load(scheduleId) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, "Schedule updated", Toast.LENGTH_SHORT).show()
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
            Text("Edit Schedule", style = MaterialTheme.typography.headlineSmall)
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        if (uiState.habitName.isNotBlank()) {
            Text(uiState.habitName, style = MaterialTheme.typography.titleMedium)
        }

        Text("Time range", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showTimePicker(context, uiState.startTime, viewModel::onStartTimeChange) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start: ${uiState.startTime.ifBlank { "--:--" }}")
            }
            OutlinedButton(
                onClick = { showTimePicker(context, uiState.endTime, viewModel::onEndTimeChange) },
                modifier = Modifier.weight(1f)
            ) {
                Text("End: ${uiState.endTime.ifBlank { "--:--" }}")
            }
        }

        OutlinedTextField(
            value = uiState.durationMinutes,
            onValueChange = viewModel::onDurationChange,
            label = { Text("Duration (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Status", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            statusOptions.forEach { status ->
                FilterChip(
                    selected = uiState.status == status,
                    onClick = { viewModel.onStatusChange(status) },
                    label = { Text(status) }
                )
            }
        }

        // Résztvevők: itt eltávolíthatók (hozzáadás nincs, lásd a megjegyzést a végén)
        Text("Partners", style = MaterialTheme.typography.titleSmall)
        if (uiState.participants.isEmpty()) {
            Text("No partners", style = MaterialTheme.typography.bodySmall)
        } else {
            uiState.participants.forEach { participant ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(participant.name ?: participant.email ?: "User #${participant.id}")
                    TextButton(onClick = { viewModel.onRemoveParticipant(participant.id) }) {
                        Text("Remove")
                    }
                }
            }
        }

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Notes") },
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
                enabled = !uiState.isSaving && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
