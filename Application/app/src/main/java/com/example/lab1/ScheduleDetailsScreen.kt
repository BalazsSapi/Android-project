package com.example.lab1

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.lab1.network.ScheduleRepository
import com.example.lab1.network.models.ScheduleResponse
import com.example.lab1.network.models.UpdateScheduleRequest
import com.example.lab1.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleDetailsUiState(
    val schedule: ScheduleResponse? = null,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false
)

class ScheduleDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository()

    private val _uiState = MutableStateFlow(ScheduleDetailsUiState())
    val uiState: StateFlow<ScheduleDetailsUiState> = _uiState.asStateFlow()

    private var scheduleId: Long = -1L

    // GET /schedule/{id}: a képernyő minden megnyitásakor újratölt (szerkesztés után is friss adat kell)
    fun load(id: Long) {
        scheduleId = id
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            scheduleRepository.getScheduleById(id)
                .onSuccess { schedule ->
                    _uiState.update { it.copy(schedule = schedule, isLoading = false) }
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

    // Jegyzet mentése: PATCH /schedule/{id} csak a notes mezővel
    fun saveNotes(notes: String, onSuccess: () -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            scheduleRepository.updateSchedule(scheduleId, UpdateScheduleRequest(notes = notes.trim()))
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false) }
                    onSuccess()
                    load(scheduleId)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = throwable.message ?: "Failed to save notes"
                        )
                    }
                }
        }
    }

    // Haladás rögzítése: POST /progress (a dátum a mai nap)
    fun addProgress(
        loggedTime: Double?,
        notes: String,
        isCompleted: Boolean,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            scheduleRepository.createProgress(
                scheduleId = scheduleId,
                date = DateTimeUtils.todayDate(),
                loggedTime = loggedTime,
                notes = notes.trim().ifBlank { null },
                isCompleted = isCompleted
            )
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false) }
                    onSuccess()
                    load(scheduleId)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = throwable.message ?: "Failed to save progress"
                        )
                    }
                }
        }
    }

    // Törlés: DELETE /schedule/{id}
    fun delete() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId)
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false, isDeleted = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = throwable.message ?: "Failed to delete schedule"
                        )
                    }
                }
        }
    }
}

// Teljesítettség 0..1: ha kész (státusz vagy haladás szerint), akkor 1,
// egyébként a rögzített idők összege a tervezett időtartamhoz képest
private fun progressFraction(schedule: ScheduleResponse): Float {
    if (schedule.status.equals("Completed", ignoreCase = true) ||
        schedule.progress.any { it.isCompleted }
    ) {
        return 1f
    }
    val duration = schedule.durationMinutes ?: return 0f
    if (duration <= 0) return 0f
    val logged = schedule.progress.sumOf { it.loggedTime ?: 0.0 }
    return (logged / duration).toFloat().coerceIn(0f, 1f)
}

@Composable
fun ScheduleDetailsScreen(
    scheduleId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ScheduleDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var isEditingNotes by remember { mutableStateOf(false) }
    var notesDraft by remember { mutableStateOf("") }

    LaunchedEffect(scheduleId) { viewModel.load(scheduleId) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            Toast.makeText(context, "Schedule deleted", Toast.LENGTH_SHORT).show()
            onDeleted()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                "Schedule Details",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            // Opciók menü: innen érhető el a törlés
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        val schedule = uiState.schedule
        if (schedule == null) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }
        } else {
            val fraction = progressFraction(schedule)

            // A habit és a schedule részletei + haladás
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        schedule.habit?.name ?: "Unknown habit",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        DateTimeUtils.formatScheduleTime(
                            schedule.startTime,
                            schedule.endTime,
                            schedule.durationMinutes
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    schedule.habit?.category?.name?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    schedule.habit?.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    schedule.habit?.goal?.takeIf { it.isNotBlank() }?.let {
                        Text("Goal: $it", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(8.dp))
                    DetailRow("Status", schedule.status ?: "-")
                    DetailRow("Duration", "${schedule.durationMinutes ?: 0} min")
                    schedule.type?.takeIf { it.isNotBlank() }?.let { DetailRow("Repeat", it) }
                    if (schedule.participants.isNotEmpty()) {
                        DetailRow(
                            "With partner(s)",
                            schedule.participants.joinToString { it.name ?: it.email ?: "#${it.id}" }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Progress", style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Jegyzetek: szerkesztő módba lehet váltani, és mentéssel tárolódik
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    if (isEditingNotes) {
                        OutlinedTextField(
                            value = notesDraft,
                            onValueChange = { notesDraft = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isEditingNotes = false },
                                enabled = !uiState.isBusy
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    viewModel.saveNotes(notesDraft) { isEditingNotes = false }
                                },
                                enabled = !uiState.isBusy
                            ) { Text("Save") }
                        }
                    } else {
                        Text(schedule.notes?.takeIf { it.isNotBlank() } ?: "No notes yet")
                        TextButton(onClick = {
                            notesDraft = schedule.notes.orEmpty()
                            isEditingNotes = true
                        }) { Text("Edit Notes") }
                    }
                }
            }

            // Legutóbbi aktivitások: a schedule korábbi haladás-bejegyzései
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
                    val recent = schedule.progress.sortedByDescending { it.date.orEmpty() }.take(5)
                    if (recent.isEmpty()) {
                        Text("No activity yet")
                    } else {
                        recent.forEach { progress ->
                            val habitName = schedule.habit?.name ?: "habit"
                            val title = if (progress.isCompleted) "Completed $habitName" else "Logged $habitName"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall)
                                    progress.date?.let {
                                        Text(it.take(10), style = MaterialTheme.typography.bodySmall)
                                    }
                                    progress.notes?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                progress.loggedTime?.let {
                                    Text("${it.toInt()} min", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Edit schedule")
                }
                Button(
                    onClick = { showProgressDialog = true },
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add progress")
                }
            }
        }
    }

    // Törlés megerősítése
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Schedule") },
            text = { Text("Are you sure you want to delete this schedule? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showProgressDialog) {
        AddProgressDialog(
            onDismiss = { showProgressDialog = false },
            onConfirm = { loggedTime, notes, completed ->
                showProgressDialog = false
                viewModel.addProgress(loggedTime, notes, completed) {}
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun AddProgressDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double?, String, Boolean) -> Unit
) {
    var minutes by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { value -> if (value.all { it.isDigit() }) minutes = value },
                    label = { Text("Logged time (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = completed, onCheckedChange = { completed = it })
                    Text("Mark as completed")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes.toDoubleOrNull(), notes, completed) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
