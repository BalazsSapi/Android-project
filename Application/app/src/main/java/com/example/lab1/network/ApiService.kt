package com.example.lab1.network

import com.example.lab1.network.models.CreateCustomScheduleRequest
import com.example.lab1.network.models.CreateHabitRequest
import com.example.lab1.network.models.CreateProgressRequest
import com.example.lab1.network.models.CreateRecurringScheduleRequest
import com.example.lab1.network.models.CreateWeekdayScheduleRequest
import com.example.lab1.network.models.HabitCategory
import com.example.lab1.network.models.HabitResponse
import com.example.lab1.network.models.ProfileDto
import com.example.lab1.network.models.ProgressResponseDto
import com.example.lab1.network.models.ScheduleResponse
import com.example.lab1.network.models.UpdateProfileRequest
import com.example.lab1.network.models.UpdateScheduleRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("habit/categories")
    suspend fun getHabitCategories(): Response<List<HabitCategory>>

    @GET("habit")
    suspend fun getHabits(): Response<List<HabitResponse>>

    @POST("habit")
    suspend fun createHabit(@Body body: CreateHabitRequest): Response<HabitResponse>

    @GET("habit/user/{userId}")
    suspend fun getHabitsByUser(@Path("userId") userId: Long): Response<List<HabitResponse>>

    @GET("schedule/day")
    suspend fun getScheduleByDay(@Query("date") date: String): Response<List<ScheduleResponse>>

    @GET("schedule/{id}")
    suspend fun getScheduleById(@Path("id") id: Long): Response<ScheduleResponse>

    @PATCH("schedule/{id}")
    suspend fun updateSchedule(
        @Path("id") id: Long,
        @Body body: UpdateScheduleRequest
    ): Response<ScheduleResponse>

    @DELETE("schedule/{id}")
    suspend fun deleteSchedule(@Path("id") id: Long): Response<Unit>

    @POST("schedule/recurring")
    suspend fun createRecurringSchedule(
        @Body body: CreateRecurringScheduleRequest
    ): Response<List<ScheduleResponse>>

    @POST("schedule/recurring/weekdays")
    suspend fun createWeekdaySchedule(
        @Body body: CreateWeekdayScheduleRequest
    ): Response<List<ScheduleResponse>>

    @POST("schedule/custom")
    suspend fun createCustomSchedule(
        @Body body: CreateCustomScheduleRequest
    ): Response<ScheduleResponse>

    @POST("progress")
    suspend fun createProgress(@Body body: CreateProgressRequest): Response<ProgressResponseDto>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileDto>

    @PATCH("profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<ProfileDto>

    @Multipart
    @POST("profile/upload-profile-image")
    suspend fun uploadProfileImage(@Part profileImage: MultipartBody.Part): Response<ProfileDto>
}
