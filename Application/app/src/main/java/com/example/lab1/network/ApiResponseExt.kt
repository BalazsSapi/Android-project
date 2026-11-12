package com.example.lab1.network

import com.example.lab1.network.models.ApiError
import retrofit2.Response

internal fun <T> Response<T>.bodyOrThrow(fallbackMessage: String): T {
    if (isSuccessful) {
        return body() ?: error("Empty response from server")
    }
    error(parseErrorMessage(this, fallbackMessage))
}

private fun parseErrorMessage(response: Response<*>, fallbackMessage: String): String {
    val raw = response.errorBody()?.string()
    if (!raw.isNullOrBlank()) {
        val parsed = try {
            RetrofitClient.json.decodeFromString(ApiError.serializer(), raw)
        } catch (e: Exception) {
            null
        }
        parsed?.message?.let { return it }
    }
    val message = response.message()
    return if (message.isBlank()) "$fallbackMessage (${response.code()})" else message
}
