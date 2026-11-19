package com.example.lab1.network

import com.example.lab1.network.models.CreateCustomScheduleRequest
import com.example.lab1.network.models.CreateProgressRequest
import com.example.lab1.network.models.CreateRecurringScheduleRequest
import com.example.lab1.network.models.CreateWeekdayScheduleRequest
import com.example.lab1.network.models.ProgressResponseDto
import com.example.lab1.network.models.ScheduleResponse
import com.example.lab1.network.models.UpdateScheduleRequest

class ScheduleRepository(
    private val api: ApiService = RetrofitClient.apiService
) {

    suspend fun getScheduleByDay(day: String): Result<List<ScheduleResponse>> = runCatching {
        api.getScheduleByDay(day).bodyOrThrow("Failed to load schedules")
    }

    suspend fun getScheduleById(id: Long): Result<ScheduleResponse> = runCatching {
        api.getScheduleById(id).bodyOrThrow("Failed to load schedule")
    }

    suspend fun updateSchedule(id: Long, body: UpdateScheduleRequest): Result<ScheduleResponse> =
        runCatching {
            api.updateSchedule(id, body).bodyOrThrow("Failed to update schedule")
        }

    suspend fun deleteSchedule(id: Long): Result<Unit> = runCatching {
        api.deleteSchedule(id).requireSuccess("Failed to delete schedule")
    }

    suspend fun createRecurringSchedule(
        habitId: Long,
        startTime: String,
        endTime: String?,
        durationMinutes: Int,
        repeatPattern: String,
        repeatDays: Int,
        notes: String?
    ): Result<List<ScheduleResponse>> = runCatching {
        api.createRecurringSchedule(
            CreateRecurringScheduleRequest(
                habitId = habitId,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                repeatPattern = repeatPattern,
                repeatDays = repeatDays,
                notes = notes
            )
        ).bodyOrThrow("Failed to create schedule")
    }

    suspend fun createWeekdaySchedule(
        habitId: Long,
        startTime: String,
        endTime: String?,
        durationMinutes: Int,
        daysOfWeek: List<Int>,
        numberOfWeeks: Int,
        notes: String?
    ): Result<List<ScheduleResponse>> = runCatching {
        api.createWeekdaySchedule(
            CreateWeekdayScheduleRequest(
                habitId = habitId,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                daysOfWeek = daysOfWeek,
                numberOfWeeks = numberOfWeeks,
                notes = notes
            )
        ).bodyOrThrow("Failed to create schedule")
    }

    suspend fun createCustomSchedule(
        habitId: Long,
        date: String,
        startTime: String,
        endTime: String?,
        durationMinutes: Int,
        notes: String?
    ): Result<ScheduleResponse> = runCatching {
        api.createCustomSchedule(
            CreateCustomScheduleRequest(
                habitId = habitId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                notes = notes
            )
        ).bodyOrThrow("Failed to create schedule")
    }

    suspend fun createProgress(
        scheduleId: Long,
        date: String,
        loggedTime: Double?,
        notes: String?,
        isCompleted: Boolean
    ): Result<ProgressResponseDto> = runCatching {
        api.createProgress(
            CreateProgressRequest(
                scheduleId = scheduleId,
                date = date,
                loggedTime = loggedTime,
                notes = notes,
                isCompleted = isCompleted
            )
        ).bodyOrThrow("Failed to save progress")
    }
}
