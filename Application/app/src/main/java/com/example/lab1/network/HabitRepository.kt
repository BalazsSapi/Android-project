package com.example.lab1.network

import com.example.lab1.network.models.CreateHabitRequest
import com.example.lab1.network.models.HabitCategory
import com.example.lab1.network.models.HabitResponse

class HabitRepository(
    private val api: ApiService = RetrofitClient.apiService
) {

    suspend fun getCategories(): Result<List<HabitCategory>> = runCatching {
        api.getHabitCategories().bodyOrThrow("Failed to load habit categories")
    }

    suspend fun getHabits(): Result<List<HabitResponse>> = runCatching {
        api.getHabits().bodyOrThrow("Failed to load habits")
    }

    suspend fun getHabitsByUser(userId: Long): Result<List<HabitResponse>> = runCatching {
        api.getHabitsByUser(userId).bodyOrThrow("Failed to load habits")
    }

    suspend fun createHabit(
        name: String,
        description: String,
        categoryId: Long,
        goal: String
    ): Result<HabitResponse> = runCatching {
        api.createHabit(
            CreateHabitRequest(
                name = name,
                description = description.ifBlank { null },
                categoryId = categoryId,
                goal = goal
            )
        ).bodyOrThrow("Failed to create habit")
    }
}
