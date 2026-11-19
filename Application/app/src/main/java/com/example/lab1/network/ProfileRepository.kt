package com.example.lab1.network

import com.example.lab1.network.models.ProfileDto
import com.example.lab1.network.models.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileRepository(
    private val api: ApiService = RetrofitClient.apiService
) {

    suspend fun getProfile(): Result<ProfileDto> = runCatching {
        api.getProfile().bodyOrThrow("Failed to load profile")
    }

    suspend fun updateProfile(username: String): Result<ProfileDto> = runCatching {
        api.updateProfile(UpdateProfileRequest(username = username))
            .bodyOrThrow("Failed to update profile")
    }

    suspend fun uploadProfileImage(file: File, mimeType: String): Result<ProfileDto> = runCatching {
        val part = MultipartBody.Part.createFormData(
            "profileImage",
            file.name,
            file.asRequestBody(mimeType.toMediaType())
        )
        api.uploadProfileImage(part).bodyOrThrow("Failed to upload photo")
    }
}
