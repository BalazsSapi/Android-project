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
    const val BASE_URL = "http://10.0.2.2:8080/"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true // a is_custom, repeatDays stb. alapértékeket is el kell küldeni
    }

    // Minden kéréshez hozzáadja a mentett access tokent
    // (ha a kérésnek már van Authorization fejléce, pl. a refresh hívásnál, azt nem írja felül)
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = appContext?.let { SessionManager.getInstance(it).accessToken }
        val request = if (token.isNullOrBlank() || original.header("Authorization") != null) {
            original
        } else {
            original.newBuilder()
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
