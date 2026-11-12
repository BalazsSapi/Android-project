package com.example.lab1.network

import android.content.Context
import com.example.lab1.utils.SessionManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSerializationApi::class)
object RetrofitClient {

    // 10.0.2.2 is the emulator's alias for the host machine's localhost.
    // Physical device: http://<your-computer-LAN-IP>:8080/
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true // defaults such as is_custom and repeatDays must be sent too
    }

    // Adds the stored access token to every request
    private val authInterceptor = Interceptor { chain ->
        val token = appContext?.let { SessionManager.getInstance(it).accessToken }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
