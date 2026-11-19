package com.example.lab1

import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Register

@Serializable
object ResetPassword

@Serializable
object Home

@Serializable
object Profile

@Serializable
object EditProfile

@Serializable
object AddHabit

@Serializable
object CreateSchedule

/** Egy schedule részletei (GET /schedule/{id}) */
@Serializable
data class ScheduleDetails(val scheduleId: Long)

/** Egy schedule szerkesztése (PATCH /schedule/{id}) */
@Serializable
data class EditSchedule(val scheduleId: Long)

/** A bejelentkezés utáni képernyők csoportja. */
@Serializable
object MainGraph
