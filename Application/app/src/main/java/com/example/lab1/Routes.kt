package com.example.lab1

import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Home

/** A bejelentkezés utáni képernyők (Home, Profile) csoportja. */
@Serializable
object MainGraph

@Serializable
data class Profile(
    val userId: String = "guest",
    val userName: String = "Guest"
)
