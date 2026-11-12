package com.example.lab1

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab1.network.HabitRepository
import com.example.lab1.network.ScheduleRepository
import com.example.lab1.network.models.HabitResponse
import com.example.lab1.network.models.ScheduleResponse
import com.example.lab1.utils.DateTimeUtils
import com.example.lab1.utils.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val today: String = DateTimeUtils.todayDate(),
    val isLoading: Boolean = true,
    val schedules: List<ScheduleResponse> = emptyList(),
    val habits: List<HabitResponse> = emptyList(),
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val habitRepository = HabitRepository()
    private val scheduleRepository = ScheduleRepository()
    private val sessionManager = SessionManager.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadToday() {
        val today = DateTimeUtils.todayDate()
        _uiState.update { it.copy(today = today, isLoading = true, error = null) }

        viewModelScope.launch {
            val habitsDeferred = async { habitRepository.getHabits() }
            val schedulesDeferred = async { scheduleRepository.getScheduleByDay(today) }

            val habitsResult = habitsDeferred.await()
            val schedulesResult = schedulesDeferred.await()

            val error = schedulesResult.exceptionOrNull()?.message
                ?: habitsResult.exceptionOrNull()?.message

            _uiState.update {
                it.copy(
                    isLoading = false,
                    habits = habitsResult.getOrDefault(it.habits),
                    schedules = schedulesResult.getOrDefault(it.schedules)
                        .sortedBy { schedule -> schedule.startTime.orEmpty() },
                    error = error
                )
            }
        }
    }

    fun logout() = sessionManager.clear()
}

@Composable
fun HomeScreen(
    onAddHabit: () -> Unit,
    onCreateSchedule: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadToday()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Today", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        DateTimeUtils.formatDisplayDate(uiState.today),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row {
                    TextButton(onClick = viewModel::loadToday) { Text("Refresh") }
                    TextButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) { Text("Log out") }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onAddHabit, modifier = Modifier.weight(1f)) {
                    Text("Add habit")
                }
                Button(onClick = onCreateSchedule, modifier = Modifier.weight(1f)) {
                    Text("New schedule")
                }
            }
        }

        if (uiState.isLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        uiState.error?.let { message ->
            item {
                Text(
                    "Couldn't load your data: $message",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Text(
                "Today's schedule (${uiState.schedules.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (uiState.schedules.isEmpty()) {
            item { Text("Nothing scheduled today") }
        } else {
            items(uiState.schedules, key = { it.id }) { schedule ->
                ScheduleCard(schedule)
            }
        }

        item {
            Text("Habits", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.habits.isEmpty()) {
            item { Text("No habits yet") }
        } else {
            items(uiState.habits, key = { it.id }) { habit ->
                HabitCard(habit)
            }
        }
    }
}

@Composable
private fun ScheduleCard(schedule: ScheduleResponse) {
    val categoryName = schedule.habit?.category?.name ?: "Other"
    val startTime = DateTimeUtils.formatTime(schedule.startTime).ifBlank { "--:--" }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(Modifier.width(64.dp)) {
                Text(startTime, style = MaterialTheme.typography.titleSmall)
                Text("${schedule.durationMinutes ?: 0} min", style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text(schedule.habit?.name ?: "Unknown habit", style = MaterialTheme.typography.titleSmall)
                Text(categoryName, style = MaterialTheme.typography.bodySmall)
                schedule.habit?.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                schedule.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            schedule.status?.let {
                Text("• $it", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HabitCard(habit: HabitResponse) {
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
