package com.example.lab1.network

import com.example.lab1.network.models.CreateCustomScheduleRequest
import com.example.lab1.network.models.CreateHabitRequest
import com.example.lab1.network.models.CreateRecurringScheduleRequest
import com.example.lab1.network.models.HabitCategory
import com.example.lab1.network.models.HabitResponse
import com.example.lab1.network.models.ScheduleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("habit/categories")
    suspend fun getHabitCategories(): Response<List<HabitCategory>>

    @GET("habit")
    suspend fun getHabits(): Response<List<HabitResponse>>

    @POST("habit")
    suspend fun createHabit(@Body body: CreateHabitRequest): Response<HabitResponse>

    @GET("schedule/day")
    suspend fun getScheduleByDay(@Query("date") date: String): Response<List<ScheduleResponse>>

    @POST("schedule/recurring")
    suspend fun createRecurringSchedule(
        @Body body: CreateRecurringScheduleRequest
    ): Response<List<ScheduleResponse>>

    @POST("schedule/custom")
    suspend fun createCustomSchedule(
        @Body body: CreateCustomScheduleRequest
    ): Response<ScheduleResponse>
}
