package com.example.lab1.network

import com.example.lab1.network.models.LoginRequest
import com.example.lab1.network.models.LoginResponse
import com.example.lab1.network.models.ResetPasswordRequest
import com.example.lab1.network.models.TokensDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthApi {

    @POST("auth/local/signin")
    suspend fun signIn(@Body body: LoginRequest): Response<LoginResponse>

    @Multipart
    @POST("auth/local/signup")
    suspend fun signUp(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part profileImage: MultipartBody.Part? // optional file
    ): Response<LoginResponse>

    // A refresh tokent kézzel adjuk át ("Bearer <refreshToken>"), az interceptor ilyenkor nem írja felül
    @POST("auth/local/refresh")
    suspend fun refresh(@Header("Authorization") authorization: String): Response<TokensDto>

    @POST("auth/local/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/reset-password-via-email")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<Unit>
}
