package com.example.lab1.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /auth/reset-password-via-email */
@Serializable
data class ResetPasswordRequest(
    val email: String
)

/** POST /progress */
@Serializable
data class CreateProgressRequest(
    val scheduleId: Long,
    val date: String,
    @SerialName("logged_time") val loggedTime: Double? = null,
    val notes: String? = null,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

/** PATCH /schedule/{id} – a null mezők kimaradnak a kérésből (explicitNulls = false) */
@Serializable
data class UpdateScheduleRequest(
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val status: String? = null,
    val notes: String? = null,
    val participantIds: List<Long>? = null
)

/** POST /schedule/recurring/weekdays (daysOfWeek: 1 = hétfő ... 7 = vasárnap) */
@Serializable
data class CreateWeekdayScheduleRequest(
    val habitId: Long,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val daysOfWeek: List<Int>,
    val numberOfWeeks: Int = 4,
    val participantIds: List<Long> = emptyList(),
    val notes: String? = null
)

/** PATCH /profile */
@Serializable
data class UpdateProfileRequest(
    val username: String
)
