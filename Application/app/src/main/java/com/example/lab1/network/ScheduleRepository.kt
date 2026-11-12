package com.example.lab1.network

import com.example.lab1.network.models.CreateCustomScheduleRequest
import com.example.lab1.network.models.CreateRecurringScheduleRequest
import com.example.lab1.network.models.ScheduleResponse

class ScheduleRepository(
    private val api: ApiService = RetrofitClient.apiService
) {

    suspend fun getScheduleByDay(day: String): Result<List<ScheduleResponse>> = runCatching {
        api.getScheduleByDay(day).bodyOrThrow("Failed to load schedules")
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
}
